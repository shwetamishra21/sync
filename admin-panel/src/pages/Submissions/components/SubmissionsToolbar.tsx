import SearchIcon from "@mui/icons-material/Search";
import {
  Box,
  InputAdornment,
  MenuItem,
  TextField,
} from "@mui/material";
import type { FormSummary } from "../../../types/form";

interface Props {
  search: string;
  onSearchChange: (value: string) => void;
  forms: FormSummary[];
  formFilter: string;
  onFormFilterChange: (value: string) => void;
  statusFilter: string;
  onStatusFilterChange: (value: string) => void;
}

export default function SubmissionsToolbar({
  search,
  onSearchChange,
  forms,
  formFilter,
  onFormFilterChange,
  statusFilter,
  onStatusFilterChange,
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
      }}
    >
      <TextField
        size="small"
        placeholder="Search by submission ID..."
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        sx={{ minWidth: 280 }}
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
        label="Form"
        value={formFilter}
        onChange={(e) => onFormFilterChange(e.target.value)}
        sx={{ width: 220 }}
      >
        <MenuItem value="all">All Forms</MenuItem>
        {forms.map((form) => (
          <MenuItem
            key={form.id}
            value={form.id}
          >
            {form.name}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        select
        size="small"
        label="Status"
        value={statusFilter}
        onChange={(e) => onStatusFilterChange(e.target.value)}
        sx={{ width: 170 }}
      >
        <MenuItem value="all">All Statuses</MenuItem>
        <MenuItem value="SYNCED">Synced</MenuItem>
        <MenuItem value="PENDING">Pending</MenuItem>
        <MenuItem value="SYNCING">Syncing</MenuItem>
        <MenuItem value="FAILED">Failed</MenuItem>
      </TextField>
    </Box>
  );
}
