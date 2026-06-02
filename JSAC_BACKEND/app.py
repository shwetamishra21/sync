cat > /mnt/user-data/outputs/app_updated.py << 'EOF'
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
    """
    try:
        data = request.get_json()
        username = data.get('username')
        
        # Validate username
        if not username:
            return {"message": "Username is required"}, 400
        
        # Check if user exists
        user = None
        for u in users:
            if u["username"] == username:
                user = u
                break
        
        if not user:
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
        print(f"Error in forgot_password: {str(e)}")
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
    try:
        data = request.get_json()
        username = data.get('username')
        reset_token = data.get('reset_token')
        new_password = data.get('new_password')
        
        # Validate inputs
        if not all([username, reset_token, new_password]):
            return {"message": "Username, token, and password are required"}, 400
        
        if len(new_password) < 6:
            return {"message": "Password must be at least 6 characters"}, 400
        
        # Verify token exists
        if reset_token not in reset_tokens:
            return {"message": "Invalid or expired token"}, 400
        
        token_data = reset_tokens[reset_token]
        
        # Verify username matches
        if token_data['username'] != username:
            return {"message": "Username does not match token"}, 400
        
        # Verify token not expired
        if datetime.datetime.utcnow() > token_data['expires_at']:
            del reset_tokens[reset_token]
            return {"message": "Token has expired"}, 400
        
        # Find and update user password
        user = None
        for u in users:
            if u["username"] == username:
                user = u
                break
        
        if not user:
            return {"message": "User not found"}, 404
        
        # Hash and update password
        hashed_password = bcrypt.generate_password_hash(
            new_password
        ).decode("utf-8")
        user["password"] = hashed_password
        
        print(f"\n{'='*50}")
        print(f"Password reset for: {username}")
        print(f"{'='*50}\n")
        
        # Delete used token
        del reset_tokens[reset_token]
        
        return {"message": "Password reset successfully"}, 200
    
    except Exception as e:
        print(f"Error in reset_password: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


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

    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )
EOF
cat /mnt/user-data/outputs/app_updated.py
Output

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
    """
    try:
        data = request.get_json()
        username = data.get('username')
        
        # Validate username
        if not username:
            return {"message": "Username is required"}, 400
        
        # Check if user exists
        user = None
        for u in users:
            if u["username"] == username:
                user = u
                break
        
        if not user:
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
        print(f"Error in forgot_password: {str(e)}")
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
    try:
        data = request.get_json()
        username = data.get('username')
        reset_token = data.get('reset_token')
        new_password = data.get('new_password')
        
        # Validate inputs
        if not all([username, reset_token, new_password]):
            return {"message": "Username, token, and password are required"}, 400
        
        if len(new_password) < 6:
            return {"message": "Password must be at least 6 characters"}, 400
        
        # Verify token exists
        if reset_token not in reset_tokens:
            return {"message": "Invalid or expired token"}, 400
        
        token_data = reset_tokens[reset_token]
        
        # Verify username matches
        if token_data['username'] != username:
            return {"message": "Username does not match token"}, 400
        
        # Verify token not expired
        if datetime.datetime.utcnow() > token_data['expires_at']:
            del reset_tokens[reset_token]
            return {"message": "Token has expired"}, 400
        
        # Find and update user password
        user = None
        for u in users:
            if u["username"] == username:
                user = u
                break
        
        if not user:
            return {"message": "User not found"}, 404
        
        # Hash and update password
        hashed_password = bcrypt.generate_password_hash(
            new_password
        ).decode("utf-8")
        user["password"] = hashed_password
        
        print(f"\n{'='*50}")
        print(f"Password reset for: {username}")
        print(f"{'='*50}\n")
        
        # Delete used token
        del reset_tokens[reset_token]
        
        return {"message": "Password reset successfully"}, 200
    
    except Exception as e:
        print(f"Error in reset_password: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


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

    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )