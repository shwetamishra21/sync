from flask import Blueprint, request, jsonify
from app.models.user_model import User
from app.database.db import db
from app import bcrypt
from app.utils.jwt_helper import generate_token

auth_bp = Blueprint("auth", __name__)

# REGISTER
@auth_bp.route("/register", methods=["POST"])
def register():

    data = request.get_json()

    name = data.get("name")
    email = data.get("email")
    password = data.get("password")

    # Check existing user
    existing_user = User.query.filter_by(email=email).first()

    if existing_user:
        return jsonify({
            "status": "error",
            "message": "Email already registered"
        }), 400

    # Hash password
    hashed_password = bcrypt.generate_password_hash(password).decode("utf-8")

    # Create user
    new_user = User(
        name=name,
        email=email,
        password=hashed_password
    )

    db.session.add(new_user)
    db.session.commit()

    return jsonify({
        "status": "success",
        "message": "User registered successfully",
        "user": new_user.to_dict()
    }), 201


# LOGIN
@auth_bp.route("/login", methods=["POST"])
def login():

    data = request.get_json()

    email = data.get("email")
    password = data.get("password")

    user = User.query.filter_by(email=email).first()

    if not user:
        return jsonify({
            "status": "error",
            "message": "User not found"
        }), 404

    is_correct = bcrypt.check_password_hash(user.password, password)

    if not is_correct:
        return jsonify({
            "status": "error",
            "message": "Invalid password"
        }), 401

    token = generate_token(user.id)

    return jsonify({
        "status": "success",
        "message": "Login successful",
        "token": token,
        "user": user.to_dict()
    }), 200