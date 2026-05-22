from flask import Flask, request
from flask_cors import CORS
from flask_bcrypt import Bcrypt
import jwt
import datetime
from datetime import timedelta
import secrets

app = Flask(__name__)

CORS(app)

bcrypt = Bcrypt(app)

# Secret key for JWT
app.config["SECRET_KEY"] = "jsac_secret_key"

# Fake in-memory database
users = []

# Store reset tokens (use database in production)
reset_tokens = {}


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


# REGISTER
@app.route("/register", methods=["POST"])
def register():

    data = request.get_json()

    username = data.get("username")
    password = data.get("password")

    # Check existing user
    for user in users:

        if user["username"] == username:

            return {
                "message": "User already exists"
            }, 400

    hashed_password = bcrypt.generate_password_hash(
        password
    ).decode("utf-8")

    users.append({
        "username": username,
        "password": hashed_password
    })

    return {
        "message": "Registration successful"
    }, 201


# LOGIN
@app.route("/login", methods=["POST"])
def login():

    data = request.get_json()

    username = data.get("username")
    password = data.get("password")

    user = None

    for u in users:

        if u["username"] == username:

            user = u
            break

    if not user:

        return {
            "message": "User not found"
        }, 404

    if not bcrypt.check_password_hash(
        user["password"],
        password
    ):

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

    return {
        "token": token,
        "username": username
    }


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
    
    ✅ FIXED: Added detailed logging to debug issues
    """
    try:
        # ✅ NEW: Log raw request data
        print(f"\n{'='*60}")
        print(f"🔐 FORGOT PASSWORD REQUEST RECEIVED")
        print(f"{'='*60}")
        print(f"📡 Raw data: {request.get_json()}")
        
        data = request.get_json()
        
        if not data:
            print(f"❌ ERROR: No JSON body received")
            return {"message": "Invalid request body"}, 400
        
        username = data.get('username')
        
        print(f"📝 Extracted username: '{username}'")
        print(f"📝 Username type: {type(username)}")
        print(f"📝 Username empty?: {not username}")
        
        # Validate username
        if not username:
            print(f"❌ ERROR: Username is empty or missing")
            return {"message": "Username is required"}, 400
        
        # ✅ NEW: Log all registered users
        print(f"\n📊 Current users in database: {len(users)}")
        for idx, u in enumerate(users, 1):
            print(f"   {idx}. {u['username']}")
        
        # Check if user exists
        user = None
        for u in users:
            if u["username"] == username:
                user = u
                break
        
        if not user:
            print(f"❌ USER NOT FOUND: '{username}' is not registered")
            print(f"{'='*60}\n")
            return {"message": "User not found"}, 404
        
        # Generate secure reset token
        reset_token = secrets.token_urlsafe(32)
        reset_tokens[reset_token] = {
            'username': username,
            'created_at': datetime.datetime.utcnow(),
            'expires_at': datetime.datetime.utcnow() + timedelta(hours=1)
        }
        
        # ✅ NEW: Better formatted output
        print(f"\n✅ USER FOUND: {username}")
        print(f"🔑 Reset token generated:")
        print(f"   Token: {reset_token}")
        print(f"   Expires: {reset_tokens[reset_token]['expires_at'].isoformat()}")
        print(f"   Tokens in memory: {len(reset_tokens)}")
        print(f"{'='*60}\n")
        
        return {
            "message": "Reset link sent",
            "reset_token": reset_token
        }, 200
    
    except Exception as e:
        print(f"❌ EXCEPTION in forgot_password: {type(e).__name__}: {str(e)}")
        print(f"{'='*60}\n")
        import traceback
        traceback.print_exc()
        return {"message": f"Server error: {str(e)}"}, 500


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
    
    ✅ FIXED: Added detailed logging
    """
    try:
        print(f"\n{'='*60}")
        print(f"🔄 RESET PASSWORD REQUEST RECEIVED")
        print(f"{'='*60}")
        
        data = request.get_json()
        print(f"📡 Raw data: {data}")
        
        username = data.get('username')
        reset_token = data.get('reset_token')
        new_password = data.get('new_password')
        
        print(f"📝 Username: {username}")
        print(f"📝 Token length: {len(reset_token) if reset_token else 0}")
        print(f"📝 Password length: {len(new_password) if new_password else 0}")
        
        # Validate inputs
        if not all([username, reset_token, new_password]):
            print(f"❌ Missing required fields")
            return {"message": "Username, token, and password are required"}, 400
        
        if len(new_password) < 6:
            print(f"❌ Password too short")
            return {"message": "Password must be at least 6 characters"}, 400
        
        # Verify token exists
        if reset_token not in reset_tokens:
            print(f"❌ Token not found in memory")
            print(f"   Available tokens: {list(reset_tokens.keys())[:3]}...")
            return {"message": "Invalid or expired token"}, 400
        
        token_data = reset_tokens[reset_token]
        
        # Verify username matches
        if token_data['username'] != username:
            print(f"❌ Username mismatch: token has '{token_data['username']}', got '{username}'")
            return {"message": "Username does not match token"}, 400
        
        # Verify token not expired
        if datetime.datetime.utcnow() > token_data['expires_at']:
            del reset_tokens[reset_token]
            print(f"❌ Token expired")
            return {"message": "Token has expired"}, 400
        
        # Find and update user password
        user = None
        for u in users:
            if u["username"] == username:
                user = u
                break
        
        if not user:
            print(f"❌ User not found during reset")
            return {"message": "User not found"}, 404
        
        # Hash and update password
        hashed_password = bcrypt.generate_password_hash(
            new_password
        ).decode("utf-8")
        user["password"] = hashed_password
        
        print(f"\n✅ PASSWORD RESET SUCCESSFUL for: {username}")
        print(f"   New password hashed and stored")
        
        # Delete used token
        del reset_tokens[reset_token]
        print(f"   Token removed from memory")
        print(f"{'='*60}\n")
        
        return {"message": "Password reset successfully"}, 200
    
    except Exception as e:
        print(f"❌ EXCEPTION in reset_password: {type(e).__name__}: {str(e)}")
        print(f"{'='*60}\n")
        import traceback
        traceback.print_exc()
        return {"message": f"Server error: {str(e)}"}, 500


@app.route('/check-reset-token', methods=['POST'])
def check_reset_token():
    """
    Verify if reset token is valid (optional endpoint for testing)
    """
    try:
        data = request.get_json()
        reset_token = data.get('reset_token')
        
        if not reset_token:
            return {"message": "Token is required"}, 400
        
        if reset_token not in reset_tokens:
            return {
                "valid": False,
                "message": "Invalid or expired token"
            }, 400
        
        token_data = reset_tokens[reset_token]
        
        if datetime.datetime.utcnow() > token_data['expires_at']:
            del reset_tokens[reset_token]
            return {
                "valid": False,
                "message": "Token has expired"
            }, 400
        
        time_remaining = token_data['expires_at'] - datetime.datetime.utcnow()
        
        return {
            "valid": True,
            "username": token_data['username'],
            "expires_at": token_data['expires_at'].isoformat(),
            "time_remaining_seconds": int(time_remaining.total_seconds())
        }, 200
    
    except Exception as e:
        return {"message": f"Error: {str(e)}"}, 500


if __name__ == "__main__":
    print(f"\n{'='*60}")
    print(f"🚀 JSAC BACKEND STARTING")
    print(f"{'='*60}")
    print(f"✅ Flask running on http://0.0.0.0:5000")
    print(f"✅ CORS enabled for all origins")
    print(f"✅ Debug mode ON")
    print(f"{'='*60}\n")

    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )