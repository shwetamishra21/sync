import {
  Box,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemSecondaryAction,
  ListItemText,
  Stack,
  Typography,
} from "@mui/material";
import DragIndicatorIcon from "@mui/icons-material/DragIndicator";
import KeyboardArrowUpIcon from "@mui/icons-material/KeyboardArrowUp";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import type { FormField } from "../../../types/form";

interface Props {
  fields: FormField[];
  selectedFieldId?: string;
  onSelect: (field: FormField) => void;
  onMoveUp: (index: number) => void;
  onMoveDown: (index: number) => void;
}

export default function FieldList({
  fields,
  selectedFieldId,
  onSelect,
  onMoveUp,
  onMoveDown,
}: Props) {
  if (fields.length === 0) {
    return (
      <Stack
        sx={{
          p: 3,
          border: "1px dashed",
          borderColor: "divider",
          borderRadius: 2,
          textAlign: "center",
        }}
      >
        <Typography
          variant="body2"
          color="text.secondary"
        >
          No fields yet. Click "Add Field" to get started.
        </Typography>
      </Stack>
    );
  }

  return (
    <Box
      sx={{
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 2,
        overflow: "hidden",
      }}
    >
      <List sx={{ p: 0 }}>
        {fields.map((field, index) => (
          <ListItemButton
            key={field.id}
            selected={selectedFieldId === field.id}
            onClick={() => onSelect(field)}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>
              <DragIndicatorIcon fontSize="small" />
            </ListItemIcon>

            <ListItemText
              primary={field.name}
              secondary={field.type}
              primaryTypographyProps={{
                variant: "body2",
                fontWeight: 500,
              }}
              secondaryTypographyProps={{
                variant: "caption",
              }}
            />

            <ListItemSecondaryAction>
              <IconButton
                size="small"
                disabled={index === 0}
                onClick={(e) => {
                  e.stopPropagation();
                  onMoveUp(index);
                }}
                title="Move up"
              >
                <KeyboardArrowUpIcon fontSize="small" />
              </IconButton>

              <IconButton
                size="small"
                disabled={index === fields.length - 1}
                onClick={(e) => {
                  e.stopPropagation();
                  onMoveDown(index);
                }}
                title="Move down"
              >
                <KeyboardArrowDownIcon fontSize="small" />
              </IconButton>
            </ListItemSecondaryAction>
          </ListItemButton>
        ))}
      </List>
    </Box>
  );
}