import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { register } from "../api";

export default function Register() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [error, setError] = useState("");

  const navigate = useNavigate();

  async function handleRegister(e) {
    e.preventDefault();
    setError("");

    if (password !== repeatPassword) {
      setError("Пароли не совпадают");
      return;
    }

    try {
      const res = await register(email, password);

      if (res.token) {
        localStorage.setItem("token", res.token);
      }

      navigate("/browser"); // переход в файловый менеджер
    } catch (err) {
      setError("Ошибка регистрации");
      console.error(err);
    }
  }

  return (
      <div className="min-h-screen bg-gradient-to-br from-blue-600 to-indigo-700 flex items-center justify-center p-4">
        <form
            onSubmit={handleRegister}
            className="bg-white/20 backdrop-blur-xl shadow-2xl rounded-3xl p-10 w-full max-w-md text-white"
        >
          <h2 className="text-3xl font-bold text-center mb-6">Создать аккаунт</h2>

          <div className="flex flex-col gap-4">
            <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="p-3 rounded-xl bg-white/30 placeholder-white/70 focus:bg-white/50 outline-none"
                required
            />

            <input
                type="password"
                placeholder="Пароль"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="p-3 rounded-xl bg-white/30 placeholder-white/70 focus:bg-white/50 outline-none"
                required
            />

            <input
                type="password"
                placeholder="Повторите пароль"
                value={repeatPassword}
                onChange={(e) => setRepeatPassword(e.target.value)}
                className="p-3 rounded-xl bg-white/30 placeholder-white/70 focus:bg-white/50 outline-none"
                required
            />

            {error && (
                <p className="text-red-400 text-center">{error}</p>
            )}

            <button
                type="submit"
                className="bg-white text-blue-600 py-3 rounded-xl font-medium hover:bg-gray-100 transition"
            >
              Зарегистрироваться
            </button>

            <p className="text-center mt-2">
              Уже есть аккаунт?{" "}
              <Link to="/login" className="underline font-semibold">
                Войти
              </Link>
            </p>
          </div>
        </form>
      </div>
  );
}
