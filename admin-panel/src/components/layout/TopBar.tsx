import { Notifications } from "@mui/icons-material";
import {
  AppBar,
  Avatar,
  Box,
  IconButton,
  Toolbar,
  Typography,
} from "@mui/material";

export default function TopBar() {
  return (
    <AppBar
      position="static"
      color="inherit"
      elevation={0}
      sx={{
        borderBottom: "1px solid #E5E7EB",
      }}
    >
      <Toolbar>
        <Typography
          variant="h6"
          sx={{
            flexGrow: 1,
          }}
        >
          Dynamic Forms Admin
        </Typography>

        <IconButton>
          <Notifications />
        </IconButton>

        <Box
          sx={{
            ml: 2,
          }}
        >
          <Avatar>A</Avatar>
        </Box>
      </Toolbar>
    </AppBar>
  );
}