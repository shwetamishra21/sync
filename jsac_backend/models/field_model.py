from database.db import db
import datetime
import json

class Field(db.Model):
    """
    Model for storing form field definitions in database
    Each field belongs to a form and defines input type, validation, etc.
    
    Field Types Supported:
    - text: Simple text input
    - email: Email validation
    - number: Numeric input
    - date: Date picker
    - textarea: Multi-line text
    - dropdown: Select from options
    - media: Photo/file upload
    - gps: GPS location capture
    
    Features:
    - Dynamic field order
    - Validation rules
    - Optional placeholder and help text
    - Support for dropdown options
    - Conditional visibility (visible_if)
    - Conditional enabling (enabled_if)
    - Default values (default_value)
    """
    __tablename__ = "form_fields"

    id = db.Column(
        db.Integer,
        primary_key=True,
        autoincrement=True
    )

    form_id = db.Column(
        db.String(50),
        db.ForeignKey('forms.id', ondelete='CASCADE'),
        nullable=False
    )

    field_id = db.Column(
        db.String(50),
        nullable=False,
        unique=False  # Can have same field_id in different forms
    )

    name = db.Column(
        db.String(255),
        nullable=False
    )

    type = db.Column(
        db.String(50),
        nullable=False,
        comment="text, email, number, date, textarea, dropdown, media, gps"
    )

    required = db.Column(
        db.Boolean,
        default=False
    )

    placeholder = db.Column(
        db.String(255),
        nullable=True
    )

    field_order = db.Column(
        db.Integer,
        default=0,
        comment="Order in which field appears on form"
    )

    # For dropdown/select fields: store options as JSON array
    options_json = db.Column(
        db.Text,
        nullable=True,
        comment="JSON array of dropdown options"
    )

    # Additional validation/config as JSON
    validation_json = db.Column(
        db.Text,
        nullable=True,
        comment="JSON object with validation rules (min, max, pattern, etc.)"
    )
    visible_if_json = db.Column(
        db.Text,
        nullable=True,
        comment="JSON object defining field visibility rules"
    )

    # Dynamic enable/disable rules
    enabled_if_json = db.Column(
        db.Text,
        nullable=True,
        comment="JSON object defining field enabled/disabled rules"
    )

    # ✅ NEW: Backend supplied default value
    default_value = db.Column(
        db.Text,
        nullable=True,
        comment="Backend supplied default value"
    )

    help_text = db.Column(
        db.String(500),
        nullable=True,
        comment="Help text shown below field"
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

    def get_options(self):
        """Parse options JSON and return as list"""
        if self.options_json:
            try:
                return json.loads(self.options_json)
            except:
                return []
        return []

    def set_options(self, options):
        """Store options as JSON"""
        if options:
            self.options_json = json.dumps(options)
        else:
            self.options_json = None

    def get_validation(self):
        """Parse validation JSON"""
        if self.validation_json:
            try:
                return json.loads(self.validation_json)
            except:
                return {}
        return {}

    def set_validation(self, validation):
        """Store validation rules as JSON"""
        if validation:
            self.validation_json = json.dumps(validation)
        else:
            self.validation_json = None
    
    def get_visible_if(self):
        """Parse visibility rules JSON"""
        if self.visible_if_json:
            try:
                return json.loads(self.visible_if_json)
            except:
                return {}
        return {}

    def set_visible_if(self, visible_if):
        """Store visibility rules"""
        if visible_if:
            self.visible_if_json = json.dumps(visible_if)
        else:
            self.visible_if_json = None

    def get_enabled_if(self):
        """Parse enabled_if JSON"""
        if self.enabled_if_json:
            try:
                return json.loads(self.enabled_if_json)
            except:
                return {}
        return {}

    def set_enabled_if(self, enabled_if):
        """Store enabled_if rules"""
        if enabled_if:
            self.enabled_if_json = json.dumps(enabled_if)
        else:
            self.enabled_if_json = None

    def to_dict(self):
        """Convert field to dictionary for API response"""
        result = {
            "db_id": self.id,
            "id": self.field_id,
            "name": self.name,
            "type": self.type,
            "required": self.required,
            "placeholder": self.placeholder,
            "field_order": self.field_order,
            "help_text": self.help_text,
            "default_value": self.default_value,
            }

        # Include options if dropdown/select type
        if self.type in ["dropdown", "select"]:
            result["options"] = self.get_options()

        # Include help text if present
        if self.help_text:
            result["help_text"] = self.help_text
        
        validation = self.get_validation()
        visible_if = self.get_visible_if()
        enabled_if = self.get_enabled_if()
        
        if visible_if:
            result["visible_if"] = visible_if
        
        if enabled_if:
            result["enabled_if"] = enabled_if
        
        if validation:
            result["validation"] = validation

        # ✅ NEW: Include default_value if present
        if self.default_value is not None:
            result["default_value"] = self.default_value

        return result

    def __repr__(self):
        return f"<Field {self.form_id}/{self.field_id}: {self.name}>"