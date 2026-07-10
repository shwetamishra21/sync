from app import app
from database.db import db
from flask_bcrypt import Bcrypt

from models.admin_model import Admin

bcrypt = Bcrypt()

with app.app_context():

    existing = Admin.query.filter_by(
        email="admin@jsac.local"
    ).first()

    if existing:
        print("Admin already exists.")
        exit()

    hashed = bcrypt.generate_password_hash(
        "admin123"
    ).decode("utf-8")

    admin = Admin(
        email="admin@jsac.local",
        password=hashed
    )

    db.session.add(admin)
    db.session.commit()

    print("Admin created successfully.")