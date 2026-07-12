import {
  Button,
  Card,
  CardContent,
  Stack,
  Typography,
} from "@mui/material";

interface Props {
  onCreateForm: () => void;
  onPreviewForm: () => void;
  onViewSubmissions: () => void;
}

export default function QuickActions({
  onCreateForm,
  onPreviewForm,
  onViewSubmissions,
}: Props) {
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
          sx={{ mb: 3 }}
        >
          Quick Actions
        </Typography>

        <Stack spacing={2}>

          <Button
            variant="contained"
            fullWidth
            onClick={onCreateForm}
          >
            Create Form
          </Button>

          <Button
            variant="outlined"
            fullWidth
            onClick={onPreviewForm}
          >
            Preview Form
          </Button>

          <Button
            variant="outlined"
            fullWidth
            onClick={onViewSubmissions}
          >
            View Submissions
          </Button>

        </Stack>

      </CardContent>
    </Card>
  );
}
