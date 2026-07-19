import { defineStore } from 'pinia';

const KEY = 'scanapp_admin_token';

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem(KEY) || '' }),
  getters: {
    isAuthenticated: (s) => !!s.token,
  },
  actions: {
    setToken(t) { this.token = t; localStorage.setItem(KEY, t); },
    logout() { this.token = ''; localStorage.removeItem(KEY); },
  },
});
