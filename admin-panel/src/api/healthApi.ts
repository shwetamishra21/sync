import api from "./axios";

export interface HealthResponse {
  status: string;
  message: string;
  version: string;
}

export async function getHealth() {
  const response = await api.get<HealthResponse>("/health");
  return response.data;
}