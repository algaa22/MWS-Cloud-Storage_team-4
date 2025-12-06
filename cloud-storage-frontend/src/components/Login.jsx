// src/components/Login.jsx
import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate, Link } from "react-router-dom";


export default function Login() {
  const { login, isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  // Если уже авторизован - редирект
  useEffect(() => {
    console.log("Login component: isAuthenticated =", isAuthenticated);
    if (isAuthenticated) {
      console.log("Login: Already authenticated, redirecting to /files");
      navigate("/files");
    }
  }, [isAuthenticated, navigate]);

  async function handleLogin() {
    if (!email || !password) {
      setError("Введите email и пароль");
      return;
    }

    if (isLoggingIn) return;

    setError("");
    setIsLoggingIn(true);
    console.log("Login: Starting login process...");

    try {
      const ok = await login(email, password);
      console.log("Login: login() returned", ok);

      if (!ok) {
        setError("Неверный email или пароль");
      } else {
        console.log("Login: Success! User should be redirected via useEffect");
        // Редирект произойдет через useEffect при изменении isAuthenticated
      }
    } catch (err) {
      console.error("Login: Unexpected error", err);
      setError("Произошла ошибка при входе");
    } finally {
      setIsLoggingIn(false);
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === "Enter") {
      handleLogin();
    }
  };

  return (
      <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 flex items-center justify-center p-4">
        <div className="bg-white/20 backdrop-blur-xl shadow-2xl rounded-3xl p-10 w-full max-w-md text-white">
          <h2 className="text-3xl font-bold text-center mb-6">Вход</h2>

          <div className="space-y-4">
            <input
                type="email"
                placeholder="Email"
                className="p-3 rounded-xl bg-white/30 w-full text-white placeholder:text-white/70 focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyPress={handleKeyPress}
                disabled={isLoggingIn || loading}
            />

            <input
                type="password"
                placeholder="Пароль"
                className="p-3 rounded-xl bg-white/30 w-full text-white placeholder:text-white/70 focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyPress={handleKeyPress}
                disabled={isLoggingIn || loading}
            />

            {error && (
                <div className="bg-red-500/20 border border-red-500 text-red-300 px-4 py-2 rounded-xl text-center">
                  {error}
                </div>
            )}

            <button
                onClick={handleLogin}
                disabled={isLoggingIn || loading}
                className={`w-full py-3 rounded-xl font-medium transition-all ${
                    isLoggingIn
                        ? "bg-gray-400 cursor-not-allowed"
                        : "bg-white text-blue-600 hover:bg-gray-100"
                }`}
            >
              {isLoggingIn ? (
                  <span className="flex items-center justify-center">
                <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-blue-600 mr-2"></div>
                Вход...
              </span>
              ) : (
                  "Войти"
              )}
            </button>

            <p className="text-center mt-4 text-white/80">
              Нет аккаунта?{" "}
              <Link
                  to="/register"
                  className="underline hover:text-white transition-colors"
              >
                Создать
              </Link>
            </p>
          </div>
        </div>
      </div>
  );
}