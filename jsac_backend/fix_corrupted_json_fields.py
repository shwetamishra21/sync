"""
One-time repair script for forms whose theme_json / layout_json /
branding_json columns were corrupted by the double-JSON-encoding bug in
PUT /admin/forms/<form_id> (see app.py, update_form()).

Before the fix, saving a theme/layout/branding from the Admin Panel called
json.dumps() on the dict before assigning it to a JSONB column, so the
column ended up storing a JSON-encoded STRING instead of a JSON OBJECT,
e.g.:

    '"{\\"primaryColor\\": \\"#1976d2\\", ...}"'   (WRONG - string)

instead of:

    {"primaryColor": "#1976d2", ...}                (RIGHT - object)

That corrupted string is what caused:
  - the Admin Panel's Theme Editor to misbehave after saving/reloading
  - the Android app crash:
      java.lang.IllegalStateException: Expected BEGIN_OBJECT but was
      STRING at line 53 column 16 path $.form.layout

Run this ONCE after deploying the app.py fix to repair any forms that were
already saved with the bug:

    cd jsac_backend
    python fix_corrupted_json_fields.py
"""

from app import app, _coerce_json_field
from database.db import db
from models.form_model import Form


def _repair(value):
    """
    Returns (needs_write, fixed_value).

    Reuses the same _coerce_json_field() logic as the PUT /admin/forms
    endpoint so this script repairs BOTH corruption patterns seen in the
    wild:
      1. theme_json / layout_json / branding_json stored as a JSON-encoded
         STRING (the original double-encoding bug), and
      2. a "char-indexed" object such as
         {"0": "{", "1": "\"", ..., "spacing": 20} caused by the Admin
         Panel spreading a corrupted string value from bug #1 into React
         state before this fix was deployed.
    Leaves already-correct dict/list values untouched.
    """
    fixed = _coerce_json_field(value)
    changed = fixed != value
    return changed, fixed


def main():
    with app.app_context():
        forms = Form.query.all()
        fixed_count = 0

        for form in forms:
            changed = False

            theme_changed, theme_value = _repair(form.theme_json)
            if theme_changed:
                form.theme_json = theme_value
                changed = True

            layout_changed, layout_value = _repair(form.layout_json)
            if layout_changed:
                form.layout_json = layout_value
                changed = True

            branding_changed, branding_value = _repair(form.branding_json)
            if branding_changed:
                form.branding_json = branding_value
                changed = True

            if changed:
                fixed_count += 1
                print(f"[FIX] Repaired form '{form.id}' "
                      f"(theme={theme_changed}, layout={layout_changed}, "
                      f"branding={branding_changed})")

        if fixed_count:
            db.session.commit()
            print(f"\n✓ Repaired {fixed_count} form(s).")
        else:
            print("\n✓ No corrupted forms found. Nothing to do.")


if __name__ == "__main__":
    main()
