from database.db import db
import datetime
import json

class FormSubmission(db.Model):
    """
    Model for storing user form submissions
    Tracks submitted data, sync status, and submission metadata
    
    FIX #5: Added idempotency_key for duplicate prevention
    - Server checks idempotency_key before inserting
    - If key exists, returns existing submission instead of creating duplicate
    - Prevents duplicate submissions when worker retries
    
    Status:
    - PENDING: Not yet synced to server
    - SYNCING: Currently uploading
    - SYNCED: Successfully uploaded
    - FAILED: Failed to upload (will retry)
    """
    __tablename__ = "form_submissions"

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

    # JSON string containing all form field values
    form_data = db.Column(
        db.Text,
        nullable=False,
        comment="JSON object with all field values"
    )

    # Submission status for sync tracking
    sync_status = db.Column(
        db.String(20),
        default="PENDING",
        comment="PENDING, SYNCING, SYNCED, FAILED"
    )

    # ✅ FIX #5: Idempotency key for duplicate prevention
    idempotency_key = db.Column(
        db.String(255),
        unique=True,
        nullable=False,
        comment="UUID to detect duplicate submissions"
    )

    # Timestamps
    created_at = db.Column(
        db.DateTime,
        default=datetime.datetime.utcnow
    )

    updated_at = db.Column(
        db.DateTime,
        default=datetime.datetime.utcnow,
        onupdate=datetime.datetime.utcnow
    )

    synced_at = db.Column(
        db.DateTime,
        nullable=True,
        comment="When the submission was successfully synced"
    )

    # Error tracking
    error_message = db.Column(
        db.Text,
        nullable=True,
        comment="Error message if sync failed"
    )

    retry_count = db.Column(
        db.Integer,
        default=0,
        comment="Number of sync attempts"
    )

    last_sync_attempt = db.Column(
        db.DateTime,
        nullable=True,
        comment="When was the last sync attempt"
    )

    # GPS location (optional)
    gps_latitude = db.Column(
        db.Float,
        nullable=True
    )

    gps_longitude = db.Column(
        db.Float,
        nullable=True
    )

    # Relationship to form
    form = db.relationship(
        'Form',
        backref='submissions'
    )

    def get_form_data(self):
        """Parse form data JSON"""
        try:
            return json.loads(self.form_data)
        except:
            return {}

    def set_form_data(self, data):
        """Store form data as JSON"""
        self.form_data = json.dumps(data)

    def to_dict(self):
        """Convert to dictionary for API response"""
        return {
            "id": self.id,
            "form_id": self.form_id,
            "form_data": self.get_form_data(),
            "sync_status": self.sync_status,
            "created_at": self.created_at.isoformat(),
            "synced_at": self.synced_at.isoformat() if self.synced_at else None,
            "retry_count": self.retry_count,
            "gps_latitude": self.gps_latitude,
            "gps_longitude": self.gps_longitude
        }

    def __repr__(self):
        return f"<FormSubmission {self.id}: {self.form_id}>"