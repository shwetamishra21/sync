import axios from "./axios";

import type {
  LoginRequest,
  LoginResponse,
} from "../types/auth";

export async function login(
  credentials: LoginRequest
): Promise<LoginResponse> {
  const response = await axios.post<LoginResponse>(
    "/admin/login",
    {
      email: credentials.username,
      password: credentials.password,
    }
  );

  return response.data;
}