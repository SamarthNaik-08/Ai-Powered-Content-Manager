import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, LoginPayload, RegisterPayload, RoleName, PermissionName } from '@/types/auth';
import { authService } from '@/services/authService';
import { tokenStorage } from '@/services/apiClient';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (role: RoleName) => boolean;
  hasPermission: (permission: PermissionName) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const savedUser = tokenStorage.getUser();
    if (savedUser && tokenStorage.getAccessToken()) {
      setUser(savedUser);
    }
    setIsLoading(false);
  }, []);

  const login = async (payload: LoginPayload) => {
    const authData = await authService.login(payload);
    setUser(authData.user);
  };

  const register = async (payload: RegisterPayload) => {
    const authData = await authService.register(payload);
    setUser(authData.user);
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  const hasRole = (role: RoleName): boolean => {
    return !!user?.roles?.includes(role);
  };

  const hasPermission = (permission: PermissionName): boolean => {
    return !!user?.permissions?.includes(permission);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        hasRole,
        hasPermission,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
