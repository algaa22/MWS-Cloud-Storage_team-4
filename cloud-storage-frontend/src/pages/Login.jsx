import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    // demo: тут нужно вызвать реальный API
    if (!email || !password) {
      setError("Введите email и пароль");
      return;
    }
    // сохраняем токен для демонстрации
    localStorage.setItem("token", "demo-token");
    navigate("/");
  };

  return (
      <div className="page-root">
        <div className="bg-decor" />
        <main className="center-wrap">
          <form className="glass-card small" onSubmit={handleSubmit}>
            <h2>Вход</h2>
            <input
                className="input"
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
            />
            <input
                className="input"
                type="password"
                placeholder="Пароль"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
            />
            {error && <div className="error">{error}</div>}
            <button className="btn btn-primary" type="submit">Войти</button>
            <div className="muted">
              Нет аккаунта? <Link to="/register">Создать</Link>
            </div>
          </form>
        </main>
      </div>
  );
}
