from database.db import db
import datetime
import json


class Form(db.Model):
    """
    Dynamic Form Model

    Stores:
    - Basic form information
    - Theme configuration
    - Layout configuration
    - Branding configuration

    Future UI should be completely driven from these configs.
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

    is_active = db.Column(
        db.Boolean,
        default=True
    )

    # ==========================
    # Dynamic UI Configuration
    # ==========================

    theme_json = db.Column(
        db.Text,
        nullable=True,
        comment="Theme configuration JSON"
    )

    layout_json = db.Column(
        db.Text,
        nullable=True,
        comment="Layout configuration JSON"
    )

    branding_json = db.Column(
        db.Text,
        nullable=True,
        comment="Branding configuration JSON"
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

    fields = db.relationship(
        "Field",
        backref="form",
        lazy=True,
        cascade="all, delete-orphan"
    )

    # =====================================================
    # Theme Helpers
    # =====================================================

    def get_theme(self):
        if self.theme_json:
            try:
                return json.loads(self.theme_json)
            except Exception:
                pass

        return {
            "primaryColor": "#6200EE",
            "accentColor": "#03DAC5",
            "backgroundColor": "#FFFFFF",
            "textColor": "#000000",
            "buttonColor": "#6200EE",
            "buttonTextColor": "#FFFFFF",
            "cornerRadius": 8
        }

    def set_theme(self, theme):
        self.theme_json = json.dumps(theme)

    # =====================================================
    # Layout Helpers
    # =====================================================

    def get_layout(self):
        if self.layout_json:
            try:
                return json.loads(self.layout_json)
            except Exception:
                pass

        return {
            "columns": 1,
            "showProgress": False,
            "submitButtonPosition": "bottom",
            "submitButtonText": "Submit"
        }

    def set_layout(self, layout):
        self.layout_json = json.dumps(layout)

    # =====================================================
    # Branding Helpers
    # =====================================================

    def get_branding(self):
        if self.branding_json:
            try:
                return json.loads(self.branding_json)
            except Exception:
                pass

        return {
            "organizationName": "",
            "logoUrl": "",
            "headerImage": ""
        }

    def set_branding(self, branding):
        self.branding_json = json.dumps(branding)

    # =====================================================
    # API Responses
    # =====================================================

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "version": self.version,
            "created_at": self.created_at.isoformat(),
            "field_count": len(self.fields)
        }

    def to_dict_with_fields(self):
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "version": self.version,
            "created_at": self.created_at.isoformat(),

            "theme": self.get_theme(),
            "layout": self.get_layout(),
            "branding": self.get_branding(),

            "fields": [
                field.to_dict()
                for field in sorted(
                    self.fields,
                    key=lambda f: f.field_order
                )
            ]
        }

    def __repr__(self):
        return f"<Form {self.id}: {self.name}>"