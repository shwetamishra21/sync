// src/types/form.ts

// ======================================================
// Configuration Types
// ======================================================

export interface ValidationConfig {
  min?: number;
  max?: number;

  minLength?: number;
  maxLength?: number;

  regex?: string;

  allowedExtensions?: string[];
  maxImageSizeMB?: number;
}

export interface VisibleIfConfig {
  field: string;
  equals: string;
}

export interface EnabledIfConfig {
  field: string;
  equals: string;
}

export interface ThemeConfig {
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  surfaceColor: string;
  buttonColor: string;
  buttonTextColor: string;
  textColor: string;
  cornerRadius: number;
}

export interface LayoutConfig {
  columns: number;
  spacing: number;
  fieldStyle: string;
  labelPosition: string;
  cardPadding: number;
  sectionSpacing: number;
  showDividers: boolean;
}

export interface BrandingConfig {
  logo: string;
  banner: string;
  organizationName: string;
  titleAlignment: string;
}

// ======================================================
// Forms
// ======================================================

export interface FormSummary {
  id: string;
  name: string;
  description: string;
  version: string;
  created_at: string;
  field_count: number;
  is_active: boolean;
}

export interface FormField {
  db_id: number;

  id: string;

  name: string;

  type: string;

  required: boolean;

  placeholder?: string;

  help_text?: string;

  field_order: number;

  options?: string[];

  validation?: ValidationConfig;

  visible_if?: VisibleIfConfig;

  enabled_if?: EnabledIfConfig;

  default_value?: string;
}

export interface FormDetail extends FormSummary {
  theme: ThemeConfig;

  layout: LayoutConfig;

  branding: BrandingConfig;

  fields: FormField[];
}

// ======================================================
// Requests
// ======================================================

export interface CreateFormRequest {
  id: string;
  name: string;
  description: string;
  version: string;
}

export interface UpdateFormRequest {
  name?: string;
  description?: string;
  version?: string;
  is_active?: boolean;
  theme?: ThemeConfig;
  layout?: LayoutConfig;
  branding?: BrandingConfig;
}

export interface UpdateFieldRequest {
  name?: string;

  type?: string;

  required?: boolean;

  placeholder?: string;

  help_text?: string;

  field_order?: number;

  options?: string[];

  validation?: ValidationConfig;

  visible_if?: VisibleIfConfig;

  enabled_if?: EnabledIfConfig;
}

export interface CreateFieldRequest {
  field_id: string;

  name: string;

  type: string;

  required?: boolean;

  placeholder?: string;

  field_order?: number;

  help_text?: string;

  options?: string[];

  validation?: ValidationConfig;

  visible_if?: VisibleIfConfig;

  enabled_if?: EnabledIfConfig;
}

// ======================================================
// Responses
// ======================================================

export interface FormsResponse {
  status: string;
  forms: FormSummary[];
  count: number;
}

export interface FormDetailResponse {
  status: string;
  form: FormDetail;
}

export interface SubmissionResponse {
  status: string;
  submission_id: string;
  message: string;
  submitted_at: number;
  is_duplicate?: boolean;
}