// src/pages/Forms/components/FormActionsMenu.tsx - UPDATED
import MoreVertIcon from "@mui/icons-material/MoreVert";
import {
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from "@mui/material";
import { useState } from "react";
import ConstructionIcon from "@mui/icons-material/Construction";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import FileCopyIcon from "@mui/icons-material/FileCopy";
import ToggleOnIcon from "@mui/icons-material/ToggleOn";
import ToggleOffIcon from "@mui/icons-material/ToggleOff";

import type { FormSummary } from "../../../types/form";

interface Props {
  form: FormSummary;
  onBuilder?: (form: FormSummary) => void;
  onEdit: (form: FormSummary) => void;
  onDelete?: (form: FormSummary) => void;
  onDuplicate?: (form: FormSummary) => void;
  onToggleActive?: (form: FormSummary) => void;
}

export default function FormActionsMenu({
  form,
  onBuilder,
  onEdit,
  onDelete,
  onDuplicate,
  onToggleActive,
}: Props) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  const handleOpen = (event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleBuilder = () => {
    handleClose();

    if (onBuilder) {
      onBuilder(form);
    }
  };

  const handleEdit = () => {
    handleClose();
    onEdit(form);
  };

  const handleDelete = () => {
    handleClose();
    if (onDelete) {
      onDelete(form);
    }
  };

  const handleDuplicate = () => {
    handleClose();
    if (onDuplicate) {
      onDuplicate(form);
    }
  };

  const handleToggle = () => {
    handleClose();
    if (onToggleActive) {
      onToggleActive(form);
    }
  };

  return (
    <>
      <IconButton
        size="small"
        onClick={handleOpen}
      >
        <MoreVertIcon />
      </IconButton>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "right",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "right",
        }}
      >
        <MenuItem onClick={handleBuilder}>
          <ListItemIcon>
            <ConstructionIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>
            Open Builder
          </ListItemText>
        </MenuItem>

        <MenuItem onClick={handleEdit}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edit</ListItemText>
        </MenuItem>

        <MenuItem onClick={handleToggle}>
          <ListItemIcon>
            {(form.is_active ?? true) ? (
              <ToggleOffIcon fontSize="small" />
            ) : (
              <ToggleOnIcon fontSize="small" />
            )}
          </ListItemIcon>
          <ListItemText>
            {(form.is_active ?? true) ? "Deactivate" : "Activate"}
          </ListItemText>
        </MenuItem>

        <MenuItem onClick={handleDuplicate} disabled>
          <ListItemIcon>
            <FileCopyIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Duplicate</ListItemText>
        </MenuItem>

        <MenuItem onClick={handleDelete}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Delete</ListItemText>
        </MenuItem>
      </Menu>
    </>
  );
}