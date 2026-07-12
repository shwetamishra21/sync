import axios from "./axios";
import type {
  GetSubmissionsParams,
  Submission,
  SubmissionResponse,
  SubmissionsResponse,
} from "../types/submission";

/**
 * Get all submissions (ADMIN ONLY, requires auth token)
 * Supports optional filtering by form_id / status and pagination.
 */
export async function getSubmissions(
  params: GetSubmissionsParams = {}
): Promise<SubmissionsResponse> {
  const response = await axios.get<SubmissionsResponse>(
    "/admin/submissions",
    { params }
  );
  return response.data;
}

/**
 * Get a single submission by id (public endpoint)
 */
export async function getSubmission(
  submissionId: number
): Promise<Submission> {
  const response = await axios.get<SubmissionResponse>(
    `/submissions/${submissionId}`
  );
  return response.data.submission;
}

/**
 * Get all submissions for a specific form (public endpoint)
 */
export async function getFormSubmissions(
  formId: string,
  params: Omit<GetSubmissionsParams, "form_id"> = {}
): Promise<SubmissionsResponse> {
  const response = await axios.get<SubmissionsResponse>(
    `/forms/${formId}/submissions`,
    { params }
  );
  return response.data;
}

/**
 * Delete a submission (ADMIN ONLY, requires auth token)
 */
export async function deleteSubmission(
  submissionId: number
): Promise<void> {
  await axios.delete(`/admin/submissions/${submissionId}`);
}
