import React, { useState } from 'react';
import { createShareLink } from '../api';

export default function ShareModal({ file, token, onClose, onShareCreated }) {
  const [shareType, setShareType] = useState('PUBLIC');
  const [expiresIn, setExpiresIn] = useState('7');
  const [maxDownloads, setMaxDownloads] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [shareUrl, setShareUrl] = useState(null);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  const handleCreateShare = async () => {
    setLoading(true);
    setError('');

    try {
      const options = {
        shareType,
        maxDownloads: maxDownloads ? parseInt(maxDownloads) : null,
      };

      if (expiresIn) {
        const expiresAt = new Date();
        expiresAt.setDate(expiresAt.getDate() + parseInt(expiresIn));
        options.expiresAt = expiresAt.toISOString();
      }

      if (shareType === 'PROTECTED' && password) {
        options.password = password;
      }

      console.log("Creating share with options:", options);
      const response = await createShareLink(token, file.id, options);
      console.log("Share created response:", response);

      const getFrontendBaseUrl = () => {
        if (process.env.NODE_ENV === 'development') {
          return 'http://localhost:5173';
        }
        return window.location.origin;
      };

      const frontendBaseUrl = getFrontendBaseUrl();
      const fullUrl = `${frontendBaseUrl}/s/${response.shareToken}`;

      console.log('Generated frontend URL:', fullUrl);
      setShareUrl(fullUrl);

      if (onShareCreated) {
        onShareCreated(response);
      }
    } catch (err) {
      console.error("Share creation error:", err);
      setError(err.message || 'Ошибка при создании ссылки');
    } finally {
      setLoading(false);
    }
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleClose = () => {
    setShareUrl(null);
    onClose();
  };

  if (shareUrl) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
        <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
          <h3 className="text-xl font-bold mb-4 text-white">Ссылка создана!</h3>

          <div className="mb-4">
            <label className="block text-sm text-white/70 mb-2">
              Скопируйте ссылку:
            </label>
            <div className="flex">
              <input
                type="text"
                value={shareUrl}
                readOnly
                className="flex-1 p-2 rounded-l-lg bg-white/20 text-white text-sm"
              />
              <button
                onClick={handleCopyLink}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-r-lg text-white transition-colors"
              >
                {copied ? '✓' : '📋'}
              </button>
            </div>
          </div>

          <div className="flex justify-end">
            <button
              onClick={handleClose}
              className="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-xl transition-colors"
            >
              Закрыть
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold text-white">
            Поделиться файлом
          </h3>
          <button
            onClick={onClose}
            className="text-white/70 hover:text-white text-2xl"
          >
            ×
          </button>
        </div>

        <div className="mb-4 p-3 bg-white/10 rounded-xl">
          <p className="text-white font-medium truncate">{file.name}</p>
          <p className="text-white/60 text-sm">
            {file.size ? `${(file.size / 1024).toFixed(2)} KB` : 'Размер неизвестен'}
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-sm text-white">
            {error}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm text-white/70 mb-2">
            Тип доступа:
          </label>
          <select
            value={shareType}
            onChange={(e) => setShareType(e.target.value)}
            className="w-full p-3 rounded-xl bg-white/20 text-white"
          >
            <option value="PUBLIC">Публичный (любой по ссылке)</option>
            <option value="PROTECTED">Защищенный (с паролем)</option>
            <option value="PRIVATE">Приватный (только для пользователей)</option>
          </select>
        </div>

        <div className="mb-4">
          <label className="block text-sm text-white/70 mb-2">
            Ссылка действительна:
          </label>
          <select
            value={expiresIn}
            onChange={(e) => setExpiresIn(e.target.value)}
            className="w-full p-3 rounded-xl bg-white/20 text-white"
          >
            <option value="1">1 день</option>
            <option value="7">7 дней</option>
            <option value="30">30 дней</option>
            <option value="">Бессрочно</option>
          </select>
        </div>

        <div className="mb-4">
          <label className="block text-sm text-white/70 mb-2">
            Лимит скачиваний (оставьте пустым для безлимита):
          </label>
          <input
            type="number"
            min="1"
            value={maxDownloads}
            onChange={(e) => setMaxDownloads(e.target.value)}
            placeholder="Например: 10"
            className="w-full p-3 rounded-xl bg-white/20 text-white"
          />
        </div>

        {shareType === 'PROTECTED' && (
          <div className="mb-4">
            <label className="block text-sm text-white/70 mb-2">
              Пароль для доступа:
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Введите пароль"
              className="w-full p-3 rounded-xl bg-white/20 text-white"
            />
          </div>
        )}

        <div className="flex justify-end space-x-3">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-white/20 hover:bg-white/30 transition-colors"
            disabled={loading}
          >
            Отмена
          </button>
          <button
            onClick={handleCreateShare}
            disabled={loading || (shareType === 'PROTECTED' && !password)}
            className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center"
          >
            {loading ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white mr-2"></div>
                Создание...
              </>
            ) : (
              'Создать ссылку'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}