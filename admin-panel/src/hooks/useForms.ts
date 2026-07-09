import { useEffect, useState } from "react";
import { getForms } from "../api/formApi";
import type { FormSummary } from "../types/form";

export function useForms() {
  const [forms, setForms] = useState<FormSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    async function loadForms() {
      try {
        const response = await getForms();
        setForms(response.forms);
      } catch (err) {
        console.error(err);
        setError(true);
      } finally {
        setLoading(false);
      }
    }

    loadForms();
  }, []);

  return {
    forms,
    loading,
    error,
  };
}