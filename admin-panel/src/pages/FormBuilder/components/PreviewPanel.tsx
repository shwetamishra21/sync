import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Divider,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import type { FormDetail, FormField } from "../../../types/form";

interface Props {
  form: FormDetail;
}

export default function PreviewPanel({ form }: Props) {
  if (!form) {
    return (
      <Card>
        <CardContent>
          <Typography color="text.secondary">
            No form to preview
          </Typography>
        </CardContent>
      </Card>
    );
  }

  // Extract theme configuration with defaults
  const theme = {
    primaryColor: form.theme?.primaryColor || "#1976d2",
    backgroundColor: form.theme?.backgroundColor || "#ffffff",
    surfaceColor: form.theme?.surfaceColor || "#f5f5f5",
    textColor: form.theme?.textColor || "#000000",
    buttonColor: form.theme?.buttonColor || "#1976d2",
    buttonTextColor: form.theme?.buttonTextColor || "#ffffff",
    cornerRadius: form.theme?.cornerRadius || 4,
  };

  // Extract layout configuration with defaults
  const layout = {
    columns: form.layout?.columns || 1,
    spacing: form.layout?.spacing || 16,
    cardPadding: form.layout?.cardPadding || 24,
    sectionSpacing: form.layout?.sectionSpacing || 32,
    showDividers: form.layout?.showDividers ?? true,
  };

  // Extract branding configuration
  const branding = {
    organizationName: form.branding?.organizationName || "",
    logo: form.branding?.logo || "",
    banner: form.branding?.banner || "",
    titleAlignment: form.branding?.titleAlignment || "left",
  };

  // Render field based on type
  const renderField = (field: FormField) => {
    const baseProps = {
      fullWidth: true,
      disabled: true,
      label: field.name,
      placeholder: field.placeholder,
      helperText: field.help_text,
      variant:
        (form.layout?.fieldStyle as
          | "outlined"
          | "filled"
          | "standard") ?? "outlined",
      required: field.required,
      sx: {
        "& .MuiOutlinedInput-root": {
          borderRadius: `${theme.cornerRadius}px`,
        },
      },
    };

    switch (field.type) {
      case "email":
        return (
          <TextField
            key={field.id}
            {...baseProps}
            type="email"
          />
        );

      case "number":
        return (
          <TextField
            key={field.id}
            {...baseProps}
            type="number"
          />
        );

      case "textarea":
        return (
          <TextField
            key={field.id}
            {...baseProps}
            multiline
            rows={4}
          />
        );

      case "date":
        return (
          <TextField
            key={field.id}
            {...baseProps}
            type="date"
            slotProps={{
              inputLabel: {
                shrink: true,
              },
            }}
          />
        );

      case "dropdown":
        return (
          <TextField
            key={field.id}
            {...baseProps}
            select
            defaultValue={field.options?.[0] || ""}
          >
            {field.options?.map((option: string) => (
              <MenuItem key={option} value={option}>
                {option}
              </MenuItem>
            ))}
          </TextField>
        );

      case "media":
        return (
          <Button
            key={field.id}
            fullWidth
            disabled
            variant="outlined"
            sx={{
              borderRadius: `${theme.cornerRadius}px`,
              color: theme.textColor,
              borderColor: theme.textColor,
            }}
          >
            Upload {field.name}
          </Button>
        );

      case "gps":
        return (
          <Button
            key={field.id}
            fullWidth
            disabled
            variant="outlined"
            sx={{
              borderRadius: `${theme.cornerRadius}px`,
              color: theme.textColor,
              borderColor: theme.textColor,
            }}
          >
            Capture Location
          </Button>
        );

      case "text":
      default:
        return (
          <TextField
            key={field.id}
            {...baseProps}
            type="text"
          />
        );
    }
  };

  return (
    <Card
      sx={{
        backgroundColor: theme.backgroundColor,
        borderRadius: `${theme.cornerRadius}px`,
      }}
    >
      <CardHeader
        title="Live Preview"
        titleTypographyProps={{ variant: "h6" }}
      />

      <Divider />

      <CardContent
        sx={{
          backgroundColor: theme.surfaceColor,
          p: `${layout.cardPadding}px`,
        }}
      >
        <Stack
          spacing={layout.sectionSpacing / 8}
          sx={{
            color: theme.textColor,
          }}
        >
          {/* Branding Section */}
          <Box
            sx={{
              textAlign: branding.titleAlignment as any,
              mb: layout.sectionSpacing / 8,
            }}
          >
            {/* Banner */}
            {branding.banner && (
              <Box
                component="img"
                src={branding.banner}
                sx={{
                  width: "100%",
                  height: 120,
                  objectFit: "cover",
                  borderRadius: `${theme.cornerRadius}px`,
                  mb: 2,
                }}
                alt="Banner"
                onError={(e) => {
                  (e.target as HTMLImageElement).style.display =
                    "none";
                }}
              />
            )}

            {/* Logo */}
            {branding.logo && (
              <Box
                component="img"
                src={branding.logo}
                sx={{
                  height: 60,
                  mb: 1,
                  objectFit: "contain",
                }}
                alt="Logo"
                onError={(e) => {
                  (e.target as HTMLImageElement).style.display =
                    "none";
                }}
              />
            )}

            {/* Organization Name */}
            {branding.organizationName && (
              <Typography
                variant="h5"
                sx={{
                  color: theme.textColor,
                  fontWeight: 600,
                  mb: 1,
                }}
              >
                {branding.organizationName}
              </Typography>
            )}

            {/* Form Title */}
            {form.name && (
              <Typography
                variant="h4"
                sx={{
                  color: theme.primaryColor,
                  fontWeight: 700,
                }}
              >
                {form.name}
              </Typography>
            )}

            {/* Form Description */}
            {form.description && (
              <Typography
                variant="body2"
                sx={{
                  color: theme.textColor,
                  opacity: 0.8,
                  mt: 1,
                }}
              >
                {form.description}
              </Typography>
            )}
          </Box>

          {/* Divider */}
          {layout.showDividers && (
            <Divider sx={{ my: 2 }} />
          )}

          {/* Fields Section */}
          {form.fields && form.fields.length > 0 ? (
            <Grid
              container
              spacing={layout.spacing}
            >
              {form.fields.map((field) => (
                <Grid
                  key={field.id}
                  size={{
                    xs: 12,
                    sm:
                      layout.columns === 1
                        ? 12
                        : (12 / layout.columns) as
                            | 1
                            | 2
                            | 3
                            | 4
                            | 6
                            | 12,
                  }}
                >
                  {renderField(field)}
                </Grid>
              ))}
            </Grid>
          ) : (
            <Typography
              color="text.secondary"
              sx={{ py: 2 }}
            >
              No fields added yet
            </Typography>
          )}

          {/* Divider before submit */}
          {form.fields && form.fields.length > 0 && layout.showDividers && (
            <Divider sx={{ my: 2 }} />
          )}

          {/* Submit Button */}
          {form.fields && form.fields.length > 0 && (
            <Button
              fullWidth
              disabled
              variant="contained"
              sx={{
                backgroundColor: theme.buttonColor,
                color: theme.buttonTextColor,
                borderRadius: `${theme.cornerRadius}px`,
                py: 1.5,
                fontSize: "1rem",
                fontWeight: 600,
                "&.Mui-disabled": {
                  backgroundColor: theme.buttonColor,
                  color: theme.buttonTextColor,
                  opacity: 0.7,
                },
              }}
            >
              Submit
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}