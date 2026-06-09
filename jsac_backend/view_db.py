from app import app
from models.submission_model import FormSubmission
from database.db import db
from models.user_model import User
from models.form_model import Form
from models.field_model import Field


def print_section(title):
    print("\n" + "=" * 80)
    print(title.center(80))
    print("=" * 80)


with app.app_context():

    # DATABASE INFO
    print_section("DATABASE SUMMARY")

    print(f"Users  : {User.query.count()}")
    print(f"Forms  : {Form.query.count()}")
    print(f"Fields : {Field.query.count()}")

    # USERS
    print_section("USERS")

    users = User.query.all()

    if not users:
        print("No users found")
    else:
        print(f"{'ID':<5} {'USERNAME':<25}")
        print("-" * 40)

        for user in users:
            print(f"{user.id:<5} {user.username:<25}")

    # FORMS
    print_section("FORMS")

    forms = Form.query.all()

    if not forms:
        print("No forms found")
    else:
        print(
            f"{'FORM ID':<25} {'NAME':<35} {'VERSION':<10} {'FIELDS':<10}"
        )
        print("-" * 90)

        for form in forms:
            print(
                f"{form.id:<25} "
                f"{form.name[:34]:<35} "
                f"{form.version:<10} "
                f"{len(form.fields):<10}"
            )

    # FORM DETAILS
    print_section("FORM DETAILS")

    for form in forms:

        print(f"\n📋 FORM: {form.name}")
        print(f"ID          : {form.id}")
        print(f"Description : {form.description}")
        print(f"Version     : {form.version}")

        print("\nFIELDS")
        print("-" * 100)

        print(
            f"{'ORDER':<8}"
            f"{'FIELD ID':<25}"
            f"{'NAME':<25}"
            f"{'TYPE':<15}"
            f"{'REQUIRED':<10}"
        )

        print("-" * 100)

        sorted_fields = sorted(
            form.fields,
            key=lambda x: x.field_order
        )

        for field in sorted_fields:
            print(
                f"{field.field_order:<8}"
                f"{field.field_id:<25}"
                f"{field.name:<25}"
                f"{field.type:<15}"
                f"{str(field.required):<10}"
            )

        print()

        print("\n" + "=" * 80)

    # FORM SUBMISSIONS
    print_section("FORM SUBMISSIONS")

    submissions = FormSubmission.query.all()

    if not submissions:
        print("No submissions found")
    else:
        print(
            f"{'ID':<8}"
            f"{'FORM ID':<25}"
            f"{'STATUS':<15}"
            f"{'CREATED AT'}"
        )
        print("-" * 100)

        for submission in submissions:
            print(
                f"{submission.id:<8}"
                f"{submission.form_id:<25}"
                f"{submission.sync_status:<15}"
                f"{submission.created_at}"
            )

        print("\nSUBMISSION DETAILS")
        print("-" * 100)

        for submission in submissions:
            print(f"\nSubmission ID : {submission.id}")
            print(f"Form ID       : {submission.form_id}")
            print(f"Status        : {submission.sync_status}")
            print(f"Created At    : {submission.created_at}")
            print(f"GPS Latitude  : {submission.gps_latitude}")
            print(f"GPS Longitude : {submission.gps_longitude}")

            print("\nForm Data:")
            print(submission.get_form_data())

            print("-" * 100)

    print("\n" + "=" * 80)
    print("DATABASE INSPECTION COMPLETE")
    print("=" * 80)