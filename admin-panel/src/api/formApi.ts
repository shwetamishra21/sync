import axios from "./axios";
import type {
  CreateFieldRequest,
  CreateFormRequest,
  FormDetail,
  FormDetailResponse,
  FormField,
  FormSummary,
  FormsResponse,
  UpdateFieldRequest,
  UpdateFormRequest,
} from "../types/form";

export async function getForms(): Promise<FormsResponse> {
  const response = await axios.get<FormsResponse>(
    "/admin/forms"
  );
  return response.data;
}

export async function createForm(
  data: CreateFormRequest
): Promise<FormSummary> {
  const response = await axios.post<{
    message: string;
    form: FormSummary;
  }>("/admin/forms", data);
  return response.data.form;
}

export async function updateForm(
  formId: string,
  data: UpdateFormRequest
): Promise<FormSummary> {
  const response = await axios.put<{
    message: string;
    form: FormSummary;
  }>(`/admin/forms/${formId}`, data);
  return response.data.form;
}

export async function deleteForm(
  formId: string
): Promise<void> {
  await axios.delete(`/admin/forms/${formId}`);
}

export async function toggleFormStatus(
  formId: string,
  isActive: boolean
): Promise<FormSummary> {
  const response = await axios.put<{
    message: string;
    form: FormSummary;
  }>(`/admin/forms/${formId}`, {
    is_active: isActive,
  });
  return response.data.form;
}

/**
 * Get form detail from PUBLIC endpoint
 * Returns only ACTIVE forms
 * Used by: Mobile/web form submission
 */
export async function getFormDetail(
  formId: string
): Promise<FormDetail> {
  const response =
    await axios.get<FormDetailResponse>(
      `/forms/${formId}`
    );
  return response.data.form;
}

/**
 * Get form detail from ADMIN endpoint
 * Returns ALL forms (active and inactive)
 * Requires authentication token
 * Used by: Form Builder to allow editing inactive forms
 */
export async function getAdminFormDetail(
  formId: string
): Promise<FormDetail> {
  const response =
    await axios.get<FormDetailResponse>(
      `/admin/forms/${formId}`
    );
  return response.data.form;
}

export async function updateField(
  formId: string,
  fieldDbId: number,
  data: UpdateFieldRequest
): Promise<FormField> {
  const response = await axios.put<{
    message: string;
    field: FormField;
  }>(
    `/admin/forms/${formId}/fields/${fieldDbId}`,
    data
  );
  return response.data.field;
}

export async function createField(
  formId: string,
  data: CreateFieldRequest
): Promise<FormField> {
  const response = await axios.post<{
    message: string;
    field: FormField;
  }>(
    `/admin/forms/${formId}/fields`,
    data
  );
  return response.data.field;
}

export async function deleteField(
  formId: string,
  fieldDbId: number
): Promise<void> {
  await axios.delete(
    `/admin/forms/${formId}/fields/${fieldDbId}`
  );
}

export async function updateFormUi(
  formId: string,
  data: Partial<UpdateFormRequest>
): Promise<FormSummary> {
  const response = await axios.put<{
    status: string;
    message: string;
    form: FormSummary;
  }>(
    `/admin/forms/${formId}`,
    data
  );
  return response.data.form;
}