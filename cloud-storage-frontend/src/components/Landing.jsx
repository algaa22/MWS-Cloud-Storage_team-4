import React from 'react';
import { Link } from "react-router-dom";

export default function Landing() {
  return (
      <div className="min-h-screen flex flex-col items-center justify-center text-white text-center p-6">
        <h1 className="text-5xl font-bold mb-4">Облачное хранилище</h1>
        <p className="text-xl opacity-90 mb-8">
          Храни и управляй своими файлами легко и удобно.
        </p>

        <div className="flex gap-4">
          <Link
              className="px-6 py-3 bg-white text-blue-600 rounded-xl font-semibold"
              to="/login"
          >
            Войти
          </Link>

          <Link
              className="px-6 py-3 bg-white/30 backdrop-blur-xl border border-white/50 rounded-xl font-semibold"
              to="/register"
          >
            Создать аккаунт
          </Link>
        </div>
      </div>
  );
}
