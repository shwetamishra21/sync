import {
  Alert,
  CircularProgress,
  Snackbar,
  Stack,
} from "@mui/material";
import { useMemo, useState } from "react";
import PageHeader from "../../ui/page/PageHeader";
import SubmissionsToolbar from "./components/SubmissionsToolbar";
import SubmissionsList from "./components/SubmissionsList";
import SubmissionDetailDialog from "./components/SubmissionDetailDialog";
import DeleteSubmissionDialog from "./components/DeleteSubmissionDialog";
import { useSubmissions } from "../../hooks/useSubmissions";
import { useForms } from "../../hooks/useForms";
import { deleteSubmission } from "../../api/submissionApi";
import type { Submission } from "../../types/submission";

export default function SubmissionsPage() {
  const [search, setSearch] = useState("");
  const [formFilter, setFormFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");

  const {
    submissions,
    loading,
    error,
    refreshSubmissions,
  } = useSubmissions({
    form_id: formFilter === "all" ? undefined : formFilter,
    status: statusFilter === "all" ? undefined : statusFilter,
    limit: 200,
  });

  const { forms } = useForms();

  const [viewingSubmission, setViewingSubmission] =
    useState<Submission | null>(null);

  const [deletingSubmission, setDeletingSubmission] =
    useState<Submission | null>(null);

  const [deleting, setDeleting] = useState(false);

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success" as "success" | "error",
  });

  const filteredSubmissions = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return submissions;

    return submissions.filter((submission) =>
      String(submission.id).includes(query)
    );
  }, [submissions, search]);

  async function handleDelete() {
    if (!deletingSubmission) return;

    try {
      setDeleting(true);

      await deleteSubmission(deletingSubmission.id);

      await refreshSubmissions();

      setDeletingSubmission(null);

      setSnackbar({
        open: true,
        severity: "success",
        message: "Submission deleted successfully.",
      });
    } catch (err) {
      console.error(err);

      setSnackbar({
        open: true,
        severity: "error",
        message: "Failed to delete submission.",
      });
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return (
      <Stack
        sx={{
          display: "flex",
          justifyContent: "center",
          mt: 10,
        }}
      >
        <CircularProgress />
      </Stack>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        Failed to load submissions.
      </Alert>
    );
  }

  return (
    <>
      <Stack spacing={3}>
        <PageHeader
          title="Submissions"
          subtitle="Review form data submitted from the Android application."
        />

        <SubmissionsToolbar
          search={search}
          onSearchChange={setSearch}
          forms={forms}
          formFilter={formFilter}
          onFormFilterChange={setFormFilter}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
        />

        <SubmissionsList
          submissions={filteredSubmissions}
          forms={forms}
          onView={setViewingSubmission}
          onDelete={setDeletingSubmission}
        />
      </Stack>

      <SubmissionDetailDialog
        submission={viewingSubmission}
        formName={
          forms.find((form) => form.id === viewingSubmission?.form_id)
            ?.name
        }
        onClose={() => setViewingSubmission(null)}
      />

      <DeleteSubmissionDialog
        open={!!deletingSubmission}
        submissionId={deletingSubmission?.id}
        loading={deleting}
        onClose={() => setDeletingSubmission(null)}
        onDelete={handleDelete}
      />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() =>
          setSnackbar((prev) => ({
            ...prev,
            open: false,
          }))
        }
      >
        <Alert severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
