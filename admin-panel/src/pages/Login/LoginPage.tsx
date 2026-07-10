import { useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";

import { login } from "../../api/authApi";

export default function LoginPage() {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");

  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState("");

  async function handleLogin() {
    setError("");

    if (!username.trim() || !password.trim()) {
      setError("Username and Password are required.");
      return;
    }

    try {
      setLoading(true);

      const response = await login({
        username,
        password,
      });

      localStorage.setItem("token", response.token);

      localStorage.setItem("username", response.username);

      navigate("/dashboard", {
        replace: true,
      });
    } catch (err) {
      console.error(err);

      setError("Invalid username or password.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        bgcolor: "background.default",
        p: 3,
      }}
    >
      <Card
        sx={{
          width: 420,
        }}
      >
        <CardContent
          sx={{
            p: 4,
          }}
        >
          <Stack spacing={3}>
            <Box>
              <Typography
                variant="h4"
                sx={{
                  mb: 1,
                }}
              >
                JSAC Admin
              </Typography>

              <Typography color="text.secondary">
                Sign in to continue.
              </Typography>
            </Box>

            {error && (
              <Alert severity="error">
                {error}
              </Alert>
            )}

            <TextField
              label="Username"
              fullWidth
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <TextField
              label="Password"
              type="password"
              fullWidth
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  handleLogin();
                }
              }}
            />

            <Button
              variant="contained"
              size="large"
              onClick={handleLogin}
              disabled={loading}
            >
              {loading ? (
                <CircularProgress
                  size={22}
                  color="inherit"
                />
              ) : (
                "Login"
              )}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}