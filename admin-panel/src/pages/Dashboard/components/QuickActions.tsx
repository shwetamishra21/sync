import {
  Button,
  Card,
  CardContent,
  Stack,
  Typography,
} from "@mui/material";

export default function QuickActions() {
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
          mb={3}
        >
          Quick Actions
        </Typography>

        <Stack spacing={2}>

          <Button
            variant="contained"
            fullWidth
          >
            Create Form
          </Button>

          <Button
            variant="outlined"
            fullWidth
          >
            Preview Form
          </Button>

          <Button
            variant="outlined"
            fullWidth
          >
            Theme Editor
          </Button>

          <Button
            variant="outlined"
            fullWidth
          >
            Publish
          </Button>

        </Stack>

      </CardContent>
    </Card>
  );
}