import React, { createContext, useContext, useState, useEffect, useRef } from "react";
import { loginRequest, registerRequest, getUserInfo } from "./api.js";

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem("user");
    return storedUser ? JSON.parse(storedUser) : null;
  });
  const [token, setToken] = useState(() => {
    return localStorage.getItem("accessToken") || null;
  });
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const isInitialMount = useRef(true);

  const updateUser = (updates) => {
    setUser(prev => {
      const updated = { ...prev, ...updates };
      localStorage.setItem("user", JSON.stringify(updated));
      return updated;
    });
  };

  const loadUserData = async (authToken) => {
    try {
      console.log("AuthContext: Loading user info with token...");
      const userInfo = await getUserInfo(authToken);
      console.log("AuthContext: Raw user info from server:", userInfo);

      if (userInfo) {
        console.log("AuthContext: All user info fields:", Object.keys(userInfo));
        console.log("AuthContext: username field:", userInfo.username);
        console.log("AuthContext: name field:", userInfo.name);
        console.log("AuthContext: email field:", userInfo.email);
      }

      const username = userInfo.Name.split('@')[0] || 'User';

      const email = userInfo.email || userInfo.Email || '';

      const formattedUser = {
        username: username,
        name: username,
        email: email,
        ...userInfo
      };

      console.log("AuthContext: Formatted user object:", formattedUser);

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
      const t = await loginRequest(email, password);
      console.log('AuthContext: Token received, length:', t?.length);

      if (!t) {
        throw new Error("No token received from server");
      }

      localStorage.setItem("accessToken", t);
      setToken(t);

      const userData = await loadUserData(t);

      if (!userData) {
        throw new Error("Failed to load user data");
      }

      console.log('AuthContext: Login completed successfully');
      return true;

    } catch (error) {
      console.error('AuthContext: Login failed:', error.message);

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

      localStorage.setItem("accessToken", t);
      setToken(t);

      const tempUser = {
        username: username,
        name: username,
        email: email
      };

      localStorage.setItem("user", JSON.stringify(tempUser));
      setUser(tempUser);
      setIsAuthenticated(true);

      setTimeout(async () => {
        try {
          const serverUserData = await getUserInfo(t);
          console.log("AuthContext: Server data after registration:", serverUserData);

          let finalUsername = username;

          if (serverUserData?.name && serverUserData.name !== "User" && serverUserData.name !== "") {
            finalUsername = serverUserData.name;
          }

          const finalUser = {
            username: finalUsername,
            name: finalUsername,
            email: email,
            ...serverUserData
          };

          console.log("AuthContext: Final user after registration:", finalUser);
          localStorage.setItem("user", JSON.stringify(finalUser));
          setUser(finalUser);
        } catch (loadError) {
          console.warn("AuthContext: Could not fetch user info after registration:", loadError);
        }
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