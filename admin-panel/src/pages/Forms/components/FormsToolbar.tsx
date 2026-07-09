import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";

import {
  Box,
  Button,
  InputAdornment,
  MenuItem,
  Stack,
  TextField,
} from "@mui/material";

export default function FormsToolbar() {
  return (
    <Stack
      direction={{ xs: "column", md: "row" }}
      spacing={2}
      justifyContent="space-between"
      alignItems={{ xs: "stretch", md: "center" }}
    >
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={2}
        sx={{ flex: 1 }}
      >
        <TextField
          size="small"
          placeholder="Search forms..."
          sx={{ minWidth: 320 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />

        <TextField
          select
          size="small"
          defaultValue="all"
          sx={{ width: 170 }}
        >
          <MenuItem value="all">All Forms</MenuItem>
          <MenuItem value="active">Active</MenuItem>
          <MenuItem value="inactive">Inactive</MenuItem>
          <MenuItem value="draft">Draft</MenuItem>
        </TextField>
      </Stack>

      <Button
        variant="contained"
        startIcon={<AddIcon />}
        sx={{
          borderRadius: 3,
          px: 3,
          textTransform: "none",
        }}
      >
        Create Form
      </Button>
    </Stack>
  );
}