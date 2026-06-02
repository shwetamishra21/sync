import os
from flask import Flask, request
from flask_cors import CORS
from flask_bcrypt import Bcrypt
import jwt
import datetime
from datetime import timedelta
import secrets
from dotenv import load_dotenv
from database.db import db
from models.user_model import User

app = Flask(__name__)
load_dotenv()

app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv(
    "DATABASE_URL"
)

app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

db.init_app(app)

# Verify Database Connection
with app.app_context():
    try:
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
# FORM TEMPLATES DATABASE
# ============================================

forms_db = [
    {
        "id": "form_001",
        "name": "Citizen Service Request",
        "description": "Request government services",
        "version": "1.0",
        "created_at": "2026-01-01",
        "fields": [
            {
                "id": "field_001",
                "name": "Full Name",
                "type": "text",
                "required": True,
                "placeholder": "Enter your full name"
            },
            {
                "id": "field_002",
                "name": "Email",
                "type": "email",
                "required": True,
                "placeholder": "Enter your email"
            },
            {
                "id": "field_003",
                "name": "Service Type",
                "type": "dropdown",
                "required": True,
                "options": ["Birth Certificate", "License", "Permit", "Other"]
            },
            {
                "id": "field_004",
                "name": "Description",
                "type": "textarea",
                "required": False,
                "placeholder": "Describe your request"
            }
        ]
    },
    {
        "id": "form_002",
        "name": "Property Registration",
        "description": "Register property with government",
        "version": "1.0",
        "created_at": "2026-01-02",
        "fields": [
            {
                "id": "field_101",
                "name": "Property Address",
                "type": "text",
                "required": True,
                "placeholder": "Enter property address"
            },
            {
                "id": "field_102",
                "name": "Property Type",
                "type": "dropdown",
                "required": True,
                "options": ["Residential", "Commercial", "Agricultural"]
            },
            {
                "id": "field_103",
                "name": "Area (sq ft)",
                "type": "number",
                "required": True,
                "placeholder": "Enter area"
            }
        ]
    },
    {
        "id": "form_003",
        "name": "Business License Application",
        "description": "Apply for a business license",
        "version": "1.0",
        "created_at": "2026-01-03",
        "fields": [
            {
                "id": "field_201",
                "name": "Business Name",
                "type": "text",
                "required": True,
                "placeholder": "Enter business name"
            },
            {
                "id": "field_202",
                "name": "Business Type",
                "type": "dropdown",
                "required": True,
                "options": ["Retail", "Service", "Manufacturing", "Technology"]
            },
            {
                "id": "field_203",
                "name": "Owner Email",
                "type": "email",
                "required": True,
                "placeholder": "owner@business.com"
            }
        ]
    }
]


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
    """
    Request password reset token
    
    Expected JSON:
    {
        "username": "user@example.com"
    }
    """
    print("\n[PASSWORD] POST /forgot-password called")
    
    try:
        data = request.get_json()
        username = data.get('username')
        
        # Validate username
        if not username:
            print("[PASSWORD] Username is required")
            return {"message": "Username is required"}, 400
        
        # Check if user exists in database
        user = User.query.filter_by(
            username=username
        ).first()
        
        if not user:
            print(f"[PASSWORD] User {username} not found")
            return {"message": "User not found"}, 404
        
        # Generate secure reset token
        reset_token = secrets.token_urlsafe(32)
        reset_tokens[reset_token] = {
            'username': username,
            'created_at': datetime.datetime.utcnow(),
            'expires_at': datetime.datetime.utcnow() + timedelta(hours=1)
        }
        
        # For development/testing, print token to console
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
    """
    Reset password with token
    
    Expected JSON:
    {
        "username": "user@example.com",
        "reset_token": "token_from_email",
        "new_password": "newpassword123"
    }
    """
    print("\n[PASSWORD] POST /reset-password called")
    
    try:
        data = request.get_json()
        username = data.get('username')
        reset_token = data.get('reset_token')
        new_password = data.get('new_password')
        
        # Validate inputs
        if not all([username, reset_token, new_password]):
            print("[PASSWORD] Missing required fields")
            return {"message": "Username, token, and password are required"}, 400
        
        if len(new_password) < 6:
            print("[PASSWORD] Password too short")
            return {"message": "Password must be at least 6 characters"}, 400
        
        # Verify token exists
        if reset_token not in reset_tokens:
            print("[PASSWORD] Invalid or expired token")
            return {"message": "Invalid or expired token"}, 400
        
        token_data = reset_tokens[reset_token]
        
        # Verify username matches
        if token_data['username'] != username:
            print("[PASSWORD] Username does not match token")
            return {"message": "Username does not match token"}, 400
        
        # Verify token not expired
        if datetime.datetime.utcnow() > token_data['expires_at']:
            del reset_tokens[reset_token]
            print("[PASSWORD] Token has expired")
            return {"message": "Token has expired"}, 400
        
        # Find user in database
        user = User.query.filter_by(
            username=username
        ).first()
        
        if not user:
            print(f"[PASSWORD] User {username} not found")
            return {"message": "User not found"}, 404
        
        # Hash and update password
        hashed_password = bcrypt.generate_password_hash(
            new_password
        ).decode("utf-8")
        user.password = hashed_password
        
        db.session.commit()
        
        print(f"\n{'='*50}")
        print(f"Password reset for: {username}")
        print(f"{'='*50}\n")
        
        # Delete used token
        del reset_tokens[reset_token]
        
        return {"message": "Password reset successfully"}, 200
    
    except Exception as e:
        print(f"[PASSWORD] Error in reset_password: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route('/check-reset-token', methods=['POST'])
def check_reset_token():
    """
    Verify if reset token is valid (optional endpoint for testing)
    """
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
# FORM ENDPOINTS
# ============================================

@app.route("/forms", methods=["GET"])
def get_forms():
    print("\n[FORMS] GET /forms called")
    """
    Get list of available forms
    
    Returns:
    - List of form metadata (id, name, description, version)
    - NOT the full form fields (for performance)
    """
    try:
        # Return only metadata, not full field definitions
        form_list = [
            {
                "id": form["id"],
                "name": form["name"],
                "description": form["description"],
                "version": form["version"],
                "created_at": form["created_at"],
                "field_count": len(form["fields"])
            }
            for form in forms_db
        ]
        
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
    print(f"\n[FORMS] GET /forms/{form_id} called")
    """
    Get complete form definition including all fields
    
    Args:
    - form_id: The form identifier
    
    Returns:
    - Complete form with all fields and configurations
    """
    try:
        # Find form by ID
        form = None
        for f in forms_db:
            if f["id"] == form_id:
                form = f
                break
        
        if not form:
            print(f"[FORMS] Form {form_id} not found")
            return {
                "message": f"Form {form_id} not found"
            }, 404
        
        print(f"[FORMS] Form {form_id} found with {len(form['fields'])} fields")
        
        return {
            "status": "success",
            "form": form
        }, 200
    
    except Exception as e:
        print(f"[FORMS] Error in get_form_detail: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


if __name__ == "__main__":
    print("\n" + "="*50)
    print("Starting JSAC Backend...")
    print("="*50)
    
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )