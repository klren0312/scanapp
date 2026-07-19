import axios from 'axios';
import { useAuthStore } from '../store/auth';

const base = import.meta.env.VITE_API_BASE || 'http://localhost:3001/api';

const http = axios.create({ baseURL: base });

http.interceptors.request.use((cfg) => {
  const token = useAuthStore().token;
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

http.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response && err.response.status === 401 && !err.config.url.endsWith('/auth/login')) {
      useAuthStore().logout();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const api = {
  login: (username, password) => http.post('/auth/login', { username, password }).then((r) => r.data),
  getStats: () => http.get('/stats/overview').then((r) => r.data),
  getMapPoints: (params) => http.get('/map/points', { params }).then((r) => r.data),
  getDevices: (params) => http.get('/devices', { params }).then((r) => r.data),
  getDevice: (id) => http.get(`/devices/${id}`).then((r) => r.data),
  getTrajectory: (id) => http.get(`/devices/${id}/trajectory`).then((r) => r.data),
  getSightings: (id, params) => http.get(`/devices/${id}/sightings`, { params }).then((r) => r.data),
  recompute: () => http.post('/admin/recompute').then((r) => r.data),
};
