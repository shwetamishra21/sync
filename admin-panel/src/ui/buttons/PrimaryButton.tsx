import { Button } from "@mui/material";
import type { ButtonProps } from "@mui/material";

export default function PrimaryButton(
  props: ButtonProps
) {
  return (
    <Button
      variant="contained"
      disableElevation
      sx={{
        borderRadius: 3,
        textTransform: "none",
        px: 3,
        py: 1.2,
      }}
      {...props}
    />
  );
}