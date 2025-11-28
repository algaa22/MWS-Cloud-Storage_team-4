import { Link } from "react-router-dom";

export default function Landing() {
  return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-900 to-blue-600 p-4">

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-3xl">

          <Link
              to="/login"
              className="bg-white/10 backdrop-blur-xl border border-white/20 rounded-3xl p-10 text-center shadow-xl hover:bg-white/20 transition"
          >
            <h2 className="text-3xl text-white font-bold mb-2">Вход</h2>
            <p className="text-white/70">У меня уже есть аккаунт</p>
          </Link>

          <Link
              to="/register"
              className="bg-white/10 backdrop-blur-xl border border-white/20 rounded-3xl p-10 text-center shadow-xl hover:bg-white/20 transition"
          >
            <h2 className="text-3xl text-white font-bold mb-2">Регистрация</h2>
            <p className="text-white/70">Создать новый аккаунт</p>
          </Link>

        </div>
      </div>
  );
}
