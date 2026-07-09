import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import AdminLayout from "../layouts/AdminLayout";
import DashboardPage from "../pages/Dashboard/DashboardPage";
import FormsPage from "../pages/Forms/FormsPage";
import PreviewPage from "../pages/Preview/PreviewPage";
import SettingsPage from "../pages/Settings/SettingsPage";

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AdminLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />

          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/forms" element={<FormsPage />} />
          <Route path="/preview" element={<PreviewPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}