import {
  Box,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Button,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableRow,
  Typography,
} from "@mui/material";
import type { Submission } from "../../../types/submission";

interface Props {
  submission: Submission | null;
  formName?: string;
  onClose: () => void;
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

const formatDateTime = (value: string | null) =>
  value
    ? new Date(value).toLocaleString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "—";

export default function SubmissionDetailDialog({
  submission,
  formName,
  onClose,
}: Props) {
  const open = !!submission;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="sm"
    >
      <DialogTitle>
        Submission #{submission?.id}
      </DialogTitle>

      <DialogContent dividers>
        {submission && (
          <Stack spacing={3}>
            <Box>
              <Typography
                variant="subtitle2"
                color="text.secondary"
              >
                Form
              </Typography>
              <Typography>
                {formName ?? submission.form_id}
              </Typography>
            </Box>

            <Stack
              direction="row"
              spacing={4}
            >
              <Box>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                >
                  Status
                </Typography>
                <Chip
                  size="small"
                  label={submission.sync_status}
                  color={
                    statusColor[submission.sync_status] ??
                    "default"
                  }
                  sx={{ mt: 0.5 }}
                />
              </Box>

              <Box>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                >
                  Submitted
                </Typography>
                <Typography>
                  {formatDateTime(submission.created_at)}
                </Typography>
              </Box>

              <Box>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                >
                  Synced
                </Typography>
                <Typography>
                  {formatDateTime(submission.synced_at)}
                </Typography>
              </Box>
            </Stack>

            {(submission.gps_latitude != null ||
              submission.gps_longitude != null) && (
              <Box>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                >
                  GPS Location
                </Typography>
                <Typography>
                  {submission.gps_latitude},{" "}
                  {submission.gps_longitude}
                </Typography>
              </Box>
            )}

            <Divider />

            <Box>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                sx={{ mb: 1 }}
              >
                Form Data
              </Typography>

              {Object.keys(submission.form_data ?? {}).length === 0 ? (
                <Typography color="text.secondary">
                  No field data recorded.
                </Typography>
              ) : (
                <Table size="small">
                  <TableBody>
                    {Object.entries(submission.form_data ?? {}).map(
                      ([key, value]) => (
                        <TableRow key={key}>
                          <TableCell
                            sx={{
                              fontWeight: 600,
                              width: "40%",
                              verticalAlign: "top",
                            }}
                          >
                            {key}
                          </TableCell>
                          <TableCell
                            sx={{
                              wordBreak: "break-word",
                            }}
                          >
                            {typeof value === "object"
                              ? JSON.stringify(value)
                              : String(value)}
                          </TableCell>
                        </TableRow>
                      )
                    )}
                  </TableBody>
                </Table>
              )}
            </Box>
          </Stack>
        )}
      </DialogContent>

      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
