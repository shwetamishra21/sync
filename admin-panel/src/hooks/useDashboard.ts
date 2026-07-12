import { useEffect, useState } from "react";

import { getHealth } from "../api/healthApi";
import { getForms } from "../api/formApi";
import { getSubmissions } from "../api/submissionApi";

import type { DashboardStats } from "../types/dashboard";
import type { FormSummary } from "../types/form";

export function useDashboard() {

    const [stats, setStats] = useState<DashboardStats>({
        totalForms: 0,
        activeForms: 0,
        totalFields: 0,
        totalSubmissions: 0,
        backendStatus: "",
        backendVersion: ""
    });

    const [forms, setForms] = useState<FormSummary[]>([]);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState<string | null>(null);

    useEffect(() => {

        async function loadDashboard() {

            try {

                setError(null);

                const [health, formResponse, submissionResponse] =
                    await Promise.all([
                        getHealth(),
                        getForms(),
                        // We only need the "total" count here, so ask for
                        // the smallest possible page.
                        getSubmissions({ limit: 1, offset: 0 }),
                    ]);

                const totalFields = formResponse.forms.reduce(
                    (sum, form) => sum + (form.field_count ?? 0),
                    0
                );

                const activeForms = formResponse.forms.filter(
                    (form) => form.is_active ?? true
                ).length;

                setStats({

                    totalForms: formResponse.forms.length,

                    activeForms,

                    totalFields,

                    totalSubmissions: submissionResponse.total ?? 0,

                    backendStatus: health.status,

                    backendVersion: health.version

                });

                setForms(formResponse.forms);

            } catch (err) {

                console.error("Error loading dashboard:", err);

                setError("Failed to load dashboard data");

            } finally {

                setLoading(false);

            }

        }

        loadDashboard();

    }, []);

    return {

        stats,

        forms,

        loading,

        error

    };

}
