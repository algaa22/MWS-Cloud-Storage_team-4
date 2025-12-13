import React from 'react';
import { useState } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate, Link } from "react-router-dom";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (password !== repeatPassword) {
      setError("Пароли не совпадают");
      return;
    }

    if (password.length < 6) {
      setError("Пароль должен быть не менее 6 символов");
      return;
    }

    setLoading(true);
    try {
      const success = await register(email, password, username);
      console.log('Register component: register result', { success });

      if (success) {
        console.log('Register component: redirecting to /files');
        navigate("/files");
      } else {
        setError("Ошибка регистрации. Возможно, email уже используется");
      }
    } catch (err) {
      console.error('Register component error:', err);
      setError("Сетевая ошибка. Проверьте подключение");
    } finally {
      setLoading(false);
    }
  }

  return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="bg-white/20 backdrop-blur-xl shadow-2xl rounded-3xl p-10 w-full max-w-md text-white">
          <h2 className="text-3xl font-bold text-center mb-6">Создать аккаунт</h2>

          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <input
                  type="email"
                  placeholder="Email"
                  required
                  className="p-3 rounded-xl bg-white/30 w-full text-white placeholder:text-white/70"
                  style={{ color: 'white' }}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="mb-3">
              <input
                  type="text"
                  placeholder="Имя пользователя"
                  required
                  className="p-3 rounded-xl bg-white/30 w-full text-white placeholder:text-white/70"
                  style={{ color: 'white' }}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
              />
            </div>

            <div className="mb-3">
              <input
                  type="password"
                  placeholder="Пароль"
                  required
                  minLength="6"
                  className="p-3 rounded-xl bg-white/30 w-full text-white placeholder:text-white/70"
                  style={{ color: 'white' }}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <div className="mb-3">
              <input
                  type="password"
                  placeholder="Повторите пароль"
                  required
                  minLength="6"
                  className="p-3 rounded-xl bg-white/30 w-full text-white placeholder:text-white/70"
                  style={{ color: 'white' }}
                  value={repeatPassword}
                  onChange={(e) => setRepeatPassword(e.target.value)}
              />
            </div>

            {error && (
                <div className="text-red-300 text-center mb-3 p-2 bg-red-500/20 rounded">
                  {error}
                </div>
            )}

            <button
                type="submit"
                disabled={loading}
                className="bg-white text-blue-600 w-full py-3 rounded-xl font-medium hover:bg-gray-100 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                  <div className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600 mr-2"></div>
                    Регистрация...
                  </div>
              ) : (
                  "Зарегистрироваться"
              )}
            </button>
          </form>

          <p className="text-center mt-3">
            Уже есть аккаунт?{" "}
            <Link to="/login" className="underline hover:text-gray-200 transition-colors">
              Войти
            </Link>
          </p>
        </div>
      </div>
  );
}