import { Box } from "@mui/material";
import { Outlet } from "react-router-dom";

import Sidebar from "../components/layout/Sidebar";
import TopBar from "../components/layout/TopBar";

export default function AdminLayout() {
  return (
    <Box
      sx={{
        display: "flex",
        height: "100vh",
      }}
    >
      <Sidebar />

      <Box
        sx={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
        }}
      >
        <TopBar />

        <Box
          sx={{
            p: 4,
            flex: 1,
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}