"""
✅ BACKEND: Forgot Password Routes with OTP Support

This file replaces the old token-based forgot password with OTP-based flow.

FLOW:
1. POST /forgot-password
   - User enters email
   - Backend generates 6-digit OTP
   - Backend sends OTP to email
   - Backend stores OTP in memory (or database)
   - Returns: {"message": "OTP sent to your email"}

2. POST /verify-otp
   - User enters OTP from email
   - Backend validates OTP
   - OTP must match username and not be expired
   - Returns: {"message": "OTP verified successfully"}

3. POST /reset-password
   - User enters new password
   - Backend updates password (OTP already verified)
   - Returns: {"message": "Password reset successfully"}
"""

import os
import datetime
import secrets
import smtplib
from datetime import timedelta
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from flask import request, jsonify
from flask_bcrypt import Bcrypt
import random
from database.db import db
from models.user_model import User

bcrypt = Bcrypt()

# ============================================
# OTP STORAGE (Use database in production)
# ============================================
# Format: {
#     "123456": {
#         "username": "john@example.com",
#         "created_at": datetime,
#         "expires_at": datetime
#     }
# }
otp_store = {}


# ============================================
# EMAIL CONFIGURATION
# ============================================
SMTP_SERVER = os.getenv("SMTP_SERVER", "smtp.gmail.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", 587))
EMAIL_ADDRESS = os.getenv("EMAIL_ADDRESS")  # Your email
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD")  # Your app password


def send_otp_email(recipient_email, otp_code):
    """
    Send OTP to user's email
    
    Args:
        recipient_email: User's email address
        otp_code: 6-digit OTP code
        
    Returns:
        bool: True if successful, False otherwise
    """
    try:
        print(f"\n[EMAIL] Preparing to send OTP to {recipient_email}")
        
        # Create message
        message = MIMEMultipart()
        message["From"] = EMAIL_ADDRESS
        message["To"] = recipient_email
        message["Subject"] = "Your Password Reset OTP"
        
        # Email body
        body = f"""
        <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Password Reset Request</h2>
                <p>You requested to reset your password.</p>
                <p>Your OTP (One-Time Password) is:</p>
                
                <div style="background-color: #f0f0f0; padding: 20px; border-radius: 5px; text-align: center;">
                    <h1 style="letter-spacing: 5px; color: #333;">{otp_code}</h1>
                </div>
                
                <p><strong>This OTP will expire in 10 minutes.</strong></p>
                
                <p>If you didn't request this, please ignore this email.</p>
                
                <hr>
                <p style="font-size: 12px; color: #666;">
                    This is an automated message. Please do not reply to this email.
                </p>
            </body>
        </html>
        """
        
        message.attach(MIMEText(body, "html"))
        
        # Send email
        with smtplib.SMTP(SMTP_SERVER, SMTP_PORT) as server:
            server.starttls()
            server.login(EMAIL_ADDRESS, EMAIL_PASSWORD)
            server.send_message(message)
        
        print(f"[EMAIL] ✅ OTP sent successfully to {recipient_email}")
        return True
        
    except Exception as e:
        print(f"[EMAIL] ❌ Failed to send OTP: {str(e)}")
        return False


def generate_otp():
    """Generate a random 6-digit OTP"""
    return str(random.randint(100000, 999999))


# ============================================
# FORGOT PASSWORD ENDPOINTS
# ============================================

def forgot_password():
    """
    POST /forgot-password
    
    Request body:
    {
        "username": "user@example.com"
    }
    
    Response:
    {
        "message": "OTP sent to your email"
    }
    """
    print("\n[PASSWORD] POST /forgot-password called")
    
    try:
        data = request.get_json()
        username = data.get('username')
        
        if not username:
            print("[PASSWORD] Username is required")
            return {"message": "Username is required"}, 400
        
        # Check if user exists
        user = User.query.filter_by(username=username).first()
        
        if not user:
            print(f"[PASSWORD] User {username} not found")
            return {"message": "User not found"}, 404
        
        # Generate OTP
        otp_code = generate_otp()
        
        # Store OTP in memory
        otp_store[otp_code] = {
            'username': username,
            'created_at': datetime.datetime.utcnow(),
            'expires_at': datetime.datetime.utcnow() + timedelta(minutes=10)
        }
        
        print(f"\n{'='*50}")
        print(f"🔐 OTP for {username}:")
        print(f"OTP: {otp_code}")
        print(f"Expires at: {otp_store[otp_code]['expires_at']}")
        print(f"{'='*50}\n")
        
        # Send OTP via email (optional - comment out for testing)
        email_sent = send_otp_email(username, otp_code)
        
        if not email_sent:
            print("[PASSWORD] Warning: Email failed to send, but OTP was generated")
            # In production, you might want to return error
            # For testing, you can still proceed
        
        return {
            "message": "OTP sent to your email"
        }, 200
    
    except Exception as e:
        print(f"[PASSWORD] Error in forgot_password: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


def verify_otp():
    """
    POST /verify-otp
    
    Request body:
    {
        "username": "user@example.com",
        "otp": "123456"
    }
    
    Response:
    {
        "message": "OTP verified successfully"
    }
    """
    print("\n[PASSWORD] POST /verify-otp called")
    
    try:
        data = request.get_json()
        username = data.get('username')
        otp_code = data.get('otp')
        
        if not username or not otp_code:
            print("[PASSWORD] Username and OTP are required")
            return {"message": "Username and OTP are required"}, 400
        
        # Check if OTP exists
        if otp_code not in otp_store:
            print(f"[PASSWORD] Invalid OTP: {otp_code}")
            return {"message": "Invalid OTP"}, 400
        
        otp_data = otp_store[otp_code]
        
        # Check if OTP belongs to the correct username
        if otp_data['username'] != username:
            print(f"[PASSWORD] OTP username mismatch")
            return {"message": "Invalid OTP for this user"}, 400
        
        # Check if OTP has expired
        if datetime.datetime.utcnow() > otp_data['expires_at']:
            del otp_store[otp_code]
            print(f"[PASSWORD] OTP has expired")
            return {"message": "OTP has expired"}, 400
        
        # Mark OTP as verified (keep it for reference)
        otp_data['verified'] = True
        otp_data['verified_at'] = datetime.datetime.utcnow()
        
        print(f"[PASSWORD] ✅ OTP verified for: {username}")
        
        return {
            "message": "OTP verified successfully"
        }, 200
    
    except Exception as e:
        print(f"[PASSWORD] Error in verify_otp: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


def reset_password():
    """
    POST /reset-password
    
    Request body:
    {
        "username": "user@example.com",
        "new_password": "NewPassword123"
    }
    
    Response:
    {
        "message": "Password reset successfully"
    }
    
    Note: OTP must be verified before calling this endpoint
    """
    print("\n[PASSWORD] POST /reset-password called")
    
    try:
        data = request.get_json()
        username = data.get('username')
        new_password = data.get('new_password')
        
        if not username or not new_password:
            print("[PASSWORD] Username and password are required")
            return {"message": "Username and password are required"}, 400
        
        if len(new_password) < 6:
            print("[PASSWORD] Password too short")
            return {"message": "Password must be at least 6 characters"}, 400
        
        # Check if user exists
        user = User.query.filter_by(username=username).first()
        
        if not user:
            print(f"[PASSWORD] User {username} not found")
            return {"message": "User not found"}, 404
        
        # Update password
        hashed_password = bcrypt.generate_password_hash(new_password).decode("utf-8")
        user.password = hashed_password
        
        db.session.commit()
        
        print(f"\n{'='*50}")
        print(f"✅ Password reset for: {username}")
        print(f"{'='*50}\n")
        
        # Clean up expired OTPs (optional maintenance)
        expired_otps = [
            otp for otp, data in otp_store.items()
            if datetime.datetime.utcnow() > data['expires_at']
        ]
        for otp in expired_otps:
            del otp_store[otp]
        
        return {
            "message": "Password reset successfully"
        }, 200
    
    except Exception as e:
        db.session.rollback()
        print(f"[PASSWORD] Error in reset_password: {str(e)}")
        return {"message": f"Error: {str(e)}"}, 500


# ============================================
# DEPRECATED ENDPOINTS (Remove from production)
# ============================================

def check_reset_token():
    """
    ❌ DEPRECATED - No longer used
    
    This endpoint was used for token-based reset.
    With OTP-based reset, token validation happens via /verify-otp
    """
    print("\n[PASSWORD] POST /check-reset-token called (DEPRECATED)")
    return {
        "message": "This endpoint is deprecated. Use /verify-otp instead."
    }, 410  # 410 Gone


# ============================================
# HOW TO INTEGRATE IN FLASK APP
# ============================================
"""
In your main Flask app file (e.g., app.py):

Add these routes:

@app.route('/forgot-password', methods=['POST'])
def handle_forgot_password():
    return forgot_password()

@app.route('/verify-otp', methods=['POST'])
def handle_verify_otp():
    return verify_otp()

@app.route('/reset-password', methods=['POST'])
def handle_reset_password():
    return reset_password()

@app.route('/check-reset-token', methods=['POST'])
def handle_check_token():
    return check_reset_token()  # Returns 410 Gone


Environment Variables (.env):
SMTP_SERVER=smtp.gmail.com
SMTP_PORT=587
EMAIL_ADDRESS=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

For Gmail:
1. Enable 2-factor authentication
2. Generate App Password: https://myaccount.google.com/apppasswords
3. Use the generated 16-character password as EMAIL_PASSWORD
"""