"""
API Testing Script for Dynamic Forms - UPDATED

This script helps test all endpoints including form submission and media upload.

Usage: 
  - Run all tests: python test_api.py
  - Interactive mode: python test_api.py --interactive
"""

import requests
import json
from datetime import datetime
import os

BASE_URL = "http://localhost:5000"
FORM_ID = "form_jk_resident"

# Test user credentials
TEST_USERNAME = "test@example.com"
TEST_PASSWORD = "password123"
TEST_TOKEN = None

# Color codes for terminal output
GREEN = '\033[92m'
RED = '\033[91m'
BLUE = '\033[94m'
YELLOW = '\033[93m'
RESET = '\033[0m'

def print_header(title):
    print(f"\n{BLUE}{'='*70}")
    print(f"{title}")
    print(f"{'='*70}{RESET}\n")

def print_success(msg):
    print(f"{GREEN}✅ {msg}{RESET}")

def print_error(msg):
    print(f"{RED}❌ {msg}{RESET}")

def print_info(msg):
    print(f"{YELLOW}ℹ️  {msg}{RESET}")

def test_health_check():
    """Test: GET /health - Health check"""
    print_header("TEST 0: Health Check")
    
    try:
        response = requests.get(f"{BASE_URL}/health")
        
        if response.status_code == 200:
            data = response.json()
            print_success(f"Backend is running!")
            print(f"Status: {data['status']}")
            print(f"Message: {data['message']}")
            print(f"Version: {data['version']}\n")
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            return False
    except Exception as e:
        print_error(f"Cannot connect to backend: {str(e)}")
        print_info("Make sure backend is running: python app.py")
        return False


def test_user_registration():
    """Test: POST /register - Create test user"""
    print_header("TEST 1: User Registration")
    
    try:
        payload = {
            "username": TEST_USERNAME,
            "password": TEST_PASSWORD
        }
        
        response = requests.post(f"{BASE_URL}/register", json=payload)
        
        if response.status_code == 201:
            print_success("User registered successfully")
            print(f"Response: {response.json()}\n")
            return True
        elif response.status_code == 400:
            print_info("User already exists (this is OK)")
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return False


def test_user_login():
    """Test: POST /login - Get JWT token"""
    print_header("TEST 2: User Login")
    
    global TEST_TOKEN
    
    try:
        payload = {
            "username": TEST_USERNAME,
            "password": TEST_PASSWORD
        }
        
        response = requests.post(f"{BASE_URL}/login", json=payload)
        
        if response.status_code == 200:
            data = response.json()
            TEST_TOKEN = data['token']
            print_success(f"Login successful!")
            print(f"Username: {data['username']}")
            print(f"Token (first 50 chars): {TEST_TOKEN[:50]}...\n")
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return False


def test_get_all_forms():
    """Test: GET /forms - Get all forms"""
    print_header("TEST 3: Get All Forms")
    
    try:
        response = requests.get(f"{BASE_URL}/forms")
        
        if response.status_code == 200:
            data = response.json()
            print_success(f"Got {data['count']} form(s)")
            
            for form in data['forms']:
                print(f"  - {form['name']} ({form['id']}) - {form['field_count']} fields")
            print()
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return False


def test_get_form_detail():
    """Test: GET /forms/{form_id} - Get form with fields"""
    print_header("TEST 4: Get Form Detail with Fields")
    
    try:
        response = requests.get(f"{BASE_URL}/forms/{FORM_ID}")
        
        if response.status_code == 200:
            data = response.json()
            form = data['form']
            print_success(f"Got form: {form['name']}")
            print(f"Fields: {len(form['fields'])} total\n")
            
            for i, field in enumerate(form['fields'], 1):
                req_badge = "🔴 Required" if field['required'] else "⚪ Optional"
                print(f"  {i}. {field['name']} ({field['type']}) - {req_badge}")
            print()
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return False


def test_submit_form():
    """Test: POST /forms/submit - Submit a form"""
    print_header("TEST 5: Submit Form (NEW ENDPOINT)")
    
    if not TEST_TOKEN:
        print_error("No token available. Login first.")
        return False
    
    try:
        payload = {
            "form_id": FORM_ID,
            "form_data": {
                "jk_full_name": "John Doe",
                "jk_email": "john@example.com",
                "jk_phone": "9876543210",
                "jk_district": "Ranchi",
                "jk_address": "123 Main St, Ranchi, Jharkhand 834001",
                "jk_age": "30",
                "jk_dob": "1995-05-15"
            },
            "submitted_at": int(datetime.now().timestamp() * 1000),
            "gps_location": {
                "lat": 25.5941,
                "lng": 85.1376
            }
        }
        
        headers = {
            "Authorization": f"Bearer {TEST_TOKEN}",
            "Content-Type": "application/json"
        }
        
        response = requests.post(
            f"{BASE_URL}/forms/submit",
            json=payload,
            headers=headers
        )
        
        if response.status_code == 201:
            data = response.json()
            print_success("Form submitted successfully!")
            print(f"Submission ID: {data['submission_id']}")
            print(f"Message: {data['message']}")
            print(f"Response: {json.dumps(data, indent=2)}\n")
            return data['submission_id']
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return None
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return None


def test_media_upload(submission_id):
    """Test: POST /media/upload - Upload media file"""
    print_header("TEST 6: Upload Media File (NEW ENDPOINT)")
    
    if not TEST_TOKEN:
        print_error("No token available. Login first.")
        return False
    
    try:
        # Create a test file
        test_file_path = "/tmp/test_photo.jpg"
        
        # Create a minimal valid JPEG file
        jpeg_header = bytes.fromhex("FFD8FFE000104A46494600010100000100010000")
        jpeg_footer = bytes.fromhex("FFD9")
        
        with open(test_file_path, 'wb') as f:
            f.write(jpeg_header)
            f.write(b"Test image content" * 100)
            f.write(jpeg_footer)
        
        print_info(f"Created test file: {test_file_path}")
        
        # Prepare multipart request
        with open(test_file_path, 'rb') as f:
            files = {
                'file': ('test_photo.jpg', f, 'image/jpeg')
            }
            
            data = {
                'submission_id': submission_id,
                'field_id': 'jk_photo'
            }
            
            headers = {
                "Authorization": f"Bearer {TEST_TOKEN}"
            }
            
            response = requests.post(
                f"{BASE_URL}/media/upload",
                files=files,
                data=data,
                headers=headers
            )
        
        if response.status_code == 201:
            result = response.json()
            print_success("File uploaded successfully!")
            print(f"Server URL: {result['server_url']}")
            print(f"File size: {result['file_size']} bytes")
            print(f"Response: {json.dumps(result, indent=2)}\n")
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return False
    
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return False
    finally:
        # Clean up test file
        if os.path.exists(test_file_path):
            os.remove(test_file_path)
            print_info(f"Cleaned up test file")


def test_admin_create_form():
    """Test: POST /admin/forms - Create new form (ADMIN)"""
    print_header("TEST 7: Create New Form (ADMIN)")
    
    if not TEST_TOKEN:
        print_error("No token available. Login first.")
        return False
    
    try:
        payload = {
            "id": "form_test_" + str(int(datetime.now().timestamp())),
            "name": "Test Form",
            "description": "A test form for validation",
            "version": "1.0"
        }
        
        headers = {
            "Authorization": f"Bearer {TEST_TOKEN}",
            "Content-Type": "application/json"
        }
        
        response = requests.post(
            f"{BASE_URL}/admin/forms",
            json=payload,
            headers=headers
        )
        
        if response.status_code == 201:
            data = response.json()
            print_success(f"Created form: {data['form']['id']}")
            print(f"Response: {json.dumps(data, indent=2)}\n")
            return data['form']['id']
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return None
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return None


def test_admin_add_field(form_id):
    """Test: POST /admin/forms/{form_id}/fields - Add field (ADMIN)"""
    print_header("TEST 8: Add Field to Form (ADMIN)")
    
    if not TEST_TOKEN:
        print_error("No token available. Login first.")
        return False
    
    try:
        payload = {
            "field_id": "test_field_" + str(int(datetime.now().timestamp())),
            "name": "Test Text Field",
            "type": "text",
            "required": True,
            "placeholder": "Enter test data",
            "field_order": 1,
            "help_text": "This is a test field"
        }
        
        headers = {
            "Authorization": f"Bearer {TEST_TOKEN}",
            "Content-Type": "application/json"
        }
        
        response = requests.post(
            f"{BASE_URL}/admin/forms/{form_id}/fields",
            json=payload,
            headers=headers
        )
        
        if response.status_code == 201:
            data = response.json()
            print_success(f"Added field: {data['field']['name']}")
            print(f"Response: {json.dumps(data, indent=2)}\n")
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return False


def test_admin_add_dropdown_field(form_id):
    """Test: POST /admin/forms/{form_id}/fields - Add dropdown (ADMIN)"""
    print_header("TEST 9: Add Dropdown Field (ADMIN)")
    
    if not TEST_TOKEN:
        print_error("No token available. Login first.")
        return False
    
    try:
        payload = {
            "field_id": "test_dropdown_" + str(int(datetime.now().timestamp())),
            "name": "Test Dropdown",
            "type": "dropdown",
            "required": False,
            "field_order": 2,
            "help_text": "Select an option",
            "options": ["Option 1", "Option 2", "Option 3"]
        }
        
        headers = {
            "Authorization": f"Bearer {TEST_TOKEN}",
            "Content-Type": "application/json"
        }
        
        response = requests.post(
            f"{BASE_URL}/admin/forms/{form_id}/fields",
            json=payload,
            headers=headers
        )
        
        if response.status_code == 201:
            data = response.json()
            print_success(f"Added dropdown field: {data['field']['name']}")
            print(f"Options: {data['field'].get('options', [])}\n")
            return True
        else:
            print_error(f"Failed with status {response.status_code}")
            print(response.text)
            return False
    except Exception as e:
        print_error(f"Exception: {str(e)}")
        return False


def run_all_tests():
    """Run all tests"""
    print(f"\n{BLUE}")
    print("╔" + "="*68 + "╗")
    print("║" + " "*12 + "DYNAMIC FORMS API TEST SUITE - COMPLETE" + " "*16 + "║")
    print("║" + " "*68 + "║")
    print("║" + f" Backend URL: {BASE_URL}".ljust(68) + "║")
    print("║" + f" Form ID: {FORM_ID}".ljust(68) + "║")
    print("║" + " "*68 + "║")
    print("║" + " Tests: Health, Auth, Forms, Submission, Upload, Admin".ljust(68) + "║")
    print("╚" + "="*68 + "╝")
    print(f"{RESET}")
    
    results = []
    
    # Test 0: Health check
    if not test_health_check():
        print_error("Backend is not running. Start it with: python app.py")
        return
    results.append(("Health Check", True))
    
    # Test 1: Register user
    results.append(("User Registration", test_user_registration()))
    
    # Test 2: Login user
    results.append(("User Login", test_user_login()))
    
    if not TEST_TOKEN:
        print_error("Could not get authentication token. Stopping tests.")
        return
    
    # Test 3: Get all forms
    results.append(("Get All Forms", test_get_all_forms()))
    
    # Test 4: Get form detail
    results.append(("Get Form Detail", test_get_form_detail()))
    
    # Test 5: Submit form
    submission_id = test_submit_form()
    results.append(("Submit Form", submission_id is not None))
    
    # Test 6: Upload media
    if submission_id:
        results.append(("Upload Media", test_media_upload(submission_id)))
    
    # Test 7: Create form (Admin)
    new_form_id = test_admin_create_form()
    results.append(("Create Form (Admin)", new_form_id is not None))
    
    # Test 8: Add text field
    if new_form_id:
        results.append(("Add Text Field (Admin)", test_admin_add_field(new_form_id)))
        
        # Test 9: Add dropdown field
        results.append(("Add Dropdown Field (Admin)", test_admin_add_dropdown_field(new_form_id)))
    
    # Print summary
    print_header("TEST SUMMARY")
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = f"{GREEN}PASSED{RESET}" if result else f"{RED}FAILED{RESET}"
        print(f"  {test_name}: {status}")
    
    print(f"\n  Total: {passed}/{total} tests passed")
    
    if passed == total:
        print_success("All tests passed! ✨")
    else:
        print_error(f"{total - passed} test(s) failed")
    
    print()


if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == "--interactive":
        print_header("INTERACTIVE API TESTER")
        print("Not implemented yet. Run without arguments for automated tests.")
    else:
        run_all_tests()