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
    return localStorage.getItem("token") || null;
  });
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Используем ref для предотвращения бесконечных рендеров
  const isInitialMount = useRef(true);

  useEffect(() => {
    console.log("AuthContext: useEffect triggered, token changed:",
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
        console.log("AuthContext: Loading user info with token...");
        const userInfo = await getUserInfo(token);
        console.log("AuthContext: User loaded successfully");

        // Сохраняем пользователя в localStorage и state
        localStorage.setItem("user", JSON.stringify(userInfo));
        setUser(userInfo);
        setIsAuthenticated(true);
      } catch (error) {
        console.error('AuthContext: Failed to load user info:', error.message);

        // Только для определенных ошибок очищаем
        if (error.message.includes("401") || error.message.includes("403") ||
            error.message.includes("TOKEN_INVALID") || error.message.includes("expired")) {
          console.log("AuthContext: Token invalid, clearing all data");
          localStorage.removeItem("token");
          localStorage.removeItem("user");
          setToken(null);
          setUser(null);
          setIsAuthenticated(false);
        }
        // Для других ошибок (сеть и т.д.) не очищаем - может быть временная проблема
      } finally {
        setLoading(false);
      }
    }

    // Загружаем пользователя только при изменении token
    loadUser();
  }, [token]); // Зависимость только от token

  async function login(email, password) {
    console.log('AuthContext: login called with email:', email);

    try {
      // Получаем токен
      const t = await loginRequest(email, password);
      console.log('AuthContext: Token received, length:', t?.length);

      if (!t) {
        throw new Error("No token received from server");
      }

      // Сохраняем токен
      localStorage.setItem("token", t);
      setToken(t);

      // Получаем информацию о пользователе
      console.log("AuthContext: Fetching user info...");
      const userInfo = await getUserInfo(t);
      console.log("AuthContext: User info received");

      // Сохраняем пользователя
      localStorage.setItem("user", JSON.stringify(userInfo));
      setUser(userInfo);
      setIsAuthenticated(true);

      console.log('AuthContext: Login completed successfully');
      return true;

    } catch (error) {
      console.error('AuthContext: Login failed:', error.message);

      // Очищаем при ошибке
      localStorage.removeItem("token");
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

      // Сохраняем токен
      localStorage.setItem("token", t);
      setToken(t);

      // Получаем пользователя
      const userInfo = await getUserInfo(t);

      // Сохраняем пользователя
      localStorage.setItem("user", JSON.stringify(userInfo));
      setUser(userInfo);
      setIsAuthenticated(true);

      console.log('AuthContext: Registration successful');
      return true;

    } catch (error) {
      console.error('AuthContext: Registration failed:', error.message);
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      setToken(null);
      setUser(null);
      setIsAuthenticated(false);
      return false;
    }
  }

  function logout() {
    console.log('AuthContext: logout called');
    localStorage.removeItem("token");
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
    isAuthenticated
  };

  // Логируем только важные изменения
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