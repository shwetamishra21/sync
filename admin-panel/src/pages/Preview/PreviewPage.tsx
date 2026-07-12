import {
  Alert,
  Card,
  CardContent,
  CircularProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";

import PageHeader from "../../ui/page/PageHeader";
import PreviewPanel from "../FormBuilder/components/PreviewPanel";
import { useForms } from "../../hooks/useForms";
import { useFormDetail } from "../../hooks/useFormDetail";

export default function PreviewPage() {
  const { forms, loading: formsLoading, error: formsError } = useForms();

  const [selectedFormId, setSelectedFormId] = useState("");

  // Admin mode so inactive/draft forms can still be previewed.
  const {
    form,
    loading: formLoading,
    error: formError,
  } = useFormDetail(selectedFormId, true);

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Preview"
        subtitle="See exactly how a form will render on the Android application."
      />

      {formsError && (
        <Alert severity="error">Failed to load forms.</Alert>
      )}

      <TextField
        select
        label="Select a form"
        value={selectedFormId}
        onChange={(e) => setSelectedFormId(e.target.value)}
        disabled={formsLoading}
        sx={{ maxWidth: 360 }}
      >
        {forms.map((f) => (
          <MenuItem
            key={f.id}
            value={f.id}
          >
            {f.name}
          </MenuItem>
        ))}
      </TextField>

      {!selectedFormId && (
        <Card
          elevation={0}
          sx={{
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 3,
            py: 8,
            textAlign: "center",
          }}
        >
          <CardContent>
            <Typography variant="h6">
              Select a form to preview
            </Typography>

            <Typography
              color="text.secondary"
              sx={{ mt: 1 }}
            >
              Choose a form above to see a live rendering of its theme,
              layout, branding, and fields.
            </Typography>
          </CardContent>
        </Card>
      )}

      {selectedFormId && formLoading && (
        <Stack
          sx={{
            display: "flex",
            justifyContent: "center",
            mt: 6,
          }}
        >
          <CircularProgress />
        </Stack>
      )}

      {selectedFormId && !formLoading && (formError || !form) && (
        <Alert severity="error">Failed to load form preview.</Alert>
      )}

      {selectedFormId && !formLoading && form && (
        <PreviewPanel form={form} />
      )}
    </Stack>
  );
}
