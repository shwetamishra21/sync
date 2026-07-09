import axios from "./axios";
import type { FormSummary } from "../types/form";

export interface FormsResponse {
  status: string;
  forms: FormSummary[];
}

export async function getForms(): Promise<FormsResponse> {
  const response = await axios.get<FormsResponse>("/forms");
  return response.data;
}