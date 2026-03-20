import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { downloadSharedFile } from '../api';

export default function SharedFilePage() {
  const { token } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [requiresPassword, setRequiresPassword] = useState(false);
  const [password, setPassword] = useState('');

  const hasAttemptedDownload = useRef(false);

  console.log("=== SharedFilePage RENDER ===");
  console.log("Token from URL:", token);
  console.log("Loading state:", loading);
  console.log("Error state:", error);
  console.log("Requires password:", requiresPassword);
  console.log("Has attempted download:", hasAttemptedDownload.current);

  useEffect(() => {
    console.log("=== useEffect triggered ===");
    console.log("Token:", token);
    console.log("Has attempted download:", hasAttemptedDownload.current);

    if (!token) {
      console.log("No token, setting error");
      setError('Ссылка недействительна');
      setLoading(false);
      return;
    }

    if (!hasAttemptedDownload.current) {
      console.log("Calling handleDownload from useEffect (first attempt)");
      hasAttemptedDownload.current = true;
      handleDownload();
    } else {
      console.log("Skipping auto download - already attempted");
    }
  }, [token]); // Зависимость только от token

  const handleDownload = async (withPassword = null) => {
    console.log("=== handleDownload START ===");
    console.log("Token:", token);
    console.log("With password provided:", withPassword ? "YES (length: " + withPassword.length + ")" : "NO");
    console.log("Stored password:", password ? "YES (length: " + password.length + ")" : "NO");

    setLoading(true);
    setError('');

    if (!withPassword) {
      setRequiresPassword(false);
    }

    try {
      const passwordToUse = withPassword || password;
      console.log("Will use password:", passwordToUse ? "YES" : "NO");

      console.log("Calling downloadSharedFile...");
      const result = await downloadSharedFile(token, passwordToUse);

      console.log("downloadSharedFile returned:", {
        hasBlob: !!result?.blob,
        blobSize: result?.blob?.size,
        filename: result?.filename
      });

      if (!result || !result.blob) {
        throw new Error('No data received from server');
      }

      if (result.blob.size === 0) {
        throw new Error('Received empty file');
      }

      const url = window.URL.createObjectURL(result.blob);
      console.log("Created blob URL:", url);

      const a = document.createElement('a');
      a.href = url;
      a.download = result.filename;
      document.body.appendChild(a);
      console.log("Triggering download click");
      a.click();

      setTimeout(() => {
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        console.log("Cleaned up");
      }, 100);

      console.log("Download triggered successfully");
      setLoading(false);

    } catch (err) {
      console.error("=== handleDownload ERROR ===");
      console.error("Error object:", err);
      console.error("Error message:", err.message);
      console.error("Error stack:", err.stack);

      if (err.message === 'PASSWORD_REQUIRED') {
        console.log("Password required - showing password form");
        setRequiresPassword(true);
        setError('');
        setLoading(false);
      } else if (err.message === 'INVALID_PASSWORD') {
        console.log("Invalid password");
        setError('Неверный пароль');
        setPassword('');
        setLoading(false);
      } else if (err.message === 'NOT_FOUND') {
        console.log("Share not found");
        setError('Ссылка недействительна или истекла');
        setLoading(false);
      } else {
        setError(err.message || 'Ошибка при скачивании файла');
        setLoading(false);
      }
    }
  };

  const handlePasswordSubmit = (e) => {
    e.preventDefault();
    console.log("Password submitted, password length:", password.length);
    if (password && password.trim()) {
      handleDownload(password);
    } else {
      setError('Введите пароль');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-6 flex items-center justify-center">
      <div className="max-w-md w-full bg-white/10 backdrop-blur-xl rounded-2xl p-8">
        <div className="text-center mb-6">
          <div className="text-6xl mb-4">📄</div>
          <h1 className="text-2xl font-bold">Скачать файл</h1>
          {token && (
            <p className="text-sm text-white/50 mt-2 break-all">
              Token: {token.substring(0, 20)}...
            </p>
          )}
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-center">
            {error}
          </div>
        )}

        {requiresPassword ? (
          <form onSubmit={handlePasswordSubmit}>
            <div className="mb-4">
              <label className="block text-sm text-white/70 mb-2">
                Эта ссылка защищена паролем
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Введите пароль"
                className="w-full p-3 rounded-xl bg-white/20 text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                autoFocus
                disabled={loading}
              />
            </div>
            <button
              type="submit"
              disabled={!password.trim() || loading}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 rounded-xl font-medium transition-colors disabled:opacity-50"
            >
              {loading ? 'Проверка...' : 'Подтвердить и скачать'}
            </button>
          </form>
        ) : (
          <div className="text-center">
            {loading ? (
              <div className="flex justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-white"></div>
              </div>
            ) : (
              <button
                onClick={() => {
                  console.log("Download button clicked");
                  handleDownload();
                }}
                className="w-full py-3 bg-green-600 hover:bg-green-700 rounded-xl font-medium transition-colors"
              >
                Скачать файл
              </button>
            )}
          </div>
        )}

        <button
          onClick={() => navigate('/')}
          className="mt-4 w-full py-2 text-white/70 hover:text-white transition-colors text-sm"
        >
          ← Вернуться на главную
        </button>
      </div>
    </div>
  );
}