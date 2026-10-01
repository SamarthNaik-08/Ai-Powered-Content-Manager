import { apiClient, tokenStorage } from './apiClient';
import { ApiResponse } from '@/types/api';
import { AuthResponse, LoginPayload, RegisterPayload, User } from '@/types/auth';

export const authService = {
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', payload);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Login failed');
    }
    tokenStorage.setAuth(response.data.data);
    return response.data.data;
  },

  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', payload);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Registration failed');
    }
    tokenStorage.setAuth(response.data.data);
    return response.data.data;
  },

  async logout(): Promise<void> {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (refreshToken) {
        await apiClient.post('/auth/logout', { refreshToken });
      }
    } catch {
      // Ignore network errors on logout
    } finally {
      tokenStorage.clearAuth();
    }
  },

  getCurrentUser(): User | null {
    return tokenStorage.getUser();
  },

  isAuthenticated(): boolean {
    return !!tokenStorage.getAccessToken();
  },
};
