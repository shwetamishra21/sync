from database.db import db
import datetime
from sqlalchemy.dialects.postgresql import JSONB


class Form(db.Model):
    """
    Model for storing form templates in database.
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

    # ----------------------------
    # Dynamic UI Configuration
    # ----------------------------

    theme_json = db.Column(
        JSONB,
        nullable=False,
        default=dict
    )

    layout_json = db.Column(
        JSONB,
        nullable=False,
        default=dict
    )

    branding_json = db.Column(
        JSONB,
        nullable=False,
        default=dict
    )

    # ----------------------------
    # Relationship
    # ----------------------------

    fields = db.relationship(
        "Field",
        backref="form",
        lazy=True,
        cascade="all, delete-orphan"
    )

    def to_dict(self):
        """Used for /forms"""

        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "version": self.version,
            "created_at": self.created_at.isoformat(),
            "field_count": len(self.fields)
        }

    def to_dict_with_fields(self):
        """Used for /forms/<form_id>"""

        result = {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "version": self.version,
            "created_at": self.created_at.isoformat(),

            "theme": self.theme_json or {},
            "layout": self.layout_json or {},
            "branding": self.branding_json or {},

            "fields": [
                field.to_dict()
                for field in sorted(
                    self.fields,
                    key=lambda f: f.field_order
                )
            ]
        }

        print("\n================ FORM RESPONSE ================")
        print(result)
        print("==============================================\n")

        return result

    def __repr__(self):
        return f"<Form {self.id}: {self.name}>"