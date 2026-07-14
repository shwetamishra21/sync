import os
from models.submission_model import FormSubmission
from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_bcrypt import Bcrypt
import jwt
from flask_mail import Mail, Message
import random
import datetime
from datetime import timedelta
import secrets
from dotenv import load_dotenv
from database.db import db
from models.user_model import User
from models.admin_model import Admin
from models.form_model import Form
from models.field_model import Field
import json
from functools import wraps
import mimetypes
import traceback
from sqlalchemy.exc import IntegrityError  # ✅ NEW: Import for duplicate handling

def _is_char_indexed_dict(value):
    """
    Detects the "spread a string" corruption pattern.

    Before the JSONB double-encoding bug was fixed, theme_json /
    layout_json / branding_json could come back from the API as a JSON
    STRING instead of an object. The Admin Panel editors (ThemeEditor /
    LayoutEditor / BrandingEditor) kept that string in React state and, on
    the next edit, did `{...prevString, [field]: newValue}`. Spreading a
    JS string doesn't throw - it silently turns each character into a
    numeric-key entry, e.g. {"0": "{", "1": "\"", "2": "c", ..., "spacing": 20}.
    That object is valid JSON, so it no longer crashes Gson/the browser,
    but it's garbage: the real config is gone, replaced by one field
    plus a pile of single-character entries. This is why "layout doesn't
    work" (silently) even after the object/string bug was fixed.
    """
    if not isinstance(value, dict) or not value:
        return False
    numeric_keys = [k for k in value if isinstance(k, str) and k.isdigit()]
    # A real theme/layout/branding config has none or very few numeric
    # keys. A corrupted one is *mostly* sequential numeric keys starting
    # at 0.
    if len(numeric_keys) < 5:
        return False
    indices = sorted(int(k) for k in numeric_keys)
    return indices[0] == 0 and len(numeric_keys) >= len(value) - 3


def _reconstruct_string_from_char_dict(value):
    numeric_items = sorted(
        ((int(k), v) for k, v in value.items() if isinstance(k, str) and k.isdigit()),
        key=lambda pair: pair[0],
    )
    return "".join(str(v) for _, v in numeric_items)


def _coerce_json_field(value, _depth=0):
    """
    Normalize a value coming from a request payload so it can be stored
    directly in a JSONB column (theme_json / layout_json / branding_json).

    JSONB columns already store/return native Python dict/list objects via
    SQLAlchemy - they must NOT be pre-serialized with json.dumps(), or the
    column ends up holding a JSON string instead of a JSON object. That
    breaks every consumer that expects an object at that path, including
    the Admin Panel's Theme Editor and the Android app's Gson models
    (ThemeConfig / LayoutConfig / BrandingConfig).

    - dict/list -> returned as-is, UNLESS it's the "char-indexed" corrupted
                   shape described in _is_char_indexed_dict(), in which case
                   it's reconstructed back into real JSON
    - str       -> assumed to be a JSON-encoded string (possibly encoded
                   more than once); decoded back into a dict/list so we
                   never persist a raw string in the JSONB column
    - anything else / invalid JSON -> falls back to {} so the column never
      ends up in a broken state
    """
    if _depth > 5:
        return {}

    if isinstance(value, dict):
        if _is_char_indexed_dict(value):
            reconstructed_str = _reconstruct_string_from_char_dict(value)
            base = _coerce_json_field(reconstructed_str, _depth + 1)
            # Any non-numeric keys mixed into the mangled object are real
            # field edits the user made (e.g. `{...prevString, spacing: 99}`
            # sets a genuine "spacing" key alongside the character keys).
            # Merge those on top so the user's latest edit isn't lost.
            if isinstance(base, dict):
                overrides = {
                    k: v for k, v in value.items()
                    if not (isinstance(k, str) and k.isdigit())
                }
                return {**base, **overrides}
            return base
        return value

    if isinstance(value, list):
        return value

    if isinstance(value, str):
        try:
            decoded = json.loads(value)
        except (TypeError, ValueError):
            return {}
        if isinstance(decoded, (dict, list, str)):
            return _coerce_json_field(decoded, _depth + 1)
        return {}

    return {}


app = Flask(__name__)
load_dotenv()
print("MAIL_USERNAME =", os.getenv("MAIL_USERNAME"))
print("MAIL_PASSWORD =", os.getenv("MAIL_PASSWORD"))
app.config["MAIL_SERVER"] = os.getenv("MAIL_SERVER")
app.config["MAIL_PORT"] = int(os.getenv("MAIL_PORT"))
app.config["MAIL_USE_TLS"] = os.getenv("MAIL_USE_TLS") == "True"
app.config["MAIL_USERNAME"] = os.getenv("MAIL_USERNAME")
app.config["MAIL_PASSWORD"] = os.getenv("MAIL_PASSWORD")
app.config["MAIL_DEFAULT_SENDER"] = os.getenv("MAIL_DEFAULT_SENDER")

mail = Mail(app)

app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv("DATABASE_URL")
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["MAX_CONTENT_LENGTH"] = 50 * 1024 * 1024  # 50MB max file size

db.init_app(app)

# Verify Database Connection
with app.app_context():
    try:
        db.create_all()  # Create all tables
        db.engine.connect()
        print("\n" + "="*50)
        print("✓ POSTGRESQL CONNECTED SUCCESSFULLY")
        print("="*50 + "\n")
    except Exception as e:
        print("\n" + "="*50)
        print(f"✗ DATABASE ERROR: {e}")
        print("="*50 + "\n")

CORS(app)

bcrypt = Bcrypt(app)

# Secret key for JWT
app.config["SECRET_KEY"] = "jsac_secret_key"

# Store reset tokens (use database in production)
otp_store = {}

# ✅ NEW: Store pending registrations awaiting OTP verification
# Format: { username: { "otp": "123456", "password": "<hashed>", "expires_at": datetime, "verified": False } }
registration_otp_store = {}

# ============================================
# MIDDLEWARE - JWT AUTHENTICATION
# ============================================

def token_required(f):
    """
    Decorator to verify JWT token on protected endpoints
    Add @token_required above route to protect it
    """
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        
        # Check if token in headers
        if 'Authorization' in request.headers:
            auth_header = request.headers['Authorization']
            try:
                token = auth_header.split(" ")[1]
            except IndexError:
                print("[AUTH] Invalid token format")
                return {"message": "Invalid token format"}, 401
        
        if not token:
            print("[AUTH] Token is missing")
            return {"message": "Token is missing"}, 401
        
        try:
            data = jwt.decode(
                token,
                app.config["SECRET_KEY"],
                algorithms=["HS256"]
            )
            current_user = data.get("username")
            print(f"[AUTH] Token verified for user: {current_user}")
        except jwt.ExpiredSignatureError:
            print("[AUTH] Token has expired")
            return {"message": "Token has expired"}, 401
        except jwt.InvalidTokenError:
            print("[AUTH] Invalid token")
            return {"message": "Invalid token"}, 401
        
        return f(current_user, *args, **kwargs)
    
    return decorated


# ============================================
# BASIC ENDPOINTS
# ============================================

@app.route("/")
def home():
    return "JSAC Backend Running"


@app.route("/health", methods=["GET"])
def health():
    return {
        "status": "success",
        "message": "JSAC backend running successfully",
        "version": "1.0.0"
    }


# ============================================
# AUTH ENDPOINTS
# ============================================

@app.route("/register", methods=["POST"])
def register():
    """
    ✅ UPDATED: Registration now goes through OTP verification,
    reusing the same OTP generate/store/send pattern as forgot-password.

    POST /register
        -> validates input, generates OTP, emails it, holds the
           hashed password in registration_otp_store (NOT yet in DB)
        -> Returns: {"message": "OTP sent to your email"}

    The actual User row is only created after the OTP is verified
    via POST /verify-registration-otp.
    """
    print("\n[AUTH] POST /register called")
    
    try:
        data = request.get_json()

        username = data.get("username")
        password = data.get("password")

        if not username or not password:
            print("[AUTH] Missing username or password")
            return {
                "message": "Username and password are required"
            }, 400

        existing_user = User.query.filter_by(
            username=username
        ).first()

        if existing_user:
            print(f"[AUTH] User {username} already exists")
            return {
                "message": "User already exists"
            }, 400

        hashed_password = bcrypt.generate_password_hash(
            password
        ).decode("utf-8")

        # Generate OTP (same helper pattern used by forgot-password)
        otp = str(random.randint(100000, 999999))

        registration_otp_store[username] = {
            "otp": otp,
            "password": hashed_password,
            "expires_at": datetime.datetime.utcnow() + timedelta(minutes=10),
            "verified": False
        }

        print(f"\n{'='*50}")
        print(f"🔐 Registration OTP for {username}: {otp}")
        print(f"{'='*50}\n")

        send_otp_email(username, otp, purpose="register")

        print(f"[AUTH] Registration OTP sent to {username}")

        return {
            "message": "OTP sent to your email"
        }, 200
    
    except Exception as e:
        print(f"[AUTH] Error in register: {str(e)}")
        db.session.rollback()
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/verify-registration-otp", methods=["POST"])
def verify_registration_otp():
    """
    ✅ NEW: Verifies the OTP sent during /register and, only on
    success, actually creates the User row in the database.

    POST /verify-registration-otp
    Request body:
    {
        "username": "user@example.com",
        "otp": "123456"
    }

    Response:
    {
        "message": "Registration successful"
    }
    """
    print("\n[AUTH] POST /verify-registration-otp called")

    try:
        data = request.get_json()

        username = data.get("username")
        otp = data.get("otp")

        if not username or not otp:
            print("[AUTH] Username and OTP are required")
            return {
                "message": "Username and OTP are required"
            }, 400

        if username not in registration_otp_store:
            print(f"[AUTH] No pending registration found for {username}")
            return {
                "message": "OTP expired or registration not found. Please register again."
            }, 400

        record = registration_otp_store[username]

        if datetime.datetime.utcnow() > record["expires_at"]:
            del registration_otp_store[username]
            print(f"[AUTH] Registration OTP expired for {username}")
            return {
                "message": "OTP expired. Please register again."
            }, 400

        if otp != record["otp"]:
            print(f"[AUTH] Invalid registration OTP for {username}")
            return {
                "message": "Invalid OTP"
            }, 400

        # Guard against a duplicate registration completing in the meantime
        existing_user = User.query.filter_by(username=username).first()

        if existing_user:
            del registration_otp_store[username]
            print(f"[AUTH] User {username} already exists")
            return {
                "message": "User already exists"
            }, 400

        new_user = User(
            username=username,
            password=record["password"]
        )

        db.session.add(new_user)
        db.session.commit()

        del registration_otp_store[username]

        print(f"[AUTH] User {username} registered successfully after OTP verification")

        return {
            "message": "Registration successful"
        }, 201

    except Exception as e:
        db.session.rollback()
        print(f"[AUTH] Error in verify_registration_otp: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/login", methods=["POST"])
def login():
    print("\n[AUTH] POST /login called")
    
    try:
        data = request.get_json()
        username = data.get("username")
        password = data.get("password")

        if not username or not password:
            print("[AUTH] Missing username or password")
            return {
                "message": "Username and password are required"
            }, 400

        user = User.query.filter_by(
            username=username
        ).first()

        if not user:
            print(f"[AUTH] User {username} not found")
            return {
                "message": "User not found"
            }, 404

        if not bcrypt.check_password_hash(
            user.password,
            password
        ):
            print(f"[AUTH] Invalid password for user {username}")
            return {
                "message": "Invalid password"
            }, 401

        token = jwt.encode(
            {
                "username": username,
                "exp": datetime.datetime.utcnow() +
                       datetime.timedelta(days=1)
            },
            app.config["SECRET_KEY"],
            algorithm="HS256"
        )

        print(f"[AUTH] User {username} logged in successfully")

        return {
            "token": token,
            "username": username
        }, 200
    
    except Exception as e:
        print(f"[AUTH] Error in login: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500

@app.route("/admin/login", methods=["POST"])
def admin_login():

    print("\n[ADMIN] POST /admin/login")

    try:

        data = request.get_json()

        email = data.get("email")

        password = data.get("password")

        if not email or not password:
            return {
                "message": "Email and password are required"
            },400

        admin = Admin.query.filter_by(
            email=email
        ).first()

        if not admin:
            return {
                "message":"Invalid credentials"
            },401

        if not bcrypt.check_password_hash(
            admin.password,
            password
        ):
            return {
                "message":"Invalid credentials"
            },401

        token = jwt.encode(
            {
                "username": email,
                "exp":
                datetime.datetime.utcnow()
                + datetime.timedelta(days=1)
            },
            app.config["SECRET_KEY"],
            algorithm="HS256"
        )

        return {
            "status":"success",
            "token": token,
            "email": email
        },200

    except Exception as e:

        return {
            "message": str(e)
        },500

def send_otp_email(email, otp, purpose="reset"):
    """
    ✅ UPDATED: Reused for both password-reset and registration OTP emails.

    Args:
        email: recipient email address
        otp: 6-digit OTP code
        purpose: "reset" (default) or "register"
    """

    subject = (
        "JSAC Registration OTP" if purpose == "register"
        else "JSAC Password Reset OTP"
    )

    action_text = (
        "verify your registration" if purpose == "register"
        else "reset your password"
    )

    msg = Message(
        subject=subject,
        recipients=[email]
    )

    msg.body = f"""
Hello,

Your OTP to {action_text} is:

{otp}

This OTP is valid for 10 minutes.

If you did not request this, ignore this email.

JSAC
"""

    mail.send(msg)

# ============================================
# FORGOT PASSWORD ENDPOINTS
# ============================================

@app.route("/forgot-password", methods=["POST"])
def forgot_password():

    try:

        print("\n========== FORGOT PASSWORD ==========")

        data = request.get_json()

        print("REQUEST:", data)

        username = data.get("username")

        print("USERNAME:", username)

        user = User.query.filter_by(username=username).first()

        print("USER FOUND:", user)

        if not user:
            return {
                "message": "User not found"
            },404

        otp = str(random.randint(100000,999999))

        print("OTP:", otp)

        otp_store[username] = {
            "otp": otp,
            "expires_at": datetime.datetime.utcnow() + timedelta(minutes=10),
            "verified": False
        }

        print("Calling send_otp_email()...")

        send_otp_email(username, otp)

        print("OTP email sent successfully.")

        return {
            "message":"OTP sent successfully"
        },200

    except Exception as e:

        import traceback
        traceback.print_exc()

        return {
            "message": str(e)
        },500
@app.route("/verify-otp", methods=["POST"])
def verify_otp():

    try:

        data = request.get_json()

        username = data.get("username")
        otp = data.get("otp")

        if username not in otp_store:

            return {
                "message":"OTP expired"
            },400

        record = otp_store[username]

        if datetime.datetime.utcnow() > record["expires_at"]:

            del otp_store[username]

            return {
                "message":"OTP expired"
            },400

        if otp != record["otp"]:

            return {
                "message":"Invalid OTP"
            },400

        record["verified"] = True

        return {
            "message":"OTP verified"
        },200

    except Exception as e:

        return {
            "message":str(e)
        },500

@app.route("/reset-password", methods=["POST"])
def reset_password():

    try:

        data = request.get_json()

        username = data.get("username")
        new_password = data.get("new_password")

        if username not in otp_store:

            return {
                "message":"OTP verification required"
            },400

        record = otp_store[username]

        if not record["verified"]:

            return {
                "message":"OTP not verified"
            },400

        user = User.query.filter_by(
            username=username
        ).first()

        if not user:

            return {
                "message":"User not found"
            },404

        user.password = bcrypt.generate_password_hash(
            new_password
        ).decode("utf-8")

        db.session.commit()

        del otp_store[username]

        return {
            "message":"Password reset successfully"
        },200

    except Exception as e:

        db.session.rollback()

        return {
            "message":str(e)
        },500



# ============================================
# FORM ENDPOINTS (DATABASE-DRIVEN)
# ============================================

@app.route("/forms", methods=["GET"])
def get_forms():
    """
    Get list of all active forms
    Returns form metadata without field details
    """
    print("\n[FORMS] GET /forms called")
    
    try:
        forms = Form.query.filter_by(is_active=True).all()
        
        form_list = [form.to_dict() for form in forms]
        
        print(f"[FORMS] Returning {len(form_list)} forms")
        
        return {
            "status": "success",
            "forms": form_list,
            "count": len(form_list)
        }, 200
    
    except Exception as e:
        print(f"[FORMS] Error in get_forms: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms", methods=["GET"])
@token_required
def get_admin_forms(current_user):
    """
    Get all forms for the Admin Panel (including inactive forms)
    """
    print(f"\n[ADMIN] GET /admin/forms called by {current_user}")

    try:
        forms = Form.query.all()

        form_list = [form.to_dict() for form in forms]

        return {
            "status": "success",
            "forms": form_list,
            "count": len(form_list)
        }, 200

    except Exception as e:
        print(f"[ADMIN] Error in get_admin_forms: {str(e)}")
        return {
            "message": f"Error: {str(e)}"
        }, 500


@app.route("/admin/forms/<form_id>", methods=["GET"])
@token_required
def get_admin_form(current_user, form_id):
    """
    Get complete form detail for admin editing (including inactive forms)
    Allows admins to edit and view all form properties including theme, layout, branding
    """
    print(f"\n[ADMIN] GET /admin/forms/{form_id} called by {current_user}")

    try:
        form = Form.query.filter_by(id=form_id).first()

        if not form:
            print(f"[ADMIN] Form {form_id} not found")
            return {
                "status": "error",
                "message": f"Form {form_id} not found"
            }, 404

        print(f"[ADMIN] Form {form_id} retrieved with {len(form.fields)} fields")

        return {
            "status": "success",
            "form": form.to_dict_with_fields()
        }, 200

    except Exception as e:
        print(f"[ADMIN] Error in get_admin_form: {str(e)}")
        return {
            "status": "error",
            "message": str(e)
        }, 500


@app.route("/forms/<form_id>", methods=["GET"])
def get_form_detail(form_id):
    """
    Get complete form with all fields
    """
    print(f"\n[FORMS] GET /forms/{form_id} called")
    
    try:
        form = Form.query.filter_by(id=form_id, is_active=True).first()
        
        if not form:
            print(f"[FORMS] Form {form_id} not found")
            return {
                "message": f"Form {form_id} not found"
            }, 404
        
        print(f"[FORMS] Form {form_id} found with {len(form.fields)} fields")
        
        return {
            "status": "success",
            "form": form.to_dict_with_fields()
        }, 200
    
    except Exception as e:
        print(f"[FORMS] Error in get_form_detail: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/forms/submit", methods=["POST"])
@token_required
def submit_form(current_user):
    """
    Submit a completed form with idempotency key support
    
    ✅ FIX #5: Prevents duplicate submissions from retried requests
    Uses idempotency_key to detect if same submission already processed.
    If duplicate detected, returns original submission_id.
    """
    print(f"\n[SUBMISSION] POST /forms/submit called by {current_user}")

    try:
        data = request.get_json()

        if not data:
            print("[SUBMISSION] No data provided")
            return {"message": "Request body is required"}, 400

        form_id = data.get("form_id")
        form_data = data.get("form_data")
        submitted_at = data.get(
            "submitted_at",
            int(datetime.datetime.utcnow().timestamp() * 1000)
        )
        idempotency_key = data.get("idempotency_key")  # ✅ NEW: Get idempotency key

        if not form_id or not form_data:
            print("[SUBMISSION] Missing form_id or form_data")
            return {
                "message": "form_id and form_data are required"
            }, 400

        if not idempotency_key:  # ✅ NEW: Validate idempotency key
            print("[SUBMISSION] Missing idempotency_key")
            return {
                "message": "idempotency_key is required"
            }, 400

        # ✅ STEP 1: Check for existing submission with same idempotency_key
        print(f"[SUBMISSION] Checking for duplicate with idempotency_key: {idempotency_key}")
        
        existing_submission = FormSubmission.query.filter_by(
            idempotency_key=idempotency_key
        ).first()

        if existing_submission:
            print(f"[SUBMISSION] ✅ Duplicate detected! Returning existing submission #{existing_submission.id}")
            return {
                "status": "success",
                "submission_id": str(existing_submission.id),
                "message": "Submission already processed (duplicate request detected)",
                "submitted_at": submitted_at,
                "is_duplicate": True  # ✅ Flag to indicate this is a retry
            }, 201

        # ✅ STEP 2: Verify form exists
        form = Form.query.filter_by(
            id=form_id,
            is_active=True
        ).first()

        if not form:
            print(f"[SUBMISSION] Form {form_id} not found")
            return {
                "message": f"Form {form_id} not found"
            }, 404

        # ✅ STEP 3: Create new submission record
        submission = FormSubmission(
            form_id=form_id,
            idempotency_key=idempotency_key,  # ✅ STORE KEY
            sync_status="SYNCED",
            created_at=datetime.datetime.utcfromtimestamp(
                submitted_at / 1000
            ) if submitted_at else datetime.datetime.utcnow(),
            synced_at=datetime.datetime.utcnow()
        )

        # Store form data
        submission.set_form_data(form_data)

        # Store GPS location if provided
        gps_location = data.get("gps_location")
        if gps_location:
            submission.gps_latitude = gps_location.get("lat")
            submission.gps_longitude = gps_location.get("lng")

        db.session.add(submission)
        
        try:
            db.session.commit()
            print(f"[SUBMISSION] Form {form_id} submitted successfully - ID: {submission.id}")

        except IntegrityError as e:
            # ✅ Handle race condition: another request inserted same idempotency_key
            db.session.rollback()
            print(f"[SUBMISSION] Race condition detected: {str(e)}")
            
            # Retry lookup
            existing_submission = FormSubmission.query.filter_by(
                idempotency_key=idempotency_key
            ).first()
            
            if existing_submission:
                print(f"[SUBMISSION] ✅ Another request won, returning their submission #{existing_submission.id}")
                return {
                    "status": "success",
                    "submission_id": str(existing_submission.id),
                    "message": "Submission already processed (concurrent request detected)",
                    "submitted_at": submitted_at,
                    "is_duplicate": True
                }, 201
            else:
                print(f"[SUBMISSION] ❌ IntegrityError but no duplicate found: {str(e)}")
                return {
                    "status": "error",
                    "message": "Submission failed due to duplicate constraint"
                }, 409

        return {
            "status": "success",
            "submission_id": str(submission.id),
            "message": "Form submitted successfully",
            "submitted_at": submitted_at,
            "is_duplicate": False  # ✅ New submission
        }, 201

    except Exception as e:
        db.session.rollback()

        print(f"[SUBMISSION] Error in submit_form: {str(e)}")
        traceback.print_exc()

        return {
            "status": "error",
            "message": f"Error: {str(e)}"
        }, 500


# ============================================
# MEDIA UPLOAD ENDPOINT
# ============================================

@app.route("/media/upload", methods=["POST"])
@token_required
def upload_media(current_user):
    """
    Upload media file (photo/document)

    Multipart form data:
    - file
    - submission_id
    - field_id
    """

    print(f"\n[MEDIA] POST /media/upload called by {current_user}")

    try:

        if "file" not in request.files:
            return {
                "status": "error",
                "message": "No file provided"
            }, 400

        file = request.files["file"]

        submission_id = request.form.get("submission_id")
        field_id = request.form.get("field_id")

        if not submission_id:
            return {
                "status": "error",
                "message": "submission_id is required"
            }, 400

        if not field_id:
            return {
                "status": "error",
                "message": "field_id is required"
            }, 400

        if file.filename == "":
            return {
                "status": "error",
                "message": "No file selected"
            }, 400

        # Create uploads directory
        upload_dir = os.path.join(
            os.path.dirname(__file__),
            "uploads"
        )

        os.makedirs(upload_dir, exist_ok=True)

        # Generate unique filename
        timestamp = int(
            datetime.datetime.utcnow().timestamp() * 1000
        )

        original_extension = os.path.splitext(
            file.filename
        )[1]

        unique_filename = (
            f"{submission_id}_"
            f"{field_id}_"
            f"{timestamp}"
            f"{original_extension}"
        )

        file_path = os.path.join(
            upload_dir,
            unique_filename
        )

        # Save file
        file.save(file_path)

        # Build URL
        server_url = (
            f"http://localhost:5000/uploads/"
            f"{unique_filename}"
        )

        print(f"[MEDIA] Uploaded: {unique_filename}")

        return {
            "status": "success",
            "message": "File uploaded successfully",
            "file_name": unique_filename,
            "server_url": server_url
        }, 201

    except Exception as e:
        print(
            f"[MEDIA] Error in upload_media: {str(e)}",
            exc_info=True
        )

        return {
            "status": "error",
            "message": str(e)
        }, 500


# ============================================
# SERVE UPLOADED FILES
# ============================================

@app.route("/uploads/<filename>", methods=["GET"])
def download_file(filename):
    """Serve uploaded files"""
    print(f"\n[MEDIA] GET /uploads/{filename} called")
    
    try:
        upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
        file_path = os.path.join(upload_dir, filename)
        
        # Security: Prevent directory traversal
        if not os.path.abspath(file_path).startswith(os.path.abspath(upload_dir)):
            print("[MEDIA] Directory traversal attempt")
            return {"message": "Invalid file path"}, 403
        
        if not os.path.exists(file_path):
            print(f"[MEDIA] File not found: {filename}")
            return {"message": "File not found"}, 404
        
        from flask import send_file
        return send_file(file_path)
    
    except Exception as e:
        print(f"[MEDIA] Error in download_file: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


# ============================================
# ADMIN ENDPOINTS - FORM MANAGEMENT
# ============================================

@app.route("/admin/forms", methods=["POST"])
@token_required
def create_form(current_user):
    """
    Create a new form (ADMIN ONLY)
    
    Request body:
    {
        "id": "form_001",
        "name": "Form Name",
        "description": "Form description",
        "version": "1.0"
    }
    """
    print(f"\n[ADMIN] POST /admin/forms called by {current_user}")
    
    try:
        data = request.get_json()
        
        required_fields = ["id", "name"]
        if not all(field in data for field in required_fields):
            return {"message": "Missing required fields: id, name"}, 400
        
        existing_form = Form.query.filter_by(id=data["id"]).first()
        if existing_form:
            return {"message": f"Form with id {data['id']} already exists"}, 400
        
        new_form = Form(
            id=data["id"],
            name=data["name"],
            description=data.get("description", ""),
            version=data.get("version", "1.0")
        )
        
        db.session.add(new_form)
        db.session.commit()
        
        print(f"[ADMIN] Form {data['id']} created successfully by {current_user}")
        
        return {
            "status": "success",
            "message": "Form created successfully",
            "form": new_form.to_dict()
        }, 201
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in create_form: {str(e)}")
        return {"status": "error", "message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>", methods=["PUT"])
@token_required
def update_form(current_user, form_id):
    """
    Update form metadata and UI configuration (ADMIN ONLY)
    
    Supported fields:
    - name, description, version, is_active (metadata)
    - theme: {} (UI theme configuration)
    - layout: {} (Form layout configuration)
    - branding: {} (Branding configuration)
    """
    print(f"\n[ADMIN] PUT /admin/forms/{form_id} called by {current_user}")
    
    try:
        form = Form.query.filter_by(id=form_id).first()
        
        if not form:
            return {"status": "error", "message": f"Form {form_id} not found"}, 404
        
        data = request.get_json()
        
        # Update basic metadata
        if "name" in data:
            form.name = data["name"]
        if "description" in data:
            form.description = data["description"]
        if "version" in data:
            form.version = data["version"]
        if "is_active" in data:
            form.is_active = data["is_active"]
        
        # Update UI configuration
        #
        # NOTE: theme_json / layout_json / branding_json are JSONB columns.
        # SQLAlchemy + psycopg already handle Python dict <-> JSONB
        # (de)serialization automatically. Calling json.dumps() here used to
        # store a JSON-encoded STRING *inside* the JSONB column (i.e. the
        # column ended up holding '"{\"primaryColor\": \"#fff\", ...}"'
        # instead of the actual object). That corrupted value was then
        # returned as-is by to_dict_with_fields(), which:
        #   - broke the Admin Panel's Theme Editor on reload (it received a
        #     string instead of a ThemeConfig object), and
        #   - crashed the Android app with
        #     "java.lang.IllegalStateException: Expected BEGIN_OBJECT but
        #     was STRING ... path $.form.layout" because Gson could not
        #     parse a JSON string into the LayoutConfig data class.
        #
        # Fix: store the parsed dict/object directly and let JSONB handle
        # serialization. If a client sends a JSON-encoded string for one of
        # these fields (legacy behavior), decode it back into an object
        # instead of persisting the raw string.
        if "theme" in data:
            form.theme_json = _coerce_json_field(data["theme"])
        if "layout" in data:
            form.layout_json = _coerce_json_field(data["layout"])
        if "branding" in data:
            form.branding_json = _coerce_json_field(data["branding"])
        
        db.session.commit()
        
        print(f"[ADMIN] Form {form_id} updated successfully by {current_user}")
        
        return {
            "status": "success",
            "message": "Form updated successfully",
            "form": form.to_dict()
        }, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in update_form: {str(e)}")
        return {"status": "error", "message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>/fields", methods=["POST"])
@token_required
def add_field_to_form(current_user, form_id):
    """
    Add a field to a form (ADMIN ONLY)
    
    Request body:
    {
        "field_id": "field_001",
        "name": "Full Name",
        "type": "text",
        "required": true,
        "placeholder": "Enter your full name",
        "field_order": 1,
        "options": ["Option1", "Option2"],  // only for dropdown
        "help_text": "This is your legal name"
    }
    """
    print(f"\n[ADMIN] POST /admin/forms/{form_id}/fields called by {current_user}")
    
    try:
        form = Form.query.filter_by(id=form_id).first()
        
        if not form:
            return {"status": "error", "message": f"Form {form_id} not found"}, 404
        
        data = request.get_json()
        
        required_fields = ["field_id", "name", "type"]
        if not all(field in data for field in required_fields):
            return {"status": "error", "message": "Missing required fields: field_id, name, type"}, 400
        
        new_field = Field(
            form_id=form_id,
            field_id=data["field_id"],
            name=data["name"],
            type=data["type"],
            required=data.get("required", False),
            placeholder=data.get("placeholder"),
            field_order=data.get("field_order", 0),
            help_text=data.get("help_text"),
            default_value=data.get("default_value")
        )
        
        # Handle dropdown options
        if data["type"] in ["dropdown", "select"]:
            if "options" in data:
                new_field.set_options(data["options"])
        
        db.session.add(new_field)
        db.session.commit()
        
        print(f"[ADMIN] Field {data['field_id']} added to form {form_id} by {current_user}")
        
        return {
            "status": "success",
            "message": "Field added successfully",
            "field": new_field.to_dict()
        }, 201
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in add_field_to_form: {str(e)}")
        return {"status": "error", "message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>/fields/<int:field_db_id>", methods=["PUT"])
@token_required
def update_field(current_user, form_id, field_db_id):
    """
    ✅ COMPREHENSIVE FIELD UPDATE ENDPOINT
    
    Updates ALL field properties. This single endpoint supports:
    - Edit Field (name, type, required, placeholder, help_text)
    - Reorder Fields (field_order)
    - Dropdown Options (options)
    - Validation (validation)
    - Conditional Logic (visible_if, enabled_if)
    - Default Values (default_value)
    
    No additional endpoints needed for builder features.
    """
    print(f"\n[ADMIN] PUT /admin/forms/{form_id}/fields/{field_db_id} called by {current_user}")
    
    try:
        field = Field.query.filter_by(id=field_db_id, form_id=form_id).first()
        
        if not field:
            return {"status": "error", "message": "Field not found"}, 404
        
        data = request.get_json()
        
        # ✅ Update basic field properties
        if "name" in data:
            field.name = data["name"]
        if "type" in data:
            field.type = data["type"]
        if "required" in data:
            field.required = data["required"]
        if "placeholder" in data:
            field.placeholder = data["placeholder"]
        if "field_order" in data:
            field.field_order = data["field_order"]
        if "help_text" in data:
            field.help_text = data["help_text"]
        if "default_value" in data:
            field.default_value = data["default_value"]
        
        # ✅ Update complex properties (JSON-serialized)
        if "options" in data:
            field.set_options(data["options"])
        
        if "validation" in data:
            field.set_validation(data["validation"])
        
        if "visible_if" in data:
            field.set_visible_if(data["visible_if"])
        
        if "enabled_if" in data:
            field.set_enabled_if(data["enabled_if"])
        
        db.session.commit()
        
        print(f"[ADMIN] Field {field_db_id} updated successfully by {current_user}")
        
        return {
            "status": "success",
            "message": "Field updated successfully",
            "field": field.to_dict()
        }, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in update_field: {str(e)}")
        traceback.print_exc()
        return {"status": "error", "message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>/fields/<int:field_db_id>", methods=["DELETE"])
@token_required
def delete_field(current_user, form_id, field_db_id):
    """
    Delete a field from a form (ADMIN ONLY)
    """
    print(f"\n[ADMIN] DELETE /admin/forms/{form_id}/fields/{field_db_id} called by {current_user}")
    
    try:
        field = Field.query.filter_by(id=field_db_id, form_id=form_id).first()
        
        if not field:
            return {"status": "error", "message": "Field not found"}, 404
        
        db.session.delete(field)
        db.session.commit()
        
        print(f"[ADMIN] Field {field_db_id} deleted successfully by {current_user}")
        
        return {"status": "success", "message": "Field deleted successfully"}, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in delete_field: {str(e)}")
        return {"status": "error", "message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>", methods=["DELETE"])
@token_required
def delete_form(current_user, form_id):
    """
    Delete a form (ADMIN ONLY)
    """
    print(f"\n[ADMIN] DELETE /admin/forms/{form_id} called by {current_user}")
    
    try:
        form = Form.query.filter_by(id=form_id).first()
        
        if not form:
            return {"status": "error", "message": f"Form {form_id} not found"}, 404
        
        db.session.delete(form)
        db.session.commit()
        
        print(f"[ADMIN] Form {form_id} deleted successfully by {current_user}")
        
        return {"status": "success", "message": "Form deleted successfully"}, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in delete_form: {str(e)}")
        return {"status": "error", "message": f"Error: {str(e)}"}, 500


# ============================================
# SUBMISSION MANAGEMENT ENDPOINTS
# ============================================

@app.route("/forms/<form_id>/submissions", methods=["GET"])
def get_form_submissions(form_id):
    """
    Get all submissions for a specific form
    """
    print(f"\n[SUBMISSION] GET /forms/{form_id}/submissions called")

    try:
        form = Form.query.filter_by(id=form_id).first()

        if not form:
            return {
                "status": "error",
                "message": f"Form {form_id} not found"
            }, 404

        limit = request.args.get("limit", 50, type=int)
        offset = request.args.get("offset", 0, type=int)
        status_filter = request.args.get("status")

        query = FormSubmission.query.filter_by(form_id=form_id)

        if status_filter:
            query = query.filter_by(sync_status=status_filter)

        total = query.count()

        submissions = query.order_by(
            FormSubmission.created_at.desc()
        ).limit(limit).offset(offset).all()

        return {
            "status": "success",
            "submissions": [s.to_dict() for s in submissions],
            "count": len(submissions),
            "total": total
        }, 200

    except Exception as e:
        print(f"[SUBMISSION] Error: {str(e)}")
        return {
            "status": "error",
            "message": str(e)
        }, 500


@app.route("/submissions/<int:submission_id>", methods=["GET"])
def get_submission(submission_id):
    """
    Get a specific submission
    """
    print(f"\n[SUBMISSION] GET /submissions/{submission_id} called")

    try:
        submission = FormSubmission.query.filter_by(
            id=submission_id
        ).first()

        if not submission:
            return {
                "status": "error",
                "message": f"Submission {submission_id} not found"
            }, 404

        return {
            "status": "success",
            "submission": submission.to_dict()
        }, 200

    except Exception as e:
        print(f"[SUBMISSION] Error: {str(e)}")
        return {
            "status": "error",
            "message": str(e)
        }, 500


@app.route("/admin/submissions", methods=["GET"])
def get_all_submissions():
    """
    Get all submissions
    """
    print("\n[ADMIN] GET /admin/submissions called")

    try:
        query = FormSubmission.query

        form_id_filter = request.args.get("form_id")
        if form_id_filter:
            query = query.filter_by(form_id=form_id_filter)

        status_filter = request.args.get("status")
        if status_filter:
            query = query.filter_by(sync_status=status_filter)

        limit = request.args.get("limit", 50, type=int)
        offset = request.args.get("offset", 0, type=int)

        total = query.count()

        submissions = query.order_by(
            FormSubmission.created_at.desc()
        ).limit(limit).offset(offset).all()

        return {
            "status": "success",
            "submissions": [s.to_dict() for s in submissions],
            "count": len(submissions),
            "total": total
        }, 200

    except Exception as e:
        print(f"[ADMIN] Error: {str(e)}")
        return {
            "status": "error",
            "message": str(e)
        }, 500


@app.route("/admin/submissions/<int:submission_id>", methods=["DELETE"])
def delete_submission(submission_id):
    """
    Delete a submission
    """
    print(f"\n[ADMIN] DELETE /admin/submissions/{submission_id} called")

    try:
        submission = FormSubmission.query.filter_by(
            id=submission_id
        ).first()

        if not submission:
            return {
                "status": "error",
                "message": f"Submission {submission_id} not found"
            }, 404

        db.session.delete(submission)
        db.session.commit()

        return {
            "status": "success",
            "message": "Submission deleted"
        }, 200

    except Exception as e:
        db.session.rollback()

        print(f"[ADMIN] Error: {str(e)}")

        return {
            "status": "error",
            "message": str(e)
        }, 500


if __name__ == "__main__":
    print("\n" + "="*50)
    print("Starting JSAC Backend...")
    print("="*50)
    
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )