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
        display="flex"
        justifyContent="space-between"
        alignItems="center"
      >

        <Typography
          color="text.secondary"
          fontSize={14}
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
        mt={2}
        variant="h4"
        fontWeight={700}
      >
        {value}
      </Typography>

      {subtitle && (
        <Typography
          mt={1}
          color="success.main"
          fontSize={13}
        >
          {subtitle}
        </Typography>
      )}

    </AppCard>
  );
}