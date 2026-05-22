import http from './http';
import type { AuthResponse, LoginPayload, RegisterPayload } from '../types/auth';

export async function register(payload: RegisterPayload) {
  const { data } = await http.post<AuthResponse>('/auth/register', payload);
  return data;
}

export async function login(payload: LoginPayload) {
  const { data } = await http.post<AuthResponse>('/auth/login', payload);
  return data;
}

export async function logout() {
  const { data } = await http.post<AuthResponse>('/auth/logout');
  return data;
}

export async function getCurrentUser() {
  const { data } = await http.get<AuthResponse>('/auth/me');
  return data;
}
