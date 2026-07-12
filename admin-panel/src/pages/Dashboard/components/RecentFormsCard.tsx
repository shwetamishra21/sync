import {
  Card,
  CardContent,
  Chip,
  Divider,
  List,
  ListItemButton,
  ListItemText,
  Typography,
} from "@mui/material";
import type { FormSummary } from "../../../types/form";

interface Props {
  forms: FormSummary[];
  onSelect: (formId: string) => void;
}

const formatDate = (date: string) =>
  new Date(date).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

export default function RecentFormsCard({ forms, onSelect }: Props) {
  const recentForms = [...forms]
    .sort(
      (a, b) =>
        new Date(b.created_at).getTime() -
        new Date(a.created_at).getTime()
    )
    .slice(0, 5);

  return (
    <Card
      elevation={0}
      sx={{
        border: "1px solid #E5E7EB",
        borderRadius: 3,
      }}
    >
      <CardContent>
        <Typography
          variant="h6"
          sx={{
            mb: 2,
          }}
        >
          Recent Forms
        </Typography>

        {recentForms.length === 0 ? (
          <Typography color="text.secondary">
            No forms have been created yet.
          </Typography>
        ) : (
          <List sx={{ p: 0 }}>
            {recentForms.map((form, index) => (
              <div key={form.id}>
                <ListItemButton
                  disableGutters
                  onClick={() => onSelect(form.id)}
                  sx={{
                    px: 1,
                    borderRadius: 2,
                  }}
                >
                  <ListItemText
                    primary={form.name}
                    secondary={`${form.field_count} field${
                      form.field_count === 1 ? "" : "s"
                    } • Created ${formatDate(form.created_at)}`}
                  />

                  <Chip
                    size="small"
                    label={
                      (form.is_active ?? true) ? "Active" : "Inactive"
                    }
                    color={
                      (form.is_active ?? true) ? "success" : "default"
                    }
                  />
                </ListItemButton>

                {index !== recentForms.length - 1 && <Divider />}
              </div>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
}
