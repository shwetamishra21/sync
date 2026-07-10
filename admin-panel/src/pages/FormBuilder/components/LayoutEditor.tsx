
import { 
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Grid,
  TextField,
  Switch,
  FormControlLabel,
  CircularProgress,
  Stack,
  MenuItem,
} from "@mui/material";
import { useCallback, useEffect, useState } from "react";
import type { LayoutConfig } from "../../../types/form";

interface Props {
  layout: LayoutConfig;
  onSave: (layout: LayoutConfig) => Promise<void>;
}

export default function LayoutEditor({
  layout,
  onSave,
}: Props) {
  const [editableLayout, setEditableLayout] =
    useState<LayoutConfig>(layout);

  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setEditableLayout(layout);
  }, [layout]);

  const handleNumberChange = useCallback(
    (field: keyof LayoutConfig, value: string) => {
      const numValue = parseInt(value, 10);
      if (!isNaN(numValue)) {
        setEditableLayout((prev) => ({
          ...prev,
          [field]: numValue,
        }));
      }
    },
    []
  );

  const handleStringChange = useCallback(
    (field: keyof LayoutConfig, value: string) => {
      setEditableLayout((prev) => ({
        ...prev,
        [field]: value,
      }));
    },
    []
  );

  const handleBooleanChange = useCallback(
    (field: keyof LayoutConfig, value: boolean) => {
      setEditableLayout((prev) => ({
        ...prev,
        [field]: value,
      }));
    },
    []
  );

  const handleSave = useCallback(
    async () => {
      try {
        setSaving(true);
        await onSave(editableLayout);
      } catch (error) {
        console.error("Failed to save layout:", error);
      } finally {
        setSaving(false);
      }
    },
    [editableLayout, onSave]
  );

  const handleReset = useCallback(() => {
    setEditableLayout(layout);
  }, [layout]);

  return (
    <Card>
      <CardHeader title="Layout Editor" />
      <CardContent>
        <Stack spacing={3}>
          <Grid container spacing={2}>
            {/* Columns */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Columns"
                type="number"
                value={editableLayout.columns || 1}
                onChange={(e) =>
                  handleNumberChange(
                    "columns",
                    e.target.value
                  )
                }
                slotProps={{
                  htmlInput: {
                    min: 1,
                    max: 4,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Spacing */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Spacing (px)"
                type="number"
                value={editableLayout.spacing || 16}
                onChange={(e) =>
                  handleNumberChange(
                    "spacing",
                    e.target.value
                  )
                }
                slotProps={{
                  htmlInput: {
                    min: 0,
                    max: 100,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Field Style */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Field Style"
                select
                value={editableLayout.fieldStyle || "outlined"}
                onChange={(e) =>
                  handleStringChange(
                    "fieldStyle",
                    e.target.value
                  )
                }
                variant="outlined"
              >
                <MenuItem value="outlined">
                  Outlined
                </MenuItem>
                <MenuItem value="filled">
                  Filled
                </MenuItem>
                <MenuItem value="standard">
                  Standard
                </MenuItem>
              </TextField>
            </Grid>

            {/* Label Position */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Label Position"
                select
                value={
                  editableLayout.labelPosition ||
                  "top"
                }
                onChange={(e) =>
                  handleStringChange(
                    "labelPosition",
                    e.target.value
                  )
                }
                variant="outlined"
              >
                <MenuItem value="top">Top</MenuItem>
                <MenuItem value="left">Left</MenuItem>
                <MenuItem value="floating">
                  Floating
                </MenuItem>
              </TextField>
            </Grid>

            {/* Card Padding */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Card Padding (px)"
                type="number"
                value={
                  editableLayout.cardPadding || 24
                }
                onChange={(e) =>
                  handleNumberChange(
                    "cardPadding",
                    e.target.value
                  )
                }
                slotProps={{
                  htmlInput: {
                    min: 0,
                    max: 100,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Section Spacing */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Section Spacing (px)"
                type="number"
                value={
                  editableLayout.sectionSpacing || 32
                }
                onChange={(e) =>
                  handleNumberChange(
                    "sectionSpacing",
                    e.target.value
                  )
                }
                slotProps={{
                  htmlInput: {
                    min: 0,
                    max: 100,
                  },
                }}
                variant="outlined"
              />
            </Grid>
          </Grid>

          {/* Toggle Switches */}
          <Box sx={{ borderTop: "1px solid #e0e0e0", pt: 2 }}>
            <Stack spacing={1}>
              <FormControlLabel
                control={
                  <Switch
                    checked={
                      editableLayout.showDividers ??
                      true
                    }
                    onChange={(e) =>
                      handleBooleanChange(
                        "showDividers",
                        e.target.checked
                      )
                    }
                  />
                }
                label="Show Dividers"
              />
            </Stack>
          </Box>

          {/* Action Buttons */}
          <Box
            sx={{
              display: "flex",
              gap: 1,
              justifyContent: "flex-end",
            }}
          >
            <Button
              variant="outlined"
              onClick={handleReset}
              disabled={saving}
            >
              Reset
            </Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={saving}
              startIcon={
                saving ? (
                  <CircularProgress size={20} />
                ) : null
              }
            >
              {saving ? "Saving..." : "Save Layout"}
            </Button>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}