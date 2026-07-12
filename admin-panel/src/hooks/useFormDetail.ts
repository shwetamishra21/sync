import { useCallback, useEffect, useState } from "react";
import { getFormDetail, getAdminFormDetail } from "../api/formApi";
import type { FormDetail } from "../types/form";

/**
 * Hook to load and manage form detail
 * @param formId - The form ID to load
 * @param adminMode - If true, uses admin endpoint (/admin/forms/{id}) which allows editing inactive forms
 *                    If false, uses public endpoint (/forms/{id}) which only returns active forms
 */
export function useFormDetail(
  formId: string,
  adminMode: boolean = false
) {
  const [form, setForm] =
    useState<FormDetail | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState(false);

  const loadForm = useCallback(async () => {
    // Skip fetching when no form is selected yet (e.g. Preview page
    // before the user picks a form). Calling /forms/ or /admin/forms/
    // with an empty id would otherwise hit a non-existent route.
    if (!formId) {
      setForm(null);
      setError(false);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(false);

      // Use admin endpoint if in admin mode, otherwise use public endpoint
      const response = adminMode
        ? await getAdminFormDetail(formId)
        : await getFormDetail(formId);

      setForm(response);
    } catch (e) {
      console.error(e);
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [formId, adminMode]);

  useEffect(() => {
    loadForm();
  }, [loadForm]);

  return {
    form,
    loading,
    error,
    refreshForm: loadForm,
  };
}