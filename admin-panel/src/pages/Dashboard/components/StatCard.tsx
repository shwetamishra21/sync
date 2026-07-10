import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import {
  Box,
  Card,
  CardContent,
  Typography,
} from "@mui/material";

interface StatCardProps {
  title: string;
  value: string | number;
  color?: string;
}

export default function StatCard({
  title,
  value,
  color = "#1976D2",
}: StatCardProps) {
  return (
    <Card
      sx={{
        height: "100%",
        borderRadius: 3,
        border: "1px solid",
        borderColor: "divider",
        boxShadow: "0 4px 12px rgba(0,0,0,.05)",
      }}
    >
      <CardContent>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Box>
            <Typography
              variant="body2"
              color="text.secondary"
            >
              {title}
            </Typography>

            <Typography
              variant="h4"
              sx={{
                fontWeight: 700,
                mt: 0.5,
              }}
            >
              {value}
            </Typography>
          </Box>

          <TrendingUpIcon
            sx={{
              color,
              fontSize: 40,
            }}
          />
        </Box>
      </CardContent>
    </Card>
  );
}