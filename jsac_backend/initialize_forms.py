"""
Script to initialize Jharkhand Resident Details Form
This creates a complete form with 8 fields dynamically in the database

Fields:
1. Full Name (text) - Required
2. Email (email) - Required
3. State District (dropdown) - Required
4. Address (textarea)
5. Age (number)
6. Date of Birth (date)
7. Profile Photo (media)
8. Current Location (gps)

Run this script: python jsac_backend/initialize_forms.py
"""

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app import app, db
from models.form_model import Form
from models.field_model import Field
import json

def initialize_jharkhand_form():
    """
    Initialize Jharkhand resident details form
    This is completely database-driven and can be modified through admin endpoints
    """
    
    print("\n" + "="*70)
    print("Initializing Jharkhand Resident Details Form")
    print("="*70 + "\n")
    
    with app.app_context():
        # Delete existing form if it exists (for fresh initialization)
        existing = Form.query.filter_by(id="form_jk_resident").first()
        if existing:
            print("🗑️  Removing existing form...")
            db.session.delete(existing)
            db.session.commit()
        
        # Create the form
        jharkhand_form = Form(
            id="form_jk_resident",
            name="Jharkhand Resident Registration",
            description="Complete resident registration form for Jharkhand state government services",
            version="1.0"
        )
        
        db.session.add(jharkhand_form)
        db.session.commit()
        
        print("✅ Form created: Jharkhand Resident Registration\n")
        
        # Define fields for the form
        fields_config = [
            {
                "field_id": "jk_full_name",
                "name": "Full Name",
                "type": "text",
                "required": True,
                "placeholder": "Enter your full name as per Aadhar",
                "field_order": 1,
                "help_text": "Your legal name as registered in government records"
            },
            {
                "field_id": "jk_email",
                "name": "Email Address",
                "type": "email",
                "required": True,
                "placeholder": "your.email@example.com",
                "field_order": 2,
                "help_text": "Valid email for communication"
            },
            {
                "field_id": "jk_phone",
                "name": "Phone Number",
                "type": "text",
                "required": True,
                "placeholder": "10-digit mobile number",
                "field_order": 3,
                "help_text": "WhatsApp enabled number preferred"
            },
            {
                "field_id": "jk_district",
                "name": "District",
                "type": "dropdown",
                "required": True,
                "field_order": 4,
                "help_text": "Select your district in Jharkhand",
                "options": [
                    "Ranchi",
                    "Dhanbad",
                    "Giridih",
                    "East Singhbhum",
                    "West Singhbhum",
                    "Purbi Singhbhum",
                    "Dumka",
                    "Godda",
                    "Deoghar",
                    "Jamtara",
                    "Latehar",
                    "Lohardaga",
                    "Hazaribag",
                    "Koderma",
                    "Chatra",
                    "Khunti",
                    "Pakur"
                ]
            },
            {
                "field_id": "jk_address",
                "name": "Residential Address",
                "type": "textarea",
                "required": True,
                "placeholder": "Enter your complete residential address (House no., Street, Village, Block, District, PIN)",
                "field_order": 5,
                "help_text": "Provide complete address for verification"
            },
            {
                "field_id": "jk_age",
                "name": "Age",
                "type": "number",
                "required": False,
                "placeholder": "Enter your age",
                "field_order": 6,
                "help_text": "Age in years"
            },
            {
                "field_id": "jk_dob",
                "name": "Date of Birth",
                "type": "date",
                "required": False,
                "placeholder": "YYYY-MM-DD",
                "field_order": 7,
                "help_text": "Date of birth in YYYY-MM-DD format"
            },
            {
                "field_id": "jk_photo",
                "name": "Profile Photo",
                "type": "media",
                "required": False,
                "field_order": 8,
                "help_text": "Upload a clear passport-size photo (JPG/PNG, max 5MB)"
            },
            {
                "field_id": "jk_location",
                "name": "Current Location (GPS)",
                "type": "gps",
                "required": False,
                "field_order": 9,
                "help_text": "Current GPS coordinates (auto-captured)"
            }
        ]
        
        # Add fields to the form
        for i, field_config in enumerate(fields_config, 1):
            field = Field(
                form_id="form_jk_resident",
                field_id=field_config["field_id"],
                name=field_config["name"],
                type=field_config["type"],
                required=field_config.get("required", False),
                placeholder=field_config.get("placeholder"),
                field_order=field_config.get("field_order", i),
                help_text=field_config.get("help_text")
            )
            
            # Set options for dropdown
            if field_config["type"] == "dropdown" and "options" in field_config:
                field.set_options(field_config["options"])
            
            db.session.add(field)
            print(f"  {i}. ✅ Added field: {field_config['name']} ({field_config['type']})")
        
        db.session.commit()
        
        print("\n" + "="*70)
        print("Form initialized successfully!")
        print("="*70)
        
        # Print form details
        form = Form.query.filter_by(id="form_jk_resident").first()
        print(f"\n📋 Form Details:")
        print(f"   ID: {form.id}")
        print(f"   Name: {form.name}")
        print(f"   Description: {form.description}")
        print(f"   Total Fields: {len(form.fields)}")
        print(f"   Version: {form.version}")
        
        print(f"\n📱 Field Summary:")
        for field in sorted(form.fields, key=lambda f: f.field_order):
            required_badge = "🔴 Required" if field.required else "⚪ Optional"
            print(f"   {field.field_order}. {field.name} ({field.type}) - {required_badge}")
        
        print("\n" + "="*70)
        print("🔧 How to Modify Fields in Real-Time:")
        print("="*70)
        print("""
1. UPDATE FIELD:
   PUT http://localhost:5000/admin/forms/form_jk_resident/fields/[field_id]
   {
       "name": "New Name",
       "required": true,
       "field_order": 5
   }

2. ADD NEW FIELD:
   POST http://localhost:5000/admin/forms/form_jk_resident/fields
   {
       "field_id": "jk_aadhar",
       "name": "Aadhar Number",
       "type": "text",
       "required": true,
       "placeholder": "12-digit Aadhar number",
       "field_order": 10
   }

3. DELETE FIELD:
   DELETE http://localhost:5000/admin/forms/form_jk_resident/fields/[field_id]

4. GET FORM (shown to users):
   GET http://localhost:5000/forms/form_jk_resident

✅ Changes are reflected INSTANTLY on all connected apps (no restart needed!)
        """)


def initialize_sample_forms():
    """
    Also create the original sample forms for reference
    """
    print("\n" + "="*70)
    print("Creating Additional Sample Forms...")
    print("="*70 + "\n")
    
    with app.app_context():
        sample_forms = [
            {
                "id": "form_citizen_service",
                "name": "Citizen Service Request",
                "description": "Request government services",
                "fields": [
                    {
                        "field_id": "cs_full_name",
                        "name": "Full Name",
                        "type": "text",
                        "required": True,
                        "placeholder": "Enter your full name",
                        "field_order": 1
                    },
                    {
                        "field_id": "cs_email",
                        "name": "Email",
                        "type": "email",
                        "required": True,
                        "placeholder": "Enter your email",
                        "field_order": 2
                    },
                    {
                        "field_id": "cs_service_type",
                        "name": "Service Type",
                        "type": "dropdown",
                        "required": True,
                        "field_order": 3,
                        "options": ["Birth Certificate", "License", "Permit", "Other"]
                    },
                    {
                        "field_id": "cs_description",
                        "name": "Description",
                        "type": "textarea",
                        "required": False,
                        "placeholder": "Describe your request",
                        "field_order": 4
                    }
                ]
            },
            {
                "id": "form_property_reg",
                "name": "Property Registration",
                "description": "Register property with government",
                "fields": [
                    {
                        "field_id": "pr_address",
                        "name": "Property Address",
                        "type": "text",
                        "required": True,
                        "placeholder": "Enter property address",
                        "field_order": 1
                    },
                    {
                        "field_id": "pr_type",
                        "name": "Property Type",
                        "type": "dropdown",
                        "required": True,
                        "field_order": 2,
                        "options": ["Residential", "Commercial", "Agricultural"]
                    },
                    {
                        "field_id": "pr_area",
                        "name": "Area (sq ft)",
                        "type": "number",
                        "required": True,
                        "placeholder": "Enter area",
                        "field_order": 3
                    }
                ]
            }
        ]
        
        for form_config in sample_forms:
            existing = Form.query.filter_by(id=form_config["id"]).first()
            if existing:
                continue  # Skip if already exists
            
            form = Form(
                id=form_config["id"],
                name=form_config["name"],
                description=form_config.get("description", "")
            )
            db.session.add(form)
            db.session.commit()
            
            for field_config in form_config.get("fields", []):
                field = Field(
                    form_id=form["id"],
                    field_id=field_config["field_id"],
                    name=field_config["name"],
                    type=field_config["type"],
                    required=field_config.get("required", False),
                    placeholder=field_config.get("placeholder"),
                    field_order=field_config.get("field_order")
                )
                
                if field_config["type"] == "dropdown":
                    field.set_options(field_config.get("options", []))
                
                db.session.add(field)
            
            db.session.commit()
            print(f"✅ Created form: {form_config['name']}")


if __name__ == "__main__":
    initialize_jharkhand_form()
    initialize_sample_forms()