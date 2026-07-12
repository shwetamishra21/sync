// src/types/submission.ts
// Matches models/submission_model.py -> FormSubmission.to_dict()

export interface Submission {
  id: number;
  form_id: string;
  form_data: Record<string, unknown>;
  sync_status: "PENDING" | "SYNCING" | "SYNCED" | "FAILED" | string;
  created_at: string;
  synced_at: string | null;
  retry_count: number;
  gps_latitude: number | null;
  gps_longitude: number | null;
}

export interface SubmissionsResponse {
  status: string;
  submissions: Submission[];
  count: number;
  total: number;
}

export interface SubmissionResponse {
  status: string;
  submission: Submission;
}

export interface GetSubmissionsParams {
  form_id?: string;
  status?: string;
  limit?: number;
  offset?: number;
}
