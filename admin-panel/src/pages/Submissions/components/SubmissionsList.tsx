import DeleteIcon from "@mui/icons-material/Delete";
import VisibilityIcon from "@mui/icons-material/Visibility";
import {
  Card,
  Chip,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from "@mui/material";
import type { FormSummary } from "../../../types/form";
import type { Submission } from "../../../types/submission";

interface Props {
  submissions: Submission[];
  forms: FormSummary[];
  onView: (submission: Submission) => void;
  onDelete: (submission: Submission) => void;
}

const statusColor: Record<
  string,
  "success" | "warning" | "error" | "default"
> = {
  SYNCED: "success",
  PENDING: "warning",
  SYNCING: "warning",
  FAILED: "error",
};

const formatDateTime = (value: string) =>
  new Date(value).toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

export default function SubmissionsList({
  submissions,
  forms,
  onView,
  onDelete,
}: Props) {
  const formNameById = new Map(
    forms.map((form) => [form.id, form.name])
  );

  if (submissions.length === 0) {
    return (
      <Card
        elevation={0}
        sx={{
          borderRadius: 3,
          border: "1px solid",
          borderColor: "divider",
          py: 8,
          textAlign: "center",
        }}
      >
        <Typography variant="h6">
          No Submissions Found
        </Typography>

        <Typography
          color="text.secondary"
          sx={{ mt: 1 }}
        >
          Submissions from the Android app will appear here once
          received.
        </Typography>
      </Card>
    );
  }

  return (
    <Card
      elevation={0}
      sx={{
        borderRadius: 3,
        border: "1px solid",
        borderColor: "divider",
        overflow: "hidden",
      }}
    >
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Form</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Submitted</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {submissions.map((submission) => (
              <TableRow
                key={submission.id}
                hover
              >
                <TableCell>#{submission.id}</TableCell>

                <TableCell>
                  {formNameById.get(submission.form_id) ??
                    submission.form_id}
                </TableCell>

                <TableCell>
                  <Chip
                    size="small"
                    label={submission.sync_status}
                    color={
                      statusColor[submission.sync_status] ??
                      "default"
                    }
                  />
                </TableCell>

                <TableCell>
                  {formatDateTime(submission.created_at)}
                </TableCell>

                <TableCell align="right">
                  <Stack
                    direction="row"
                    spacing={0.5}
                    sx={{ justifyContent: "flex-end" }}
                  >
                    <Tooltip title="View details">
                      <IconButton
                        size="small"
                        onClick={() => onView(submission)}
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>

                    <Tooltip title="Delete submission">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => onDelete(submission)}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Card>
  );
}
