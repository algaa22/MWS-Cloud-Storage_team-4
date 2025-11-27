import { useState } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate, Link } from "react-router-dom";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function handleLogin() {
    setError("");
    const ok = await login(email, password);
    if (!ok) {
      setError("Неверные данные");
      return;
    }
    navigate("/storage");
  }

  return (
      <div className="min-h-screen bg-gradient-to-br from-blue-600 to-indigo-700 flex items-center justify-center p-4">
        <div className="bg-white/20 backdrop-blur-xl shadow-2xl rounded-3xl p-10 w-full max-w-md text-white">

          <h2 className="text-3xl font-bold text-center mb-6">Вход</h2>

          <input
              type="email"
              placeholder="Email"
              className="p-3 rounded-xl bg-white/30 w-full mb-3"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
          />

          <input
              type="password"
              placeholder="Пароль"
              className="p-3 rounded-xl bg-white/30 w-full mb-3"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
          />

          {error && <div className="text-red-300 text-center mb-3">{error}</div>}

          <button
              onClick={handleLogin}
              className="bg-white text-blue-600 w-full py-3 rounded-xl font-medium"
          >
            Войти
          </button>

          <p className="text-center mt-2">
            Нет аккаунта? <Link to="/register" className="underline">Создать</Link>
          </p>
        </div>
      </div>
  );
}
