import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AppNavbar from './components/AppNavbar.jsx';
import Footer from './components/Footer.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import { Loading } from './components/ui.jsx';
import LandingPage from './pages/LandingPage.jsx';
import LoginPage from './pages/LoginPage.jsx';

// Pages are code-split: each role only downloads the screens it can use
// (e.g. the Chart.js-heavy admin dashboard never loads for students).
const RegisterPage = lazy(() => import('./pages/RegisterPage.jsx'));
const JobsPage = lazy(() => import('./pages/JobsPage.jsx'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage.jsx'));
const StudentDashboard = lazy(() => import('./pages/student/StudentDashboard.jsx'));
const MyApplicationsPage = lazy(() => import('./pages/student/MyApplicationsPage.jsx'));
const StudentProfilePage = lazy(() => import('./pages/student/StudentProfilePage.jsx'));
const SavedJobsPage = lazy(() => import('./pages/student/SavedJobsPage.jsx'));
const CompanyJobsPage = lazy(() => import('./pages/company/CompanyJobsPage.jsx'));
const ApplicantsPage = lazy(() => import('./pages/company/ApplicantsPage.jsx'));
const CompanyInterviewsPage = lazy(() => import('./pages/company/CompanyInterviewsPage.jsx'));
const CompanyProfilePage = lazy(() => import('./pages/company/CompanyProfilePage.jsx'));
const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard.jsx'));
const AdminCompaniesPage = lazy(() => import('./pages/admin/AdminCompaniesPage.jsx'));
const AdminUsersPage = lazy(() => import('./pages/admin/AdminUsersPage.jsx'));
const AdminActivityPage = lazy(() => import('./pages/admin/AdminActivityPage.jsx'));

export default function App() {
  return (
    <div className="app-shell">
      <AppNavbar />
      <main className="app-main">
        <Suspense fallback={<Loading />}>
          <Routes>
            {/* Public */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/jobs" element={<JobsPage />} />

            {/* Student */}
            <Route element={<ProtectedRoute role="STUDENT" />}>
              <Route path="/student" element={<StudentDashboard />} />
              <Route path="/student/applications" element={<MyApplicationsPage />} />
              <Route path="/student/profile" element={<StudentProfilePage />} />
              <Route path="/student/saved" element={<SavedJobsPage />} />
            </Route>

            {/* Company */}
            <Route element={<ProtectedRoute role="COMPANY" />}>
              <Route path="/company" element={<Navigate to="/company/jobs" replace />} />
              <Route path="/company/jobs" element={<CompanyJobsPage />} />
              <Route path="/company/applications" element={<ApplicantsPage />} />
              <Route path="/company/interviews" element={<CompanyInterviewsPage />} />
              <Route path="/company/profile" element={<CompanyProfilePage />} />
            </Route>

            {/* Admin */}
            <Route element={<ProtectedRoute role="ADMIN" />}>
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/companies" element={<AdminCompaniesPage />} />
              <Route path="/admin/users" element={<AdminUsersPage />} />
              <Route path="/admin/activity" element={<AdminActivityPage />} />
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </main>
      <Footer />
    </div>
  );
}
