import { Card, CardContent } from "@mui/material";
import type { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export default function AppCard({ children }: Props) {
  return (
    <Card
      elevation={0}
      sx={{
        borderRadius: 4,
        border: "1px solid #E5E7EB",
        backgroundColor: "#FFFFFF",
        transition: "all .2s ease",

        "&:hover": {
          boxShadow: "0 8px 24px rgba(0,0,0,.08)",
          transform: "translateY(-2px)",
        },
      }}
    >
      <CardContent>{children}</CardContent>
    </Card>
  );
}