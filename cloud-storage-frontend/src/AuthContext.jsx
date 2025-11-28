import { createContext, useContext, useEffect, useState } from "react";
import { login as apiLogin, registerUser, getUserInfo } from "./api";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // авто-вход по токену
  useEffect(() => {
    async function load() {
      if (!token) {
        setLoading(false);
        return;
      }

      const info = await getUserInfo(token);
      if (info) {
        setUser(info);
      } else {
        setToken("");
        localStorage.removeItem("token");
      }
      setLoading(false);
    }
    load();
  }, []);

  async function login(email, password) {
    const t = await apiLogin(email, password);
    if (!t) return false;

    setToken(t);
    localStorage.setItem("token", t);

    const info = await getUserInfo(t);
    setUser(info);

    return true;
  }

  async function register(email, password, username) {
    const t = await registerUser(email, password, username);
    if (!t) return false;

    setToken(t);
    localStorage.setItem("token", t);

    const info = await getUserInfo(t);
    setUser(info);

    return true;
  }

  function logout() {
    setToken("");
    setUser(null);
    localStorage.removeItem("token");
  }

  return (
      <AuthContext.Provider value={{ user, token, login, register, logout, loading }}>
        {children}
      </AuthContext.Provider>
  );
}
