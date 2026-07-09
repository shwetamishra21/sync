import {
  Dashboard,
  Description,
  Preview,
  Settings,
} from "@mui/icons-material";

import {
  Box,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
} from "@mui/material";

import { NavLink } from "react-router-dom";

const menuItems = [
  {
    title: "Dashboard",
    icon: <Dashboard />,
    path: "/dashboard",
  },
  {
    title: "Forms",
    icon: <Description />,
    path: "/forms",
  },
  {
    title: "Preview",
    icon: <Preview />,
    path: "/preview",
  },
  {
    title: "Settings",
    icon: <Settings />,
    path: "/settings",
  },
];

export default function Sidebar() {
  return (
    <Box
      sx={{
        width: 260,
        bgcolor: "white",
        borderRight: "1px solid #E5E7EB",
        height: "100vh",
      }}
    >
      <Typography
        variant="h5"
        sx={{
          p: 3,
          fontWeight: 700,
        }}
      >
        JSAC Admin
      </Typography>

      <List>
        {menuItems.map((item) => (
          <ListItemButton
            key={item.path}
            component={NavLink}
            to={item.path}
            sx={{
              mx: 2,
              borderRadius: 2,
              mb: 1,
            }}
          >
            <ListItemIcon>{item.icon}</ListItemIcon>

            <ListItemText primary={item.title} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );
}