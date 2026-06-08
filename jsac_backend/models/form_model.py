from database.db import db
import datetime

class Form(db.Model):
    """
    Model for storing form templates in database
    This replaces hardcoded forms_db list
    
    Features:
    - Store form metadata (name, description, version)
    - Store creation and update timestamps
    - Support multiple form versions
    """
    __tablename__ = "forms"

    id = db.Column(
        db.String(50),
        primary_key=True,
        nullable=False,
        unique=True
    )

    name = db.Column(
        db.String(255),
        nullable=False
    )

    description = db.Column(
        db.Text,
        nullable=True
    )

    version = db.Column(
        db.String(10),
        default="1.0"
    )

    created_at = db.Column(
        db.DateTime,
        default=datetime.datetime.utcnow
    )

    updated_at = db.Column(
        db.DateTime,
        default=datetime.datetime.utcnow,
        onupdate=datetime.datetime.utcnow
    )

    is_active = db.Column(
        db.Boolean,
        default=True
    )

    # Relationship to fields
    fields = db.relationship(
        'Field',
        backref='form',
        lazy=True,
        cascade='all, delete-orphan'
    )

    def to_dict(self):
        """Convert form to dictionary for API response"""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "version": self.version,
            "created_at": self.created_at.isoformat(),
            "field_count": len(self.fields)
        }

    def to_dict_with_fields(self):
        """Convert form with all fields to dictionary"""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "version": self.version,
            "created_at": self.created_at.isoformat(),
            "fields": [field.to_dict() for field in sorted(self.fields, key=lambda f: f.field_order)]
        }

    def __repr__(self):
        return f"<Form {self.id}: {self.name}>"