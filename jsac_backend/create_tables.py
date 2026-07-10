from app import app
from models.admin_model import Admin
from database.db import db

with app.app_context():
    print("\n" + "=" * 50)
    print("Recreating database tables...")
    print("=" * 50)

    # Drop all existing tables
    db.drop_all()
    print("✓ Existing tables dropped")

    # Create tables from current models
    db.create_all()
    print("✓ New tables created")

print("=" * 50)
print("Database recreated successfully!")
print("=" * 50)