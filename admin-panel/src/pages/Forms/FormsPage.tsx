import { Stack, Alert, CircularProgress, Box } from "@mui/material";

import PageHeader from "../../ui/page/PageHeader";
import FormsToolbar from "./components/FormsToolbar";
import FormsList from "./components/FormsList";

import { useForms } from "../../hooks/useForms";

export default function FormsPage() {
  const { forms, loading, error } = useForms();

  if (loading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          mt: 10,
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        Failed to load forms.
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title="Forms"
        subtitle="Manage all dynamic forms available to the Android application."
      />

      <FormsToolbar />

      <FormsList forms={forms} />
    </Stack>
  );
}