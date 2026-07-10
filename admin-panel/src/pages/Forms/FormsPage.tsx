// src/pages/Forms/FormsPage.tsx - COMPLETE
import {
  Alert,
  CircularProgress,
  Stack,
  Snackbar,
} from "@mui/material";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageHeader from "../../ui/page/PageHeader";
import FormsToolbar from "./components/FormsToolbar";
import FormsList from "./components/FormsList";
import FormDialog from "./components/FormDialog";
import DeleteFormDialog from "./components/DeleteFormDialog";
import { useForms } from "../../hooks/useForms";
import {
  createForm,
  updateForm,
  deleteForm,
  toggleFormStatus,
} from "../../api/formApi";
import type {
  CreateFormRequest,
  FormSummary,
  UpdateFormRequest,
} from "../../types/form";

export default function FormsPage() {
  const navigate = useNavigate();

  const { forms, loading, error, refreshForms } = useForms();

  const [search, setSearch] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [editingForm, setEditingForm] = useState<FormSummary | null>(null);
  const [deletingForm, setDeletingForm] = useState<FormSummary | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success" as "success" | "error",
  });

  const filteredForms = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return forms;

    return forms.filter((form) =>
      form.name.toLowerCase().includes(query) ||
      form.description.toLowerCase().includes(query) ||
      form.id.toLowerCase().includes(query)
    );
  }, [forms, search]);

  async function handleCreate(values: CreateFormRequest) {
    try {
      setCreating(true);

      await createForm(values);

      await refreshForms();

      setEditingForm(null);

      setDialogOpen(false);

      setSnackbar({
        open: true,
        message: "Form created successfully.",
        severity: "success",
      });
    } catch (error) {
      console.error(error);

      setSnackbar({
        open: true,
        message: "Failed to create form.",
        severity: "error",
      });
    } finally {
      setCreating(false);
    }
  }

  async function handleUpdate(values: CreateFormRequest) {
    if (!editingForm) return;

    try {
      setCreating(true);

      const updateData: UpdateFormRequest = {
        name: values.name,
        description: values.description,
        version: values.version,
      };

      await updateForm(editingForm.id, updateData);

      await refreshForms();

      setEditingForm(null);

      setDialogOpen(false);

      setSnackbar({
        open: true,
        severity: "success",
        message: "Form updated successfully.",
      });
    } catch (error) {
      console.error(error);

      setSnackbar({
        open: true,
        severity: "error",
        message: "Failed to update form.",
      });
    } finally {
      setCreating(false);
    }
  }

  async function handleDelete() {
    if (!deletingForm) return;

    try {
      setDeleting(true);

      await deleteForm(deletingForm.id);

      await refreshForms();

      setDeletingForm(null);

      setSnackbar({
        open: true,
        severity: "success",
        message: "Form deleted successfully.",
      });
    } catch (error) {
      console.error(error);

      setSnackbar({
        open: true,
        severity: "error",
        message: "Failed to delete form.",
      });
    } finally {
      setDeleting(false);
    }
  }

  async function handleToggleActive(form: FormSummary) {
    const nextState = !(form.is_active ?? true);

    try {
      await toggleFormStatus(form.id, nextState);

      await refreshForms();

      setSnackbar({
        open: true,
        severity: "success",
        message: `Form ${
          nextState
            ? "activated"
            : "deactivated"
        } successfully.`,
      });
    } catch (error) {
      console.error(error);

      setSnackbar({
        open: true,
        severity: "error",
        message: "Failed to update form status.",
      });
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
        Failed to load forms.
      </Alert>
    );
  }

  return (
    <>
      <Stack spacing={3}>
        <PageHeader
          title="Forms"
          subtitle="Manage all dynamic forms available to the Android application."
        />

        <FormsToolbar
          search={search}
          onSearchChange={setSearch}
          onCreate={() => {
            setEditingForm(null);
            setDialogOpen(true);
          }}
        />

        <FormsList
          forms={filteredForms}
          onBuilder={(form) =>
            navigate(`/builder/${form.id}`)
          }
          onEdit={(form) => {
            setEditingForm(form);
            setDialogOpen(true);
          }}
          onDelete={(form) => {
            setDeletingForm(form);
          }}
          onToggleActive={handleToggleActive}
        />
      </Stack>

      <FormDialog
        open={dialogOpen}
        loading={creating}
        title={editingForm ? "Edit Form" : "Create Form"}
        submitLabel={editingForm ? "Save" : "Create"}
        initialValues={
          editingForm
            ? {
                id: editingForm.id,
                name: editingForm.name,
                description: editingForm.description,
                version: editingForm.version,
              }
            : undefined
        }
        onClose={() => {
          setDialogOpen(false);
          setEditingForm(null);
        }}
        onSubmit={editingForm ? handleUpdate : handleCreate}
      />

      <DeleteFormDialog
        open={!!deletingForm}
        formName={deletingForm?.name ?? ""}
        loading={deleting}
        onClose={() => {
          setDeletingForm(null);
        }}
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