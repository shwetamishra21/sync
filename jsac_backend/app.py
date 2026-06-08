import os
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

app = Flask(__name__)
load_dotenv()

app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv("DATABASE_URL")
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

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


# ============================================
# ADMIN ENDPOINTS - FORM MANAGEMENT
# ============================================

@app.route("/admin/forms", methods=["POST"])
def create_form():
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
    print("\n[ADMIN] POST /admin/forms called")
    
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
        
        print(f"[ADMIN] Form {data['id']} created successfully")
        
        return {
            "message": "Form created successfully",
            "form": new_form.to_dict()
        }, 201
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in create_form: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>", methods=["PUT"])
def update_form(form_id):
    """
    Update form metadata (ADMIN ONLY)
    """
    print(f"\n[ADMIN] PUT /admin/forms/{form_id} called")
    
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
        
        print(f"[ADMIN] Form {form_id} updated successfully")
        
        return {
            "message": "Form updated successfully",
            "form": form.to_dict()
        }, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in update_form: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>/fields", methods=["POST"])
def add_field_to_form(form_id):
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
    print(f"\n[ADMIN] POST /admin/forms/{form_id}/fields called")
    
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
        
        print(f"[ADMIN] Field {data['field_id']} added to form {form_id}")
        
        return {
            "message": "Field added successfully",
            "field": new_field.to_dict()
        }, 201
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in add_field_to_form: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>/fields/<int:field_db_id>", methods=["PUT"])
def update_field(form_id, field_db_id):
    """
    Update a field in a form (ADMIN ONLY)
    """
    print(f"\n[ADMIN] PUT /admin/forms/{form_id}/fields/{field_db_id} called")
    
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
        
        print(f"[ADMIN] Field {field_db_id} updated successfully")
        
        return {
            "message": "Field updated successfully",
            "field": field.to_dict()
        }, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in update_field: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>/fields/<int:field_db_id>", methods=["DELETE"])
def delete_field(form_id, field_db_id):
    """
    Delete a field from a form (ADMIN ONLY)
    """
    print(f"\n[ADMIN] DELETE /admin/forms/{form_id}/fields/{field_db_id} called")
    
    try:
        field = Field.query.filter_by(id=field_db_id, form_id=form_id).first()
        
        if not field:
            return {"message": f"Field not found"}, 404
        
        db.session.delete(field)
        db.session.commit()
        
        print(f"[ADMIN] Field {field_db_id} deleted successfully")
        
        return {"message": "Field deleted successfully"}, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in delete_field: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


@app.route("/admin/forms/<form_id>", methods=["DELETE"])
def delete_form(form_id):
    """
    Delete a form (ADMIN ONLY)
    """
    print(f"\n[ADMIN] DELETE /admin/forms/{form_id} called")
    
    try:
        form = Form.query.filter_by(id=form_id).first()
        
        if not form:
            return {"message": f"Form {form_id} not found"}, 404
        
        db.session.delete(form)
        db.session.commit()
        
        print(f"[ADMIN] Form {form_id} deleted successfully")
        
        return {"message": "Form deleted successfully"}, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[ADMIN] Error in delete_form: {str(e)}")
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