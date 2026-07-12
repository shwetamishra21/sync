import { useCallback, useEffect, useState } from "react";
import { getSubmissions } from "../api/submissionApi";
import type { GetSubmissionsParams, Submission } from "../types/submission";

export function useSubmissions(
  filters: GetSubmissionsParams = {}
) {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const formId = filters.form_id;
  const status = filters.status;
  const limit = filters.limit;
  const offset = filters.offset;

  const loadSubmissions = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await getSubmissions({
        form_id: formId,
        status,
        limit,
        offset,
      });

      setSubmissions(response.submissions || []);
      setTotal(response.total ?? response.submissions?.length ?? 0);
    } catch (err) {
      console.error("Error loading submissions:", err);
      setError("Failed to load submissions");
      setSubmissions([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [formId, status, limit, offset]);

  useEffect(() => {
    loadSubmissions();
  }, [loadSubmissions]);

  return {
    submissions,
    total,
    loading,
    error,
    refreshSubmissions: loadSubmissions,
  };
}
