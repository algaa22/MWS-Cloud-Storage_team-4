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

  async function handleRegister() {
    setError("");
    if (password !== repeatPassword) {
      setError("Пароли не совпадают");
      return;
    }

    const ok = await register(email, password, username);
    if (!ok) {
      setError("Ошибка регистрации");
      return;
    }
    navigate("/files");;
  }

  return (
      <div className="min-h-screen bg-gradient-to-br from-blue-600 to-indigo-700 flex items-center justify-center p-4">
        <div className="bg-white/20 backdrop-blur-xl shadow-2xl rounded-3xl p-10 w-full max-w-md text-white">

          <h2 className="text-3xl font-bold text-center mb-6">Создать аккаунт</h2>

          <input
              placeholder="Email"
              className="p-3 rounded-xl bg-white/30 w-full mb-3"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
          />

          <input
              placeholder="Имя пользователя"
              className="p-3 rounded-xl bg-white/30 w-full mb-3"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
          />

          <input
              type="password"
              placeholder="Пароль"
              className="p-3 rounded-xl bg-white/30 w-full mb-3"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
          />

          <input
              type="password"
              placeholder="Повторите пароль"
              className="p-3 rounded-xl bg-white/30 w-full mb-3"
              value={repeatPassword}
              onChange={(e) => setRepeatPassword(e.target.value)}
          />

          {error && <div className="text-red-300 text-center mb-3">{error}</div>}

          <button
              onClick={handleRegister}
              className="bg-white text-blue-600 w-full py-3 rounded-xl font-medium"
          >
            Зарегистрироваться
          </button>

          <p className="text-center mt-2">
            Уже есть аккаунт? <Link to="/login" className="underline">Войти</Link>
          </p>
        </div>
      </div>
  );
}
