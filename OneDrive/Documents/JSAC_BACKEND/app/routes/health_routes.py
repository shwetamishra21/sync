from flask import Blueprint, jsonify

health_bp = Blueprint("health", __name__)

@health_bp.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "success",
        "message": "JSAC backend running successfully",
        "version": "1.0.0"
    })