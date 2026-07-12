import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from "@mui/material";

interface Props {
  open: boolean;
  submissionId?: number;
  loading?: boolean;
  onClose: () => void;
  onDelete: () => void;
}

export default function DeleteSubmissionDialog({
  open,
  submissionId,
  loading = false,
  onClose,
  onDelete,
}: Props) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle>Delete Submission</DialogTitle>

      <DialogContent>
        <Typography>
          Are you sure you want to delete submission{" "}
          <strong>#{submissionId}</strong>?
        </Typography>

        <Typography
          color="text.secondary"
          sx={{ mt: 2 }}
        >
          This action cannot be undone.
        </Typography>
      </DialogContent>

      <DialogActions sx={{ p: 3 }}>
        <Button
          onClick={onClose}
          disabled={loading}
        >
          Cancel
        </Button>

        <Button
          color="error"
          variant="contained"
          onClick={onDelete}
          disabled={loading}
        >
          Delete
        </Button>
      </DialogActions>
    </Dialog>
  );
}
