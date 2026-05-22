import { defineStore } from 'pinia';
import { getCurrentUser, login, logout, register } from '../api/auth';
import type { AuthUser, LoginPayload, RegisterPayload } from '../types/auth';
import { parseApiError } from '../utils/error';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as AuthUser | null,
    authenticated: false,
    loading: false,
    initialized: false,
    error: ''
  }),
  actions: {
    async fetchMe() {
      this.loading = true;
      this.error = '';
      try {
        const response = await getCurrentUser();
        this.authenticated = response.authenticated;
        this.user = response.user;
      } catch (error) {
        this.authenticated = false;
        this.user = null;
        this.error = parseApiError(error);
      } finally {
        this.loading = false;
        this.initialized = true;
      }
    },
    async register(payload: RegisterPayload) {
      this.loading = true;
      this.error = '';
      try {
        return await register(payload);
      } catch (error) {
        this.error = parseApiError(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async login(payload: LoginPayload) {
      this.loading = true;
      this.error = '';
      try {
        const response = await login(payload);
        this.authenticated = response.authenticated;
        this.user = response.user;
        return response;
      } catch (error) {
        this.error = parseApiError(error);
        throw error;
      } finally {
        this.loading = false;
        this.initialized = true;
      }
    },
    async logout() {
      this.loading = true;
      this.error = '';
      try {
        await logout();
      } finally {
        this.user = null;
        this.authenticated = false;
        this.loading = false;
        this.initialized = true;
      }
    }
  }
});
