import React from "react";
import { Link } from "react-router-dom";

export default function Home() {
  return (
      <div className="page-root">
        <div className="bg-decor" />

        <main className="center-wrap">
          <div className="glass-card">
            <div className="brand">
              <div className="logo-circle">☁️</div>
              <h1>CloudBox</h1>
            </div>

            <p className="subtitle">Храни свои файлы красиво и безопасно</p>

            <div className="actions">
              <Link to="/login" className="btn btn-primary">
                Войти
              </Link>

              <Link to="/register" className="btn btn-ghost">
                Создать аккаунт
              </Link>
            </div>

            <div className="fancy-shapes" aria-hidden>
              <div className="shape s1" />
              <div className="shape s2" />
            </div>
          </div>
        </main>
      </div>
  );
}
