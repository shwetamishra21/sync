// FormsToolbar.tsx - FIXED
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";

import {
  Box,
  Button,
  InputAdornment,
  MenuItem,
  TextField,
} from "@mui/material";

interface Props {
  search: string;
  onSearchChange: (value: string) => void;
  onCreate: () => void;
}

export default function FormsToolbar({
  search,
  onSearchChange,
  onCreate,
}: Props) {
  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: {
          xs: "column",
          md: "row",
        },
        gap: 2,
        justifyContent: "space-between",
        alignItems: {
          xs: "stretch",
          md: "center",
        },
      }}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: {
            xs: "column",
            sm: "row",
          },
          gap: 2,
          flex: 1,
        }}
      >
        <TextField
          size="small"
          placeholder="Search forms..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          sx={{ minWidth: 320 }}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            },
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
      </Box>

      <Button
        variant="contained"
        startIcon={<AddIcon />}
        onClick={onCreate}
        sx={{
          borderRadius: 3,
          px: 3,
          textTransform: "none",
        }}
      >
        Create Form
      </Button>
    </Box>
  );
}