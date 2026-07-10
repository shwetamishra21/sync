// src/pages/Forms/components/FormsList.tsx - UPDATED
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import {
  Box,
  Card,
  Chip,
  Divider,
  Typography,
} from "@mui/material";
import type { FormSummary } from "../../../types/form";
import FormActionsMenu from "./FormActionsMenu";

interface Props {
  forms: FormSummary[];
  onBuilder: (form: FormSummary) => void;
  onEdit: (form: FormSummary) => void;
  onDelete: (form: FormSummary) => void;
  onToggleActive: (form: FormSummary) => void;
}

const formatDate = (date: string) =>
  new Date(date).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

export default function FormsList({
  forms,
  onBuilder,
  onEdit,
  onDelete,
  onToggleActive,
}: Props) {
  if (forms.length === 0) {
    return (
      <Card
        elevation={0}
        sx={{
          borderRadius: 3,
          border: "1px solid",
          borderColor: "divider",
          py: 8,
          textAlign: "center",
        }}
      >
        <Typography variant="h6">
          No Forms Available
        </Typography>

        <Typography
          color="text.secondary"
          sx={{ mt: 1 }}
        >
          Create your first dynamic form to get started.
        </Typography>
      </Card>
    );
  }

  return (
    <Card
      elevation={0}
      sx={{
        borderRadius: 3,
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
              cursor: "pointer",
              transition: ".2s",
              "&:hover": {
                bgcolor: "action.hover",
              },
            }}
          >
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
                gap: 2,
              }}
            >
              <Box>
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 1.5,
                  }}
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
                    label={
                      (form.is_active ?? true)
                        ? "Active"
                        : "Inactive"
                    }
                    size="small"
                    color={
                      (form.is_active ?? true)
                        ? "success"
                        : "default"
                    }
                  />
                </Box>
                <Typography
                  color="text.secondary"
                  sx={{
                    mt: 1,
                    maxWidth: 700,
                  }}
                >
                  {form.description}
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{
                    mt: 0.5,
                  }}
                >
                  Version {form.version}
                  {" • "}
                  {form.field_count}{" "}
                  {form.field_count === 1 ? "Field" : "Fields"}
                  {" • "}
                  Created {formatDate(form.created_at)}
                </Typography>
              </Box>
              <FormActionsMenu
                form={form}
                onBuilder={onBuilder}
                onEdit={onEdit}
                onDelete={onDelete}
                onToggleActive={onToggleActive}
              />
            </Box>
          </Box>
          {index !== forms.length - 1 && <Divider />}
        </Box>
      ))}
    </Card>
  );
}