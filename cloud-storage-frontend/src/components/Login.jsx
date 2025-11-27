import { Link } from "react-router-dom";
import { useState } from "react";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
      <div className="min-h-screen bg-gradient-to-br from-blue-600 to-indigo-700 flex items-center justify-center p-4">
        <div className="bg-white/20 backdrop-blur-xl shadow-2xl rounded-3xl p-10 w-full max-w-md text-white">
          <h2 className="text-3xl font-bold text-center mb-6">Войти</h2>

          <div className="flex flex-col gap-4">
            <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="p-3 rounded-xl bg-white/30 placeholder-white/70 focus:bg-white/50 outline-none"
            />

            <input
                type="password"
                placeholder="Пароль"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="p-3 rounded-xl bg-white/30 placeholder-white/70 focus:bg-white/50 outline-none"
            />

            <button className="bg-white text-blue-600 py-3 rounded-xl font-medium hover:bg-gray-100 transition">
              Войти
            </button>

            <p className="text-center mt-2">
              Нет аккаунта?{" "}
              <Link to="/register" className="underline font-semibold">
                Создать
              </Link>
            </p>
          </div>
        </div>
      </div>
  );
}
