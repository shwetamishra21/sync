import { useEffect, useState } from "react";

import { getHealth } from "../api/healthApi";
import { getForms } from "../api/formApi";

import type { DashboardStats } from "../types/dashboard";
import type { FormSummary } from "../types/form";

export function useDashboard() {

    const [stats, setStats] = useState<DashboardStats>({
        totalForms: 0,
        backendStatus: "",
        backendVersion: ""
    });

    const [forms, setForms] = useState<FormSummary[]>([]);

    const [loading, setLoading] = useState(true);

    useEffect(() => {

        async function loadDashboard() {

            try {

                const health = await getHealth();

                const formResponse = await getForms();

                setStats({

                    totalForms: formResponse.forms.length,

                    backendStatus: health.status,

                    backendVersion: health.version

                });

                setForms(formResponse.forms);

            } finally {

                setLoading(false);

            }

        }

        loadDashboard();

    }, []);

    return {

        stats,

        forms,

        loading

    };

}