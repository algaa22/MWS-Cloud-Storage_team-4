import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";

export default function Register() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError("Введите email и пароль");
      return;
    }
    if (password !== confirm) {
      setError("Пароли не совпадают");
      return;
    }
    // demo: здесь вызови реальный API регистрации
    localStorage.setItem("token", "demo-token");
    navigate("/");
  };

  return (
      <div className="page-root">
        <div className="bg-decor" />
        <main className="center-wrap">
          <form className="glass-card small" onSubmit={handleSubmit}>
            <h2>Создать аккаунт</h2>

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

            <input
                className="input"
                type="password"
                placeholder="Повторите пароль"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
            />

            {error && <div className="error">{error}</div>}

            <button className="btn btn-primary" type="submit">Зарегистрироваться</button>
            <div className="muted">
              Уже есть аккаунт? <Link to="/login">Войти</Link>
            </div>
          </form>
        </main>
      </div>
  );
}
