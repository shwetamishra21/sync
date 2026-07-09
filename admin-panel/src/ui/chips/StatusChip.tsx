import { Chip } from "@mui/material";

interface Props {
  label: string;
  color?:
    | "success"
    | "warning"
    | "error"
    | "info"
    | "default";
}

export default function StatusChip({
  label,
  color = "info",
}: Props) {
  return (
    <Chip
      size="small"
      label={label}
      color={color}
    />
  );
}