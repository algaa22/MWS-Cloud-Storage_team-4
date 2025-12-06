// src/AuthContext.jsx
import React, { createContext, useContext, useState, useEffect, useRef } from "react";
import { loginRequest, registerRequest, getUserInfo } from "./api.js";

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem("user");
    return storedUser ? JSON.parse(storedUser) : null;
  });
  const [token, setToken] = useState(() => {
    return localStorage.getItem("accessToken") || null; // Изменено с "token" на "accessToken"
  });
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const isInitialMount = useRef(true);

  // Функция для обновления данных пользователя
  const updateUser = (updates) => {
    setUser(prev => {
      const updated = { ...prev, ...updates };
      localStorage.setItem("user", JSON.stringify(updated));
      return updated;
    });
  };

  // Функция для загрузки информации о пользователе
  const loadUserData = async (authToken) => {
    try {
      console.log("AuthContext: Loading user info with token...");
      const userInfo = await getUserInfo(authToken);
      console.log("AuthContext: User loaded successfully:", userInfo);

      // Извлекаем username из разных возможных полей
      const username = userInfo.username || userInfo.Username ||
          userInfo.email?.split('@')[0] || 'User';

      const email = userInfo.email || userInfo.Email || '';

      const formattedUser = {
        username,
        email,
        ...userInfo
      };

      localStorage.setItem("user", JSON.stringify(formattedUser));
      setUser(formattedUser);
      setIsAuthenticated(true);
      return formattedUser;

    } catch (error) {
      console.error('AuthContext: Failed to load user info:', error.message);
      return null;
    }
  };

  useEffect(() => {
    console.log("AuthContext: useEffect triggered, token:",
        token ? `${token.substring(0, 20)}...` : "null");

    async function loadUser() {
      if (!token) {
        console.log("AuthContext: No token, setting unauthenticated");
        setUser(null);
        setIsAuthenticated(false);
        setLoading(false);
        return;
      }

      try {
        await loadUserData(token);
      } catch (error) {
        console.error('AuthContext: Failed to load user info:', error.message);

        // Очищаем при ошибках авторизации
        if (error.message.includes("401") || error.message.includes("403") ||
            error.message.includes("TOKEN_INVALID") || error.message.includes("expired")) {
          console.log("AuthContext: Token invalid, clearing all data");
          localStorage.removeItem("accessToken");
          localStorage.removeItem("user");
          setToken(null);
          setUser(null);
          setIsAuthenticated(false);
        }
      } finally {
        setLoading(false);
      }
    }

    loadUser();
  }, [token]);

  async function login(email, password) {
    console.log('AuthContext: login called with email:', email);

    try {
      // Получаем токен
      const t = await loginRequest(email, password);
      console.log('AuthContext: Token received, length:', t?.length);

      if (!t) {
        throw new Error("No token received from server");
      }

      // Сохраняем токен как accessToken
      localStorage.setItem("accessToken", t);
      setToken(t);

      // Загружаем информацию о пользователе
      const userData = await loadUserData(t);

      if (!userData) {
        throw new Error("Failed to load user data");
      }

      console.log('AuthContext: Login completed successfully');
      return true;

    } catch (error) {
      console.error('AuthContext: Login failed:', error.message);

      // Очищаем при ошибке
      localStorage.removeItem("accessToken");
      localStorage.removeItem("user");
      setToken(null);
      setUser(null);
      setIsAuthenticated(false);

      return false;
    }
  }

  async function register(email, password, username) {
    console.log('AuthContext: register called', { email, username });

    try {
      const t = await registerRequest(email, password, username);
      console.log('AuthContext: Token received from register');

      if (!t) {
        throw new Error("No token received");
      }

      // Сохраняем токен как accessToken
      localStorage.setItem("accessToken", t);
      setToken(t);

      // Создаем временного пользователя до загрузки данных с сервера
      const tempUser = {
        username: username,
        email: email
      };

      localStorage.setItem("user", JSON.stringify(tempUser));
      setUser(tempUser);
      setIsAuthenticated(true);

      // Загружаем полные данные с сервера
      setTimeout(() => {
        loadUserData(t);
      }, 100);

      console.log('AuthContext: Registration successful');
      return true;

    } catch (error) {
      console.error('AuthContext: Registration failed:', error.message);
      localStorage.removeItem("accessToken");
      localStorage.removeItem("user");
      setToken(null);
      setUser(null);
      setIsAuthenticated(false);
      return false;
    }
  }

  function logout() {
    console.log('AuthContext: logout called');
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
    setIsAuthenticated(false);
  }

  const value = {
    user,
    token,
    login,
    register,
    logout,
    loading,
    isAuthenticated,
    updateUser,
    loadUserData
  };

  if (isInitialMount.current) {
    console.log("AuthContext: Initial mount");
    isInitialMount.current = false;
  }

  return (
      <AuthContext.Provider value={value}>
        {children}
      </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
};