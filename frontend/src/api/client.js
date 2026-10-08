import axios from 'axios';

export const AUTH_STORAGE_KEY = 'edulink.auth';

/**
 * Shared HTTP client. In development Vite proxies /api to the Spring Boot backend (see vite.config.js).
 * Every request automatically carries the JWT of the logged-in user.
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 15000,
});

client.interceptors.request.use((config) => {
  const token = readStoredAuth()?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 on an authenticated request means the session expired or the account was disabled
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const hadToken = Boolean(error.config?.headers?.Authorization);
    if (error.response?.status === 401 && hadToken) {
      window.dispatchEvent(new CustomEvent('edulink:session-expired'));
    }
    return Promise.reject(error);
  },
);

export function readStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY));
  } catch {
    return null;
  }
}

/** The user-friendly message from an API error (the backend always sends { message, fieldErrors }). */
export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (error?.code === 'ECONNABORTED') {
    return 'The server took too long to respond. Please try again.';
  }
  if (!error?.response) {
    return 'Cannot reach the EduLink server. Is the backend running on port 8080?';
  }
  return error.response.data?.message || fallback;
}

/** Field-level validation errors, e.g. { email: 'Enter a valid email address' }. */
export function fieldErrors(error) {
  return error?.response?.data?.fieldErrors || {};
}

export default client;
