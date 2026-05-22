export interface AuthUser {
  id: number;
  username: string;
  nickname: string;
}

export interface AuthResponse {
  authenticated: boolean;
  user: AuthUser | null;
}

export interface RegisterPayload {
  username: string;
  password: string;
  confirmPassword: string;
  nickname: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}
