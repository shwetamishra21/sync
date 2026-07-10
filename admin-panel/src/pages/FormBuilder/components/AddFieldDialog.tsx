import { useEffect, useState } from "react";

import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Switch,
  TextField,
} from "@mui/material";

import type {
  CreateFieldRequest,
} from "../../../types/form";

interface Props {
  open: boolean;
  loading?: boolean;
  onClose: () => void;
  onCreate: (
    field: CreateFieldRequest
  ) => Promise<void>;
}

const initialState: CreateFieldRequest = {
  field_id: "",
  name: "",
  type: "text",
  required: false,
  placeholder: "",
};

export default function AddFieldDialog({
  open,
  loading = false,
  onClose,
  onCreate,
}: Props) {
  const [field, setField] =
    useState<CreateFieldRequest>(initialState);

  useEffect(() => {
    if (open) {
      setField(initialState);
    }
  }, [open]);

  async function handleSubmit() {
    await onCreate(field);
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="sm"
    >
      <DialogTitle>
        Add Field
      </DialogTitle>

      <DialogContent>
        <Stack
          spacing={3}
          sx={{ mt: 1 }}
        >
          <TextField
            label="Field Name"
            value={field.name}
            onChange={(e) =>
              setField({
                ...field,
                name: e.target.value,
              })
            }
            fullWidth
          />

          <TextField
            label="Field ID"
            helperText="Unique identifier used by Android"
            value={field.field_id}
            onChange={(e) =>
              setField({
                ...field,
                field_id: e.target.value,
              })
            }
            fullWidth
          />

          <FormControl fullWidth>
            <InputLabel>
              Field Type
            </InputLabel>

            <Select
              label="Field Type"
              value={field.type}
              onChange={(e) =>
                setField({
                  ...field,
                  type: e.target.value,
                })
              }
            >
              <MenuItem value="text">
                Text
              </MenuItem>

              <MenuItem value="textarea">
                Text Area
              </MenuItem>

              <MenuItem value="number">
                Number
              </MenuItem>

              <MenuItem value="email">
                Email
              </MenuItem>

              <MenuItem value="date">
                Date
              </MenuItem>

              <MenuItem value="dropdown">
                Dropdown
              </MenuItem>

              <MenuItem value="media">
                Media
              </MenuItem>

              <MenuItem value="gps">
                GPS
              </MenuItem>
            </Select>
          </FormControl>

          <TextField
            label="Placeholder"
            value={field.placeholder}
            onChange={(e) =>
              setField({
                ...field,
                placeholder: e.target.value,
              })
            }
            fullWidth
          />

          <FormControlLabel
            control={
              <Switch
                checked={field.required ?? false}
                onChange={(e) =>
                  setField({
                    ...field,
                    required: e.target.checked,
                  })
                }
              />
            }
            label="Required Field"
          />
        </Stack>
      </DialogContent>

      <DialogActions>
        <Button
          onClick={onClose}
        >
          Cancel
        </Button>

        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={
            loading ||
            !field.name.trim() ||
            !field.field_id.trim()
          }
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
}