import {
  Alert,
  CircularProgress,
  Grid,
  Stack,
} from "@mui/material";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import {
  updateFormUi,
  createField,
  updateField,
  deleteField,
} from "../../api/formApi";

import type {
  ThemeConfig,
  LayoutConfig,
  BrandingConfig,
  CreateFieldRequest,
  FormField,
  UpdateFieldRequest,
} from "../../types/form";

import ThemeEditor from "./components/ThemeEditor";
import LayoutEditor from "./components/LayoutEditor";
import BrandingEditor from "./components/BrandingEditor";
import PageHeader from "../../ui/page/PageHeader";
import BuilderToolbar from "./components/BuilderToolbar";
import FieldList from "./components/FieldList";
import PropertyPanel from "./components/PropertyPanel";
import PreviewPanel from "./components/PreviewPanel";
import AddFieldDialog from "./components/AddFieldDialog";
import DeleteFieldDialog from "./components/DeleteFieldDialog";
import { useFormDetail } from "../../hooks/useFormDetail";

export default function FormBuilderPage() {
  const {
    formId = "",
  } = useParams();

  // ✅ Enable admin mode to load inactive forms
  const {
    form,
    loading,
    error,
    refreshForm,
  } = useFormDetail(formId, true);

  const [selectedFieldId, setSelectedFieldId] =
    useState<string>();

  const [orderedFields, setOrderedFields] =
    useState<FormField[]>([]);

  const [addDialogOpen, setAddDialogOpen] =
    useState(false);

  const [creating, setCreating] =
    useState(false);

  const [deleteDialogOpen, setDeleteDialogOpen] =
    useState(false);

  const [deleting, setDeleting] =
    useState(false);

  const [savingOrder, setSavingOrder] =
    useState(false);

  // Keep ordered fields in sync with form
  useEffect(() => {
    if (form) {
      setOrderedFields(
        [...form.fields].sort(
          (a, b) => a.field_order - b.field_order
        )
      );
    }
  }, [form]);

  const selectedField = useMemo(() => {
    if (!form) return null;

    return (
      form.fields.find(
        (field) => field.id === selectedFieldId
      ) ?? null
    );
  }, [form, selectedFieldId]);

  const moveFieldUp = useCallback(
    (index: number) => {
      if (index === 0) return;

      const updated = [...orderedFields];

      // Swap fields
      [updated[index - 1], updated[index]] =
        [updated[index], updated[index - 1]];

      // Update field order values
      updated.forEach(
        (field, i) => (field.field_order = i + 1)
      );

      setOrderedFields(updated);
    },
    [orderedFields]
  );

  const moveFieldDown = useCallback(
    (index: number) => {
      if (index === orderedFields.length - 1)
        return;

      const updated = [...orderedFields];

      // Swap fields
      [updated[index], updated[index + 1]] =
        [updated[index + 1], updated[index]];

      // Update field order values
      updated.forEach(
        (field, i) => (field.field_order = i + 1)
      );

      setOrderedFields(updated);
    },
    [orderedFields]
  );

  const handleSaveOrder = useCallback(
    async () => {
      try {
        setSavingOrder(true);

        await Promise.all(
          orderedFields.map((field) =>
            updateField(formId, field.db_id, {
              field_order: field.field_order,
            })
          )
        );

        await refreshForm();
      } catch (err) {
        console.error(err);
      } finally {
        setSavingOrder(false);
      }
    },
    [orderedFields, formId, refreshForm]
  );

  const handleSaveField = useCallback(
    async (updatedField: FormField) => {
      try {
        const request: UpdateFieldRequest = {
          name: updatedField.name,
          type: updatedField.type,
          required: updatedField.required,
          placeholder: updatedField.placeholder,
          help_text: updatedField.help_text,
          field_order: updatedField.field_order,
          options: updatedField.options,
          validation: updatedField.validation,
          visible_if: updatedField.visible_if,
          enabled_if: updatedField.enabled_if,
        };

        await updateField(
          formId,
          updatedField.db_id,
          request
        );

        await refreshForm();

        setSelectedFieldId(updatedField.id);
      } catch (err) {
        console.error(err);
      }
    },
    [formId, refreshForm]
  );

  const handleCreateField = useCallback(
    async (newField: CreateFieldRequest) => {
      if (!form) {
        return;
      }

      try {
        setCreating(true);

        const nextFieldOrder =
          Math.max(
            0,
            ...form.fields.map(
              (field) => field.field_order
            )
          ) + 1;

        const createdField = await createField(
          formId,
          {
            ...newField,
            field_order: nextFieldOrder,
          }
        );

        await refreshForm();

        setAddDialogOpen(false);

        setSelectedFieldId(createdField.id);
      } catch (err) {
        console.error(err);
      } finally {
        setCreating(false);
      }
    },
    [form, formId, refreshForm]
  );

  const handleDeleteField = useCallback(
    async () => {
      if (!selectedField) {
        return;
      }

      try {
        setDeleting(true);

        await deleteField(formId, selectedField.db_id);

        await refreshForm();

        setDeleteDialogOpen(false);

        setSelectedFieldId(undefined);
      } catch (err) {
        console.error(err);
      } finally {
        setDeleting(false);
      }
    },
    [formId, selectedField, refreshForm]
  );

  const handleSaveTheme = useCallback(
    async (theme: ThemeConfig) => {
      try {
        await updateFormUi(formId, {
          theme,
        });

        await refreshForm();
      } catch (err) {
        console.error(err);
      }
    },
    [formId, refreshForm]
  );

  const handleSaveLayout = useCallback(
    async (layout: LayoutConfig) => {
      try {
        await updateFormUi(formId, {
          layout,
        });

        await refreshForm();
      } catch (err) {
        console.error(err);
      }
    },
    [formId, refreshForm]
  );

  const handleSaveBranding = useCallback(
    async (branding: BrandingConfig) => {
      try {
        await updateFormUi(formId, {
          branding,
        });

        await refreshForm();
      } catch (err) {
        console.error(err);
      }
    },
    [formId, refreshForm]
  );

  if (loading) {
    return <CircularProgress />;
  }

  if (error || !form) {
    return (
      <Alert severity="error">
        Failed to load form.
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        title={form.name}
        subtitle={form.description}
      />

      <BuilderToolbar
        onAddField={() =>
          setAddDialogOpen(true)
        }
        onSave={handleSaveOrder}
        loading={savingOrder}
      />

      <Grid
        container
        spacing={3}
      >
        <Grid
          size={{
            xs: 12,
            md: 4,
          }}
        >
          <FieldList
            fields={orderedFields}
            selectedFieldId={selectedFieldId}
            onSelect={(field) =>
              setSelectedFieldId(field.id)
            }
            onMoveUp={moveFieldUp}
            onMoveDown={moveFieldDown}
          />
        </Grid>

        <Grid
          size={{
            xs: 12,
            md: 8,
          }}
        >
          <PropertyPanel
            field={selectedField}
            onSave={handleSaveField}
            onDelete={
              selectedField
                ? () => setDeleteDialogOpen(true)
                : undefined
            }
          />
        </Grid>
      </Grid>

      <AddFieldDialog
        open={addDialogOpen}
        loading={creating}
        onClose={() =>
          setAddDialogOpen(false)
        }
        onCreate={handleCreateField}
      />

      <DeleteFieldDialog
        open={deleteDialogOpen}
        fieldName={selectedField?.name ?? ""}
        loading={deleting}
        onClose={() =>
          setDeleteDialogOpen(false)
        }
        onDelete={handleDeleteField}
      />

      <ThemeEditor
        theme={form.theme}
        onSave={handleSaveTheme}
      />

      <LayoutEditor
        layout={form.layout}
        onSave={handleSaveLayout}
      />

      <BrandingEditor
        branding={form.branding}
        onSave={handleSaveBranding}
      />

      <PreviewPanel form={form} />
    </Stack>
  );
}