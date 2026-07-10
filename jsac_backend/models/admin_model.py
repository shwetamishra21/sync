from database.db import db
import datetime


class Admin(db.Model):
    __tablename__ = "admins"

    id = db.Column(
        db.Integer,
        primary_key=True
    )

    email = db.Column(
        db.String(255),
        unique=True,
        nullable=False
    )

    password = db.Column(
        db.String(255),
        nullable=False
    )

    created_at = db.Column(
        db.DateTime,
        default=datetime.datetime.utcnow
    )

    def __repr__(self):
        return f"<Admin {self.email}>"