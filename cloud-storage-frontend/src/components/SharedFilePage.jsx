import React, { useState, useEffect, useCallback } from 'react';
import { getShareInfo, downloadSharedFile } from '../api';
import { useLocation, useParams } from 'react-router-dom';

export default function SharedFilePage() {
  const location = useLocation();

  const queryParams = new URLSearchParams(location.search);
  const token = queryParams.get('shareToken');

  const [fileInfo, setFileInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState(null);
  const [requiresPassword, setRequiresPassword] = useState(false);
  const [password, setPassword] = useState('');

  const fetchInfo = useCallback(async () => {
    if (!token) {
      setError('Токен отсутствует');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const info = await getShareInfo(token);
      setFileInfo(info);
      setRequiresPassword(info.hasPassword);
    } catch (err) {
      setError(err.status === 404 ? 'LINK_EXPIRED' : 'AUTH_REQUIRED');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchInfo();
  }, [fetchInfo]);

  const handleDownload = async (e) => {
    if (e) e.preventDefault();
    setDownloading(true);
    setError(null);

    try {
      const { blob, filename } = await downloadSharedFile(token, password);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename || 'file');
      document.body.appendChild(link);
      link.click();

      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);

    } catch (err) {
      console.error("Download error:", err);
      setError(err.status === 401 ? 'INVALID_PASSWORD' : 'DOWNLOAD_FAILED');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <div className="p-20 text-center text-white">Загрузка...</div>;

  if (error && !fileInfo && error !== 'INVALID_PASSWORD') {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center p-6">
        <div className="bg-slate-800 p-10 rounded-2xl border border-red-500/30 text-center max-w-sm">
          <div className="text-4xl mb-4">⚠️</div>
          <h2 className="text-white font-bold text-xl mb-2">Доступ невозможен</h2>
          <p className="text-slate-400">
            {error === 'LINK_EXPIRED' ? 'Ссылка более не активна.' : 'Требуется авторизация или верный токен.'}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-white flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 p-8 rounded-3xl w-full max-w-md shadow-2xl">
        <div className="text-center mb-8">
          <div className="text-5xl mb-4 text-blue-500">📄</div>
          <h1 className="text-xl font-bold truncate">
            {fileInfo?.fileName || 'Защищенный объект'}
          </h1>
        </div>

        <form onSubmit={handleDownload} className="space-y-4">
          {requiresPassword && (
            <div className="space-y-2">
              <label className="text-xs text-slate-500 uppercase font-bold ml-1">Пароль</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={`w-full p-4 rounded-xl bg-slate-800 border ${error === 'INVALID_PASSWORD' ? 'border-red-500' : 'border-slate-700'} outline-none focus:border-blue-500`}
                placeholder="Введите пароль"
              />
              {error === 'INVALID_PASSWORD' && <p className="text-red-500 text-xs">Неверный пароль!</p>}
            </div>
          )}

          <button
            type="submit"
            disabled={downloading || (requiresPassword && !password)}
            className="w-full py-4 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 rounded-xl font-bold transition-all"
          >
            {downloading ? 'Процесс...' : 'Скачать'}
          </button>
        </form>
      </div>
    </div>
  );
}