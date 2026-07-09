import MoreVertIcon from "@mui/icons-material/MoreVert";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";

import {
  Box,
  Card,
  Chip,
  Divider,
  IconButton,
  Stack,
  Typography,
} from "@mui/material";

const forms = [
  {
    id: 1,
    name: "Resident Registration",
    description: "Government citizen registration form",
    version: "1.0",
    fields: 9,
    updated: "Today",
    status: "Active",
  },
  {
    id: 2,
    name: "Water Survey",
    description: "Village water source survey",
    version: "2.1",
    fields: 6,
    updated: "Yesterday",
    status: "Draft",
  },
  {
    id: 3,
    name: "Farmer Registration",
    description: "Farmer welfare registration form",
    version: "1.3",
    fields: 14,
    updated: "2 days ago",
    status: "Active",
  },
];

export default function FormsList() {
  return (
    <Card
      elevation={0}
      sx={{
        borderRadius: 4,
        border: "1px solid",
        borderColor: "divider",
        overflow: "hidden",
      }}
    >
      {forms.map((form, index) => (
        <Box key={form.id}>
          <Box
            sx={{
              px: 3,
              py: 2.5,
              transition: "0.2s",
              cursor: "pointer",

              "&:hover": {
                bgcolor: "#F8FAFC",
              },
            }}
          >
            <Stack
              direction="row"
              justifyContent="space-between"
              alignItems="flex-start"
            >
              <Stack spacing={1}>
                <Stack
                  direction="row"
                  spacing={1.5}
                  alignItems="center"
                >
                  <DescriptionOutlinedIcon color="primary" />

                  <Typography
                    variant="h6"
                    sx={{
                      fontWeight: 600,
                    }}
                  >
                    {form.name}
                  </Typography>

                  <Chip
                    label={form.status}
                    size="small"
                    color={
                      form.status === "Active"
                        ? "success"
                        : "warning"
                    }
                  />
                </Stack>

                <Typography
                  color="text.secondary"
                  sx={{
                    fontSize: 14,
                  }}
                >
                  {form.description}
                </Typography>

                <Typography
                  color="text.secondary"
                  sx={{
                    fontSize: 13,
                  }}
                >
                  Version {form.version}
                  {" • "}
                  {form.fields} Fields
                  {" • "}
                  Updated {form.updated}
                </Typography>
              </Stack>

              <IconButton>
                <MoreVertIcon />
              </IconButton>
            </Stack>
          </Box>

          {index < forms.length - 1 && <Divider />}
        </Box>
      ))}
    </Card>
  );
}