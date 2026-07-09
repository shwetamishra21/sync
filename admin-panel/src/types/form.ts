export interface FormSummary {
  id: string;
  name: string;
  description: string;
  version: string;
  created_at: string;
  field_count: number;
}

export interface FormsResponse {
  status: string;
  forms: FormSummary[];
}