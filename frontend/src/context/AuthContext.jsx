import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AUTH_STORAGE_KEY, readStoredAuth } from '../api/client.js';
import { authApi } from '../api/endpoints.js';

const AuthContext = createContext(null);

/** Where each role lands after logging in. */
export const HOME_BY_ROLE = {
  STUDENT: '/student',
  COMPANY: '/company/jobs',
  ADMIN: '/admin',
};

function loadInitialAuth() {
  const stored = readStoredAuth();
  if (!stored?.token || !stored?.expiresAt || new Date(stored.expiresAt) <= new Date()) {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
  return stored;
}

/**
 * Holds the logged-in user and their JWT.
 * The token is only a key for the API: every permission is checked again by the backend,
 * so editing localStorage cannot grant extra access (unlike the old frontend).
 */
export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadInitialAuth);
  const [sessionExpired, setSessionExpired] = useState(false);
  const navigate = useNavigate();

  const save = useCallback((next) => {
    if (next) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(next));
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY);
    }
    setAuth(next);
  }, []);

  const startSession = useCallback((response) => {
    save({ token: response.token, expiresAt: response.expiresAt, user: response.user });
    setSessionExpired(false);
    return response.user;
  }, [save]);

  const login = useCallback(async (email, password) => startSession(await authApi.login(email, password)), [startSession]);
  const registerStudent = useCallback(async (body) => startSession(await authApi.registerStudent(body)), [startSession]);
  const registerCompany = useCallback(async (body) => startSession(await authApi.registerCompany(body)), [startSession]);

  const logout = useCallback(() => {
    save(null);
    navigate('/login');
  }, [save, navigate]);

  /** Re-reads the user from the server (e.g. after a name change or verification). */
  const refreshUser = useCallback(async () => {
    const user = await authApi.me();
    setAuth((current) => {
      if (!current) return current;
      const next = { ...current, user };
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(next));
      return next;
    });
    return user;
  }, []);

  // The API client fires this when the server rejects our token
  useEffect(() => {
    const onExpired = () => {
      save(null);
      setSessionExpired(true);
      navigate('/login');
    };
    window.addEventListener('edulink:session-expired', onExpired);
    return () => window.removeEventListener('edulink:session-expired', onExpired);
  }, [save, navigate]);

  // Log out automatically when the token's lifetime ends
  useEffect(() => {
    if (!auth?.expiresAt) return undefined;
    const ms = new Date(auth.expiresAt) - new Date();
    const timer = setTimeout(() => window.dispatchEvent(new CustomEvent('edulink:session-expired')), Math.max(ms, 0));
    return () => clearTimeout(timer);
  }, [auth?.expiresAt]);

  const value = useMemo(() => ({
    user: auth?.user ?? null,
    isAuthenticated: Boolean(auth?.token),
    sessionExpired,
    login,
    registerStudent,
    registerCompany,
    logout,
    refreshUser,
  }), [auth, sessionExpired, login, registerStudent, registerCompany, logout, refreshUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
}
