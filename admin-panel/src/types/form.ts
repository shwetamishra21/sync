export interface FormSummary {
    id: string;
    name: string;
    description: string;
    version: string;
    created_at: string;
    field_count: number;

    is_active?: boolean;
}

export interface FormsResponse {
    status: string;
    forms: FormSummary[];
    count: number;
}