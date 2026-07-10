import AddIcon from "@mui/icons-material/Add";
import SaveIcon from "@mui/icons-material/Save";
import {
  Button,
  Card,
  CardContent,
  CircularProgress,
  Stack,
} from "@mui/material";

interface Props {
  onAddField?: () => void;
  onSave?: () => void;
  loading?: boolean;
}

export default function BuilderToolbar({
  onAddField,
  onSave,
  loading = false,
}: Props) {
  return (
    <Card elevation={0}>
      <CardContent>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
        >
          <Stack direction="row" spacing={2}>
            {onAddField && (
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={onAddField}
              >
                Add Field
              </Button>
            )}
          </Stack>

          {onSave && (
            <Button
              variant="outlined"
              startIcon={
                loading ? (
                  <CircularProgress size={20} />
                ) : (
                  <SaveIcon />
                )
              }
              onClick={onSave}
              disabled={loading}
            >
              Save Form
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}