import { useMemo, useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  CardContent,
  Divider,
  FormControlLabel,
  IconButton,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";

import type { FormField } from "../../../types/form";

interface Props {
  field: FormField | null;
  onSave: (field: FormField) => Promise<void>;
  onDelete?: () => void;
}

export default function PropertyPanel({
  field,
  onSave,
  onDelete,
}: Props) {
  const [editableField, setEditableField] =
    useState<FormField | null>(null);

  const [saving, setSaving] =
    useState(false);

  const [error, setError] =
    useState("");

  function handleOptionChange(
    index: number,
    value: string
  ) {
    if (!editableField) return;

    const options = [...(editableField.options ?? [])];

    options[index] = value;

    setEditableField({
      ...editableField,
      options,
    });
  }

  function handleAddOption() {
    if (!editableField) return;

    setEditableField({
      ...editableField,
      options: [
        ...(editableField.options ?? []),
        "",
      ],
    });
  }

  function handleDeleteOption(
    index: number
  ) {
    if (!editableField) return;

    const options = [...(editableField.options ?? [])];

    options.splice(index, 1);

    setEditableField({
      ...editableField,
      options,
    });
  }

  function updateValidation(
    key: string,
    value: any
  ) {
    if (!editableField) return;

    setEditableField({
      ...editableField,
      validation: {
        ...(editableField.validation ?? {}),
        [key]: value,
      },
    });
  }

  function updateVisibleIf(
    key: "field" | "equals",
    value: string
  ) {
    if (!editableField) return;

    setEditableField({
      ...editableField,
      visible_if: {
        ...(editableField.visible_if ?? {
          field: "",
          equals: "",
        }),
        [key]: value,
      },
    });
  }

  function updateEnabledIf(
    key: "field" | "equals",
    value: string
  ) {
    if (!editableField) return;

    setEditableField({
      ...editableField,
      enabled_if: {
        ...(editableField.enabled_if ?? {
          field: "",
          equals: "",
        }),
        [key]: value,
      },
    });
  }

  const isDirty = useMemo(() => {
    if (!field || !editableField) return false;

    return (
      editableField.name !== field.name ||
      editableField.placeholder !== field.placeholder ||
      editableField.help_text !== field.help_text ||
      editableField.required !== field.required ||
      JSON.stringify(editableField.options) !==
        JSON.stringify(field.options) ||
      JSON.stringify(editableField.validation) !==
        JSON.stringify(field.validation) ||
      JSON.stringify(editableField.visible_if) !==
        JSON.stringify(field.visible_if) ||
      JSON.stringify(editableField.enabled_if) !==
        JSON.stringify(field.enabled_if)
    );
  }, [field, editableField]);

  useEffect(() => {
    if (!field) {
      setEditableField(null);
      return;
    }

    // Clone to avoid mutating props
    setEditableField({ ...field });

    setError("");
  }, [field]);

  async function handleSave() {
    if (!editableField) return;

    try {
      setSaving(true);
      setError("");

      await onSave(editableField);
    } catch (err) {
      console.error(err);
      setError("Failed to save changes.");
    } finally {
      setSaving(false);
    }
  }

  if (!editableField) {
    return (
      <Card
        elevation={0}
        sx={{
          minHeight: 450,
        }}
      >
        <CardContent>
          <Typography variant="h6">
            Field Properties
          </Typography>

          <Divider sx={{ my: 2 }} />

          <Typography color="text.secondary">
            Select a field from the left to edit it.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card
      elevation={0}
      sx={{
        minHeight: 450,
      }}
    >
      <CardContent>
        <Typography variant="h6">
          Field Properties
        </Typography>

        <Divider sx={{ my: 2 }} />

        <Stack spacing={3}>
          {error && (
            <Alert severity="error">
              {error}
            </Alert>
          )}

          <TextField
            label="Field Name"
            value={editableField.name}
            onChange={(e) =>
              setEditableField({
                ...editableField,
                name: e.target.value,
              })
            }
            fullWidth
          />

          <TextField
            label="Placeholder"
            value={editableField.placeholder ?? ""}
            onChange={(e) =>
              setEditableField({
                ...editableField,
                placeholder: e.target.value,
              })
            }
            fullWidth
          />

          <TextField
            label="Help Text"
            value={editableField.help_text ?? ""}
            onChange={(e) =>
              setEditableField({
                ...editableField,
                help_text: e.target.value,
              })
            }
            multiline
            minRows={3}
            fullWidth
          />

          <FormControlLabel
            control={
              <Switch
                checked={editableField.required}
                onChange={(e) =>
                  setEditableField({
                    ...editableField,
                    required: e.target.checked,
                  })
                }
              />
            }
            label="Required"
          />

          {editableField.type === "dropdown" && (
            <Stack spacing={2}>
              <Typography
                variant="subtitle2"
              >
                Options
              </Typography>

              {(editableField.options ?? []).map(
                (option, index) => (
                  <Stack
                    key={index}
                    direction="row"
                    spacing={1}
                    sx={{ alignItems: "center" }}
                  >
                    <TextField
                      fullWidth
                      size="small"
                      value={option}
                      onChange={(e) =>
                        handleOptionChange(
                          index,
                          e.target.value
                        )
                      }
                    />

                    <IconButton
                      color="error"
                      onClick={() =>
                        handleDeleteOption(index)
                      }
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Stack>
                )
              )}

              <Button
                startIcon={<AddIcon />}
                onClick={handleAddOption}
              >
                Add Option
              </Button>
            </Stack>
          )}

          <Divider />

          <Typography variant="subtitle1">
            Validation
          </Typography>

          {editableField.type === "text" && (
            <Stack spacing={2}>
              <TextField
                label="Minimum Length"
                type="number"
                value={
                  editableField.validation
                    ?.minLength ?? ""
                }
                onChange={(e) =>
                  updateValidation(
                    "minLength",
                    e.target.value
                      ? Number(e.target.value)
                      : null
                  )
                }
              />

              <TextField
                label="Maximum Length"
                type="number"
                value={
                  editableField.validation
                    ?.maxLength ?? ""
                }
                onChange={(e) =>
                  updateValidation(
                    "maxLength",
                    e.target.value
                      ? Number(e.target.value)
                      : null
                  )
                }
              />
            </Stack>
          )}

          {editableField.type === "number" && (
            <Stack spacing={2}>
              <TextField
                label="Minimum Value"
                type="number"
                value={
                  editableField.validation?.min ?? ""
                }
                onChange={(e) =>
                  updateValidation(
                    "min",
                    e.target.value
                      ? Number(e.target.value)
                      : null
                  )
                }
              />

              <TextField
                label="Maximum Value"
                type="number"
                value={
                  editableField.validation?.max ?? ""
                }
                onChange={(e) =>
                  updateValidation(
                    "max",
                    e.target.value
                      ? Number(e.target.value)
                      : null
                  )
                }
              />
            </Stack>
          )}

          {editableField.type === "email" && (
            <TextField
              label="Regex Pattern"
              fullWidth
              value={
                editableField.validation?.regex ?? ""
              }
              onChange={(e) =>
                updateValidation("regex", e.target.value)
              }
            />
          )}

          {editableField.type === "media" && (
            <Stack spacing={2}>
              <TextField
                label="Maximum Image Size (MB)"
                type="number"
                value={
                  editableField.validation
                    ?.maxImageSizeMB ?? ""
                }
                onChange={(e) =>
                  updateValidation(
                    "maxImageSizeMB",
                    e.target.value
                      ? Number(e.target.value)
                      : null
                  )
                }
              />

              <TextField
                label="Allowed Extensions"
                helperText="Example: jpg,png,pdf"
                fullWidth
                value={
                  editableField.validation
                    ?.allowedExtensions?.join(",") ?? ""
                }
                onChange={(e) =>
                  updateValidation(
                    "allowedExtensions",
                    e.target.value
                      .split(",")
                      .map((s) => s.trim())
                      .filter((s) => s.length > 0)
                  )
                }
              />
            </Stack>
          )}

          <Divider sx={{ my: 2 }} />

          <Typography variant="subtitle1">
            Conditional Logic
          </Typography>

          <Stack spacing={2}>
            <Typography
              variant="subtitle2"
              color="text.secondary"
            >
              Visible If
            </Typography>

            <TextField
              label="Field ID"
              fullWidth
              value={
                editableField.visible_if?.field ?? ""
              }
              onChange={(e) =>
                updateVisibleIf("field", e.target.value)
              }
            />

            <TextField
              label="Equals"
              fullWidth
              value={
                editableField.visible_if?.equals ?? ""
              }
              onChange={(e) =>
                updateVisibleIf("equals", e.target.value)
              }
            />
          </Stack>

          <Stack spacing={2}>
            <Typography
              variant="subtitle2"
              color="text.secondary"
            >
              Enabled If
            </Typography>

            <TextField
              label="Field ID"
              fullWidth
              value={
                editableField.enabled_if?.field ?? ""
              }
              onChange={(e) =>
                updateEnabledIf("field", e.target.value)
              }
            />

            <TextField
              label="Equals"
              fullWidth
              value={
                editableField.enabled_if?.equals ?? ""
              }
              onChange={(e) =>
                updateEnabledIf("equals", e.target.value)
              }
            />
          </Stack>

          <TextField
            label="Field ID"
            value={editableField.id}
            slotProps={{
              input: {
                readOnly: true,
              },
            }}
            fullWidth
          />

          <TextField
            label="Field Type"
            value={editableField.type}
            slotProps={{
              input: {
                readOnly: true,
              },
            }}
            fullWidth
          />

          <Stack
            direction="row"
            spacing={2}
          >
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={saving || !isDirty}
              fullWidth
            >
              {saving ? "Saving..." : "Save Changes"}
            </Button>

            <Button
              variant="outlined"
              color="error"
              onClick={() => onDelete?.()}
            >
              Delete
            </Button>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}