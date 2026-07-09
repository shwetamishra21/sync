import { Box, Typography } from "@mui/material";

export default function DashboardHeader() {
  return (
    <Box
      sx={{
        mb: 4,
      }}
    >
      <Typography
        variant="h4"
        gutterBottom
        sx={{
          fontWeight: 700,
        }}
      >
        Dashboard
      </Typography>

      <Typography color="text.secondary">
        Welcome back, Administrator. Manage dynamic forms across the
        organization from one place.
      </Typography>
    </Box>
  );
}