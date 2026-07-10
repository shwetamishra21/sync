// src/pages/Forms/components/FormDialog.tsx
import { useEffect, useState } from "react";

import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Box,
} from "@mui/material";

import type { CreateFormRequest } from "../../../types/form";

interface Props {
  open: boolean;
  loading?: boolean;
  initialValues?: CreateFormRequest;
  title: string;
  submitLabel: string;
  onClose: () => void;
  onSubmit: (values: CreateFormRequest) => void;
}

export default function FormDialog({
  open,
  loading = false,
  initialValues,
  title,
  submitLabel,
  onClose,
  onSubmit,
}: Props) {
  const [values, setValues] = useState<CreateFormRequest>({
    id: "",
    name: "",
    description: "",
    version: "1.0",
  });

  useEffect(() => {
    if (initialValues) {
      setValues(initialValues);
    } else {
      setValues({
        id: "",
        name: "",
        description: "",
        version: "1.0",
      });
    }
  }, [initialValues, open]);

  function handleChange(field: keyof CreateFormRequest) {
    return (event: React.ChangeEvent<HTMLInputElement>) => {
      setValues((prev) => ({
        ...prev,
        [field]: event.target.value,
      }));
    };
  }

  function handleSubmit() {
    if (!values.id.trim()) return;

    if (!values.name.trim()) return;

    onSubmit(values);
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="sm"
    >
      <DialogTitle>{title}</DialogTitle>

      <DialogContent>
        <Box sx={{ display: "flex", flexDirection: "column", gap: 3, mt: 1 }}>
          <TextField
            label="Form ID"
            disabled={!!initialValues}
            value={values.id}
            onChange={handleChange("id")}
            fullWidth
            required
            helperText="Unique identifier"
          />

          <TextField
            label="Form Name"
            value={values.name}
            onChange={handleChange("name")}
            fullWidth
            required
          />

          <TextField
            label="Description"
            value={values.description}
            onChange={handleChange("description")}
            fullWidth
            multiline
            minRows={3}
          />

          <TextField
            label="Version"
            value={values.version}
            onChange={handleChange("version")}
            fullWidth
          />
        </Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button
          onClick={onClose}
          disabled={loading}
        >
          Cancel
        </Button>

        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={loading}
        >
          {submitLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}