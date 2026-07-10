import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";


import AdminLayout from "../layouts/AdminLayout";

import DashboardPage from "../pages/Dashboard/DashboardPage";
import FormsPage from "../pages/Forms/FormsPage";
import FormBuilderPage from "../pages/FormBuilder/FormBuilderPage";
import PreviewPage from "../pages/Preview/PreviewPage";
import SettingsPage from "../pages/Settings/SettingsPage";
import LoginPage from "../pages/Login/LoginPage";

import type { ReactNode } from "react";

function RequireAuth({
  children,
}: {
  children: ReactNode;
}) {
  const token = localStorage.getItem("token");

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function RedirectIfAuthenticated({
  children,
}: {
  children:  ReactNode;
}) {
  const token = localStorage.getItem("token");

  if (token) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <RedirectIfAuthenticated>
              <LoginPage />
            </RedirectIfAuthenticated>
          }
        />

        <Route
          element={
            <RequireAuth>
              <AdminLayout />
            </RequireAuth>
          }
        >
          <Route
            index
            element={
              <Navigate
                to="/dashboard"
                replace
              />
            }
          />

          <Route
            path="/dashboard"
            element={<DashboardPage />}
          />

          <Route
            path="/forms"
            element={<FormsPage />}
          />

          <Route
            path="/builder/:formId"
            element={<FormBuilderPage />}
          />

          <Route
            path="/preview"
            element={<PreviewPage />}
          />

          <Route
            path="/settings"
            element={<SettingsPage />}
          />
        </Route>

        <Route
          path="*"
          element={<Navigate to="/dashboard" replace />}
        />
      </Routes>
    </BrowserRouter>
  );
}