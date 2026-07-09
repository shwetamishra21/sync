import { Grid } from "@mui/material";

import DashboardHeader from "./components/DashboardHeader";
import QuickActions from "./components/QuickActions";
import RecentFormsCard from "./components/RecentFormsCard";
import StatCard from "./components/StatCard";

export default function DashboardPage() {
  return (
    <>
      <DashboardHeader />

      <Grid container spacing={3}>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard title="Total Forms" value="14" />
        </Grid>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard title="Published" value="13" />
        </Grid>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard title="Fields" value="126" />
        </Grid>

        <Grid size={{ xs: 12, md: 3 }}>
          <StatCard
            title="Backend"
            value="Healthy"
            color="#16A34A"
          />
        </Grid>

        <Grid size={{ xs: 12, md: 8 }}>
          <RecentFormsCard />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <QuickActions />
        </Grid>

      </Grid>
    </>
  );
}