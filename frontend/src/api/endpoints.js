import client from './client.js';

/** One function per backend endpoint, grouped by area. Each returns the response body. */
const data = (promise) => promise.then((res) => res.data);

export const authApi = {
  login: (email, password) => data(client.post('/auth/login', { email, password })),
  registerStudent: (body) => data(client.post('/auth/register/student', body)),
  registerCompany: (body) => data(client.post('/auth/register/company', body)),
  me: () => data(client.get('/auth/me')),
  changePassword: (currentPassword, newPassword) =>
    data(client.put('/auth/password', { currentPassword, newPassword })),
};

export const publicApi = {
  stats: () => data(client.get('/public/stats')),
};

export const jobsApi = {
  search: ({ q, type, sort, page, size } = {}) =>
    data(client.get('/jobs', { params: { q: q || undefined, type: type || undefined, sort, page, size } })),
  get: (jobId) => data(client.get(`/jobs/${jobId}`)),
  create: (body) => data(client.post('/jobs', body)),
  update: (jobId, body) => data(client.put(`/jobs/${jobId}`, body)),
  setActive: (jobId, active) => data(client.patch(`/jobs/${jobId}/status`, { active })),
  remove: (jobId) => data(client.delete(`/jobs/${jobId}`)),
};

export const studentApi = {
  profile: () => data(client.get('/students/me')),
  update: (body) => data(client.put('/students/me', body)),
  applications: () => data(client.get('/students/me/applications')),
  recommendations: (limit = 6) => data(client.get('/students/me/recommendations', { params: { limit } })),
  uploadCv: (file) => {
    const form = new FormData();
    form.append('file', file);
    return data(client.post('/students/me/cv', form));
  },
  downloadCv: () => client.get('/students/me/cv', { responseType: 'blob' }),
  deleteCv: () => data(client.delete('/students/me/cv')),
  cvSkills: () => data(client.get('/students/me/cv/skills')),
  savedJobs: () => data(client.get('/students/me/saved-jobs')),
  saveJob: (jobId) => data(client.put(`/students/me/saved-jobs/${jobId}`)),
  unsaveJob: (jobId) => data(client.delete(`/students/me/saved-jobs/${jobId}`)),
  interviews: () => data(client.get('/students/me/interviews')),
};

export const interviewsApi = {
  propose: (applicationId, body) => data(client.post(`/applications/${applicationId}/interview`, body)),
  confirm: (interviewId, slotId) => data(client.post(`/interviews/${interviewId}/confirm`, { slotId })),
  cancel: (interviewId) => data(client.post(`/interviews/${interviewId}/cancel`)),
  calendar: (interviewId) => client.get(`/interviews/${interviewId}/calendar`, { responseType: 'blob' }),
};

export const companyApi = {
  profile: () => data(client.get('/companies/me')),
  update: (body) => data(client.put('/companies/me', body)),
  jobs: () => data(client.get('/companies/me/jobs')),
  applications: ({ status, jobId } = {}) =>
    data(client.get('/companies/me/applications', { params: { status: status || undefined, jobId: jobId || undefined } })),
  interviews: () => data(client.get('/companies/me/interviews')),
};

export const applicationsApi = {
  apply: (jobId, coverLetter) => data(client.post('/applications', { jobId, coverLetter })),
  withdraw: (applicationId) => data(client.delete(`/applications/${applicationId}`)),
  setStatus: (applicationId, status) => data(client.patch(`/applications/${applicationId}/status`, { status })),
  downloadCv: (applicationId) => client.get(`/applications/${applicationId}/cv`, { responseType: 'blob' }),
};

export const notificationsApi = {
  list: () => data(client.get('/notifications')),
  markRead: (id) => data(client.patch(`/notifications/${id}/read`)),
  markAllRead: () => data(client.post('/notifications/read-all')),
};

export const adminApi = {
  stats: () => data(client.get('/admin/stats')),
  users: ({ role, search, page = 0, size = 10 } = {}) =>
    data(client.get('/admin/users', { params: { role: role || undefined, search: search || undefined, page, size } })),
  setUserActive: (userId, active) => data(client.patch(`/admin/users/${userId}/status`, { active })),
  companies: (verified) => data(client.get('/admin/companies', { params: { verified } })),
  setVerified: (companyId, verified) => data(client.patch(`/admin/companies/${companyId}/verification`, { verified })),
  activity: ({ action, actor, page = 0, size = 20 } = {}) =>
    data(client.get('/admin/activity', { params: { action: action || undefined, actor: actor || undefined, page, size } })),
};
