import { Alert, CircularProgress, Grid, Stack } from "@mui/material";
import { useNavigate } from "react-router-dom";

import DashboardHeader from "./components/DashboardHeader";
import QuickActions from "./components/QuickActions";
import RecentFormsCard from "./components/RecentFormsCard";
import StatCard from "./components/StatCard";
import { useDashboard } from "../../hooks/useDashboard";

export default function DashboardPage() {
  const navigate = useNavigate();

  const { stats, forms, loading, error } = useDashboard();

  if (loading) {
    return (
      <Stack
        sx={{
          display: "flex",
          justifyContent: "center",
          mt: 10,
        }}
      >
        <CircularProgress />
      </Stack>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  return (
    <>
      <DashboardHeader />

      <Grid container spacing={3}>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard title="Total Forms" value={stats.totalForms} />
        </Grid>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard title="Active Forms" value={stats.activeForms} />
        </Grid>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard title="Total Fields" value={stats.totalFields} />
        </Grid>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard title="Submissions" value={stats.totalSubmissions} />
        </Grid>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard
            title="Backend"
            value={stats.backendStatus === "success" ? "Healthy" : "Unavailable"}
            color={stats.backendStatus === "success" ? "#16A34A" : "#DC2626"}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 8 }}>
          <RecentFormsCard
            forms={forms}
            onSelect={(formId) => navigate(`/builder/${formId}`)}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <QuickActions
            onCreateForm={() => navigate("/forms")}
            onPreviewForm={() => navigate("/preview")}
            onViewSubmissions={() => navigate("/submissions")}
          />
        </Grid>

      </Grid>
    </>
  );
}
