import {
  Card,
  CardContent,
  Divider,
  List,
  ListItem,
  ListItemText,
  Typography,
} from "@mui/material";

const forms = [
  "Resident Registration",
  "Land Survey",
  "Citizen Census",
  "Agriculture Form",
];

export default function RecentFormsCard() {
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
          sx={{
            mb: 2,
          }}
        >
          Recent Forms
        </Typography>

        <List>
          {forms.map((form) => (
            <div key={form}>
              <ListItem disablePadding>
                <ListItemText
                  primary={form}
                  secondary="Updated recently"
                />
              </ListItem>

              <Divider />
            </div>
          ))}
        </List>
      </CardContent>
    </Card>
  );
}