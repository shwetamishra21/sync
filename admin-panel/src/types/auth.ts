export interface LoginRequest {
  username: string;
  password: string;
}

// Matches the actual response shape of POST /admin/login: { status, token, email }
export interface LoginResponse {
  status: string;
  token: string;
  email: string;
}