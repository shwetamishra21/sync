import {
  Box,
  Typography,
} from "@mui/material";

import TrendingUpIcon from "@mui/icons-material/TrendingUp";

import AppCard from "./AppCard";

interface Props {
  title: string;
  value: string | number;
  subtitle?: string;
  color?: string;
}

export default function MetricCard({
  title,
  value,
  subtitle,
  color = "#2563EB",
}: Props) {
  return (
    <AppCard>

      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >

        <Typography
          color="text.secondary"
          sx={{ fontSize: 14 }}
        >
          {title}
        </Typography>

        <TrendingUpIcon
          sx={{
            color,
          }}
        />

      </Box>

      <Typography
        variant="h4"
        sx={{
          mt: 2,
          fontWeight: 700,
        }}
      >
        {value}
      </Typography>

      {subtitle && (
        <Typography
          color="success.main"
          sx={{
            mt: 1,
            fontSize: 13,
          }}
        >
          {subtitle}
        </Typography>
      )}

    </AppCard>
  );
}
