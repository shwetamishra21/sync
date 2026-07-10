import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Grid,
  TextField,
  CircularProgress,
  Stack,
} from "@mui/material";
import { useCallback, useEffect, useState } from "react";
import type { ThemeConfig } from "../../../types/form";

interface Props {
  theme: ThemeConfig;
  onSave: (theme: ThemeConfig) => Promise<void>;
}

export default function ThemeEditor({
  theme,
  onSave,
}: Props) {
  const [editableTheme, setEditableTheme] =
    useState<ThemeConfig>(theme);

  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setEditableTheme(theme);
  }, [theme]);

  const handleColorChange = useCallback(
    (field: keyof ThemeConfig, value: string) => {
      setEditableTheme((prev) => ({
        ...prev,
        [field]: value,
      }));
    },
    []
  );

  const handleNumberChange = useCallback(
    (field: keyof ThemeConfig, value: string) => {
      const numValue = parseInt(value, 10);
      if (!isNaN(numValue)) {
        setEditableTheme((prev) => ({
          ...prev,
          [field]: numValue,
        }));
      }
    },
    []
  );

  const handleSave = useCallback(
    async () => {
      try {
        setSaving(true);
        await onSave(editableTheme);
      } catch (error) {
        console.error("Failed to save theme:", error);
      } finally {
        setSaving(false);
      }
    },
    [editableTheme, onSave]
  );

  const handleReset = useCallback(() => {
    setEditableTheme(theme);
  }, [theme]);

  return (
    <Card>
      <CardHeader title="Theme Editor" />
      <CardContent>
        <Stack spacing={3}>
          <Grid container spacing={2}>
            {/* Primary Color */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Primary Color"
                type="color"
                value={
                  editableTheme.primaryColor || "#1976d2"
                }
                onChange={(e) =>
                  handleColorChange(
                    "primaryColor",
                    e.target.value
                  )
                }
                slotProps={{
                  inputLabel: {
                    shrink: true,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Secondary Color */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Secondary Color"
                type="color"
                value={
                  editableTheme.secondaryColor || "#dc004e"
                }
                onChange={(e) =>
                  handleColorChange(
                    "secondaryColor",
                    e.target.value
                  )
                }
                slotProps={{
                  inputLabel: {
                    shrink: true,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Background Color */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Background Color"
                type="color"
                value={
                  editableTheme.backgroundColor ||
                  "#ffffff"
                }
                onChange={(e) =>
                  handleColorChange(
                    "backgroundColor",
                    e.target.value
                  )
                }
                slotProps={{
                  inputLabel: {
                    shrink: true,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Surface Color */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Surface Color"
                type="color"
                value={
                  editableTheme.surfaceColor ||
                  "#f5f5f5"
                }
                onChange={(e) =>
                  handleColorChange(
                    "surfaceColor",
                    e.target.value
                  )
                }
                slotProps={{
                  inputLabel: {
                    shrink: true,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Button Color */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Button Color"
                type="color"
                value={
                  editableTheme.buttonColor || "#1976d2"
                }
                onChange={(e) =>
                  handleColorChange(
                    "buttonColor",
                    e.target.value
                  )
                }
                slotProps={{
                  inputLabel: {
                    shrink: true,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Button Text Color */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Button Text Color"
                type="color"
                value={
                  editableTheme.buttonTextColor ||
                  "#ffffff"
                }
                onChange={(e) =>
                  handleColorChange(
                    "buttonTextColor",
                    e.target.value
                  )
                }
                slotProps={{
                  inputLabel: {
                    shrink: true,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Text Color */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Text Color"
                type="color"
                value={
                  editableTheme.textColor || "#000000"
                }
                onChange={(e) =>
                  handleColorChange(
                    "textColor",
                    e.target.value
                  )
                }
                slotProps={{
                  inputLabel: {
                    shrink: true,
                  },
                }}
                variant="outlined"
              />
            </Grid>

            {/* Corner Radius */}
            <Grid
              size={{
                xs: 12,
                sm: 6,
              }}
            >
              <TextField
                fullWidth
                label="Corner Radius (px)"
                type="number"
                value={
                  editableTheme.cornerRadius || 4
                }
                onChange={(e) =>
                  handleNumberChange(
                    "cornerRadius",
                    e.target.value
                  )
                }
                slotProps={{
                  htmlInput: {
                    min: 0,
                    max: 50,
                  },
                }}
                variant="outlined"
              />
            </Grid>
          </Grid>

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
              {saving ? "Saving..." : "Save Theme"}
            </Button>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}