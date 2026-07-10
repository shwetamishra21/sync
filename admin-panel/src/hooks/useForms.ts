// File 3: useForms.ts
import { useState, useEffect, useCallback } from "react";
import { getForms } from "../api/formApi";
import type { FormSummary } from "../types/form";

export function useForms() {
  const [forms, setForms] = useState<FormSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadForms = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await getForms();

      setForms(response.forms || []);
    } catch (err) {
      console.error("Error loading forms:", err);
      setError("Failed to load forms");
      setForms([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadForms();
  }, [loadForms]);

  return {
    forms,
    loading,
    error,
    refreshForms: loadForms,
  };
}