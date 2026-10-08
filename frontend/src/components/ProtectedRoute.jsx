import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { HOME_BY_ROLE, useAuth } from '../context/AuthContext.jsx';

/**
 * Only renders child routes for a logged-in user with the given role.
 * This is a convenience for the UI — the backend enforces the same rule on every API call.
 */
export default function ProtectedRoute({ role }) {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (role && user.role !== role) {
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/'} replace />;
  }
  return <Outlet />;
}
