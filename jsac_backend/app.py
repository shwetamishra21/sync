import os
from models.submission_model import FormSubmission
from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_bcrypt import Bcrypt
import jwt
import datetime
from datetime import timedelta
import secrets
from dotenv import load_dotenv
from database.db import db
from models.user_model import User
from models.form_model import Form
from models.field_model import Field
import json
from functools import wraps
import mimetypes

app = Flask(__name__)
load_dotenv()

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
reset_tokens = {}

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

        new_user = User(
            username=username,
            password=hashed_password
        )

        db.session.add(new_user)
        db.session.commit()

        print(f"[AUTH] User {username} registered successfully")
        
        return {
            "message": "Registration successful"
        }, 201
    
    except Exception as e:
        print(f"[AUTH] Error in register: {str(e)}")
        db.session.rollback()
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


# ============================================
# FORGOT PASSWORD ENDPOINTS
# ============================================

@app.route('/forgot-password', methods=['POST'])
def forgot_password():
    print("\n[PASSWORD] POST /forgot-password called")
    
    try:
        data = request.get_json()
        username = data.get('username')
        
        if not username:
            print("[PASSWORD] Username is required")
            return {"message": "Username is required"}, 400
        
        user = User.query.filter_by(
            username=username
        ).first()
        
        if not user:
            print(f"[PASSWORD] User {username} not found")
            return {"message": "User not found"}, 404
        
        reset_token = secrets.token_urlsafe(32)
        reset_tokens[reset_token] = {
            'username': username,
            'created_at': datetime.datetime.utcnow(),
            'expires_at': datetime.datetime.utcnow() + timedelta(hours=1)
        }
        
        print(f"\n{'='*50}")
        print(f"Reset token for {username}:")
        print(f"Token: {reset_token}")
        print(f"Expires at: {reset_tokens[reset_token]['expires_at']}")
        print(f"{'='*50}\n")
        
        return {
            "message": "Reset link sent",
            "reset_token": reset_token
        }, 200
    
    except Exception as e:
        print(f"[PASSWORD] Error in forgot_password: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route('/reset-password', methods=['POST'])
def reset_password():
    print("\n[PASSWORD] POST /reset-password called")
    
    try:
        data = request.get_json()
        username = data.get('username')
        reset_token = data.get('reset_token')
        new_password = data.get('new_password')
        
        if not all([username, reset_token, new_password]):
            print("[PASSWORD] Missing required fields")
            return {"message": "Username, token, and password are required"}, 400
        
        if len(new_password) < 6:
            print("[PASSWORD] Password too short")
            return {"message": "Password must be at least 6 characters"}, 400
        
        if reset_token not in reset_tokens:
            print("[PASSWORD] Invalid or expired token")
            return {"message": "Invalid or expired token"}, 400
        
        token_data = reset_tokens[reset_token]
        
        if token_data['username'] != username:
            print("[PASSWORD] Username does not match token")
            return {"message": "Username does not match token"}, 400
        
        if datetime.datetime.utcnow() > token_data['expires_at']:
            del reset_tokens[reset_token]
            print("[PASSWORD] Token has expired")
            return {"message": "Token has expired"}, 400
        
        user = User.query.filter_by(
            username=username
        ).first()
        
        if not user:
            print(f"[PASSWORD] User {username} not found")
            return {"message": "User not found"}, 404
        
        hashed_password = bcrypt.generate_password_hash(
            new_password
        ).decode("utf-8")
        user.password = hashed_password
        
        db.session.commit()
        
        print(f"\n{'='*50}")
        print(f"Password reset for: {username}")
        print(f"{'='*50}\n")
        
        del reset_tokens[reset_token]
        
        return {"message": "Password reset successfully"}, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[PASSWORD] Error in reset_password: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route('/check-reset-token', methods=['POST'])
def check_reset_token():
    print("\n[PASSWORD] POST /check-reset-token called")
    
    try:
        data = request.get_json()
        reset_token = data.get('reset_token')
        
        if not reset_token:
            print("[PASSWORD] Token is required")
            return {"message": "Token is required"}, 400
        
        if reset_token not in reset_tokens:
            print("[PASSWORD] Invalid or expired token")
            return {
                "valid": False,
                "message": "Invalid or expired token"
            }, 400
        
        token_data = reset_tokens[reset_token]
        
        if datetime.datetime.utcnow() > token_data['expires_at']:
            del reset_tokens[reset_token]
            print("[PASSWORD] Token has expired")
            return {
                "valid": False,
                "message": "Token has expired"
            }, 400
        
        time_remaining = token_data['expires_at'] - datetime.datetime.utcnow()
        
        print(f"[PASSWORD] Token is valid, {int(time_remaining.total_seconds())}s remaining")
        
        return {
            "valid": True,
            "username": token_data['username'],
            "expires_at": token_data['expires_at'].isoformat(),
            "time_remaining_seconds": int(time_remaining.total_seconds())
        }, 200
    
    except Exception as e:
        print(f"[PASSWORD] Error in check_reset_token: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


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
    Submit a completed form
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

        if not form_id or not form_data:
            print("[SUBMISSION] Missing form_id or form_data")
            return {
                "message": "form_id and form_data are required"
            }, 400

        # Verify form exists
        form = Form.query.filter_by(
            id=form_id,
            is_active=True
        ).first()

        if not form:
            print(f"[SUBMISSION] Form {form_id} not found")
            return {
                "message": f"Form {form_id} not found"
            }, 404

        # Create submission record
        submission = FormSubmission(
            form_id=form_id,
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
        db.session.commit()

        print(
            f"[SUBMISSION] Form {form_id} submitted - "
            f"ID: {submission.id}"
        )

        return {
            "status": "success",
            "submission_id": str(submission.id),
            "message": "Form submitted successfully",
            "submitted_at": submitted_at
        }, 201

    except Exception as e:
        db.session.rollback()

        print(
            f"[SUBMISSION] Error in submit_form: {str(e)}",
            exc_info=True
        )

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
            "message": "Form created successfully",
            "form": new_form.to_dict()
        }, 201
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in create_form: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>", methods=["PUT"])
@token_required
def update_form(current_user, form_id):
    """
    Update form metadata (ADMIN ONLY)
    """
    print(f"\n[ADMIN] PUT /admin/forms/{form_id} called by {current_user}")
    
    try:
        form = Form.query.filter_by(id=form_id).first()
        
        if not form:
            return {"message": f"Form {form_id} not found"}, 404
        
        data = request.get_json()
        
        if "name" in data:
            form.name = data["name"]
        if "description" in data:
            form.description = data["description"]
        if "version" in data:
            form.version = data["version"]
        if "is_active" in data:
            form.is_active = data["is_active"]
        
        db.session.commit()
        
        print(f"[ADMIN] Form {form_id} updated successfully by {current_user}")
        
        return {
            "message": "Form updated successfully",
            "form": form.to_dict()
        }, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in update_form: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


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
            return {"message": f"Form {form_id} not found"}, 404
        
        data = request.get_json()
        
        required_fields = ["field_id", "name", "type"]
        if not all(field in data for field in required_fields):
            return {"message": "Missing required fields: field_id, name, type"}, 400
        
        new_field = Field(
            form_id=form_id,
            field_id=data["field_id"],
            name=data["name"],
            type=data["type"],
            required=data.get("required", False),
            placeholder=data.get("placeholder"),
            field_order=data.get("field_order", 0),
            help_text=data.get("help_text")
        )
        
        # Handle dropdown options
        if data["type"] in ["dropdown", "select"]:
            if "options" in data:
                new_field.set_options(data["options"])
        
        db.session.add(new_field)
        db.session.commit()
        
        print(f"[ADMIN] Field {data['field_id']} added to form {form_id} by {current_user}")
        
        return {
            "message": "Field added successfully",
            "field": new_field.to_dict()
        }, 201
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in add_field_to_form: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>/fields/<int:field_db_id>", methods=["PUT"])
@token_required
def update_field(current_user, form_id, field_db_id):
    """
    Update a field in a form (ADMIN ONLY)
    """
    print(f"\n[ADMIN] PUT /admin/forms/{form_id}/fields/{field_db_id} called by {current_user}")
    
    try:
        field = Field.query.filter_by(id=field_db_id, form_id=form_id).first()
        
        if not field:
            return {"message": f"Field not found"}, 404
        
        data = request.get_json()
        
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
        if "options" in data:
            field.set_options(data["options"])
        
        db.session.commit()
        
        print(f"[ADMIN] Field {field_db_id} updated successfully by {current_user}")
        
        return {
            "message": "Field updated successfully",
            "field": field.to_dict()
        }, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in update_field: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


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
            return {"message": f"Field not found"}, 404
        
        db.session.delete(field)
        db.session.commit()
        
        print(f"[ADMIN] Field {field_db_id} deleted successfully by {current_user}")
        
        return {"message": "Field deleted successfully"}, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in delete_field: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


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
            return {"message": f"Form {form_id} not found"}, 404
        
        db.session.delete(form)
        db.session.commit()
        
        print(f"[ADMIN] Form {form_id} deleted successfully by {current_user}")
        
        return {"message": "Form deleted successfully"}, 200
    
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in delete_form: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


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