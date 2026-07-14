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
  MenuItem,
} from "@mui/material";
import { useCallback, useEffect, useState } from "react";
import type { BrandingConfig } from "../../../types/form";
import { asPlainObject } from "../../../utils/normalizeConfig";

interface Props {
  branding: BrandingConfig;
  onSave: (branding: BrandingConfig) => Promise<void>;
}

const DEFAULT_BRANDING: BrandingConfig = {
  logo: "",
  banner: "",
  organizationName: "",
  titleAlignment: "left",
};

export default function BrandingEditor({
  branding,
  onSave,
}: Props) {
  const [editableBranding, setEditableBranding] = useState<BrandingConfig>(
    asPlainObject(branding, DEFAULT_BRANDING)
  );

  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setEditableBranding(asPlainObject(branding, DEFAULT_BRANDING));
  }, [branding]);

  const handleStringChange = useCallback(
    (field: keyof BrandingConfig, value: string) => {
      setEditableBranding((prev) => ({
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
        await onSave(editableBranding);
      } catch (error) {
        console.error("Failed to save branding:", error);
      } finally {
        setSaving(false);
      }
    },
    [editableBranding, onSave]
  );

  const handleReset = useCallback(() => {
    setEditableBranding(asPlainObject(branding, DEFAULT_BRANDING));
  }, [branding]);

  return (
    <Card>
      <CardHeader title="Branding Editor" />
      <CardContent>
        <Stack spacing={3}>
          <Grid container spacing={2}>
            {/* Organization Name */}
            <Grid
              size={{
                xs: 12,
              }}
            >
              <TextField
                fullWidth
                label="Organization Name"
                value={editableBranding.organizationName || ""}
                onChange={(e) =>
                  handleStringChange(
                    "organizationName",
                    e.target.value
                  )
                }
                placeholder="e.g., Acme Corporation"
                variant="outlined"
              />
            </Grid>

            {/* Logo URL */}
            <Grid
              size={{
                xs: 12,
              }}
            >
              <TextField
                fullWidth
                label="Logo URL"
                value={editableBranding.logo || ""}
                onChange={(e) =>
                  handleStringChange("logo", e.target.value)
                }
                placeholder="https://example.com/logo.png"
                variant="outlined"
              />
            </Grid>

            {/* Banner URL */}
            <Grid
              size={{
                xs: 12,
              }}
            >
              <TextField
                fullWidth
                label="Banner URL"
                value={editableBranding.banner || ""}
                onChange={(e) =>
                  handleStringChange(
                    "banner",
                    e.target.value
                  )
                }
                placeholder="https://example.com/banner.png"
                variant="outlined"
              />
            </Grid>

            {/* Title Alignment */}
            <Grid
              size={{
                xs: 12,
              }}
            >
              <TextField
                fullWidth
                label="Title Alignment"
                select
                value={
                  editableBranding.titleAlignment || "left"
                }
                onChange={(e) =>
                  handleStringChange(
                    "titleAlignment",
                    e.target.value
                  )
                }
                variant="outlined"
              >
                <MenuItem value="left">Left</MenuItem>
                <MenuItem value="center">Center</MenuItem>
                <MenuItem value="right">Right</MenuItem>
              </TextField>
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
              {saving ? "Saving..." : "Save Branding"}
            </Button>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}