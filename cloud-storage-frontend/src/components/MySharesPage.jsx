import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate } from "react-router-dom";
import {
  getUserShares,
  deactivateShare,
  deleteSharePermanently
} from "../api";

export default function MySharesPage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [shares, setShares] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [deactivatingId, setDeactivatingId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    loadShares();
  }, []);

  const loadShares = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await getUserShares(token);
      console.log("Shares loaded:", data);
      setShares(data?.shares || []);
    } catch (err) {
      console.error("Failed to load shares:", err);
      setError("Не удалось загрузить список ссылок");
    } finally {
      setLoading(false);
    }
  };

  // Деактивация ссылки (меняет статус на неактивную)
  const handleDeactivate = async (shareId) => {
    console.log("Deactivating share:", shareId);

    if (!window.confirm("Вы уверены, что хотите деактивировать эту ссылку?")) {
      return;
    }

    setDeactivatingId(shareId);
    setError("");
    setSuccess("");

    try {
      await deactivateShare(token, shareId);

      // Обновляем статус ссылки на неактивный
      setShares(prevShares =>
        prevShares.map(share =>
          share.id === shareId
            ? { ...share, isActive: false }
            : share
        )
      );

      setSuccess("Ссылка деактивирована");
    } catch (err) {
      console.error("Failed to deactivate share:", err);
      setError(`Ошибка: ${err.message}`);
    } finally {
      setDeactivatingId(null);
      setTimeout(() => {
        setSuccess("");
        setError("");
      }, 3000);
    }
  };

  // Полное удаление ссылки из списка
  const handleDelete = async (shareId) => {
    if (!window.confirm("Удалить ссылку безвозвратно? Это действие нельзя отменить.")) {
      return;
    }

    setDeletingId(shareId);

    try {
      await deleteSharePermanently(token, shareId);
      setShares(prevShares => prevShares.filter(share => share.id !== shareId));
      setSuccess("Ссылка удалена навсегда");
    } catch (err) {
      console.error("Failed to delete share:", err);
      setError(`Ошибка: ${err.message}`);
    } finally {
      setDeletingId(null);
      setTimeout(() => {
        setSuccess("");
        setError("");
      }, 3000);
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setSuccess("Ссылка скопирована в буфер обмена");
    setTimeout(() => setSuccess(""), 3000);
  };

  const formatDate = (dateString) => {
    if (!dateString) return "Без ограничения";
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return "—";
      return date.toLocaleDateString("ru-RU", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      });
    } catch {
      return "—";
    }
  };

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return "—";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const getShareTypeIcon = (type, hasPassword) => {
    if (hasPassword) {
      return (
        <svg className="w-5 h-5 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
      );
    }
    return (
      <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    );
  };

  const getStatusBadge = (share) => {
    const isExpired = share.expiresAt && new Date(share.expiresAt) < new Date();
    const isLimitReached = share.maxDownloads && share.downloadCount >= share.maxDownloads;

    if (!share.isActive || isExpired || isLimitReached) {
      return (
        <span className="px-2 py-1 bg-red-500/20 text-red-300 rounded-full text-xs font-medium">
          Неактивна
        </span>
      );
    }

    return (
      <span className="px-2 py-1 bg-green-500/20 text-green-300 rounded-full text-xs font-medium">
        Активна
      </span>
    );
  };

  const getProgressPercentage = (share) => {
    if (!share.maxDownloads) return null;
    const percentage = Math.round((share.downloadCount / share.maxDownloads) * 100);
    return Math.min(percentage, 100);
  };

  return (
    <div className="h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-6 flex flex-col overflow-hidden">
      {/* Верхняя панель */}
      <div className="flex justify-between items-center mb-4 flex-shrink-0">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center text-white/70 hover:text-white transition-colors"
        >
          <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Назад
        </button>

        <div className="text-center">
          <h1 className="text-3xl md:text-4xl font-bold text-white">
            Мои ссылки
          </h1>
          <p className="text-white/60 text-sm mt-1">
            Управление созданными вами ссылками
          </p>
        </div>

        <div className="relative w-32 flex justify-end">
          <button className="flex items-center space-x-3 bg-white/10 hover:bg-white/15 backdrop-blur-sm rounded-xl px-4 py-2.5 transition-all duration-200 border border-white/10 hover:border-white/20">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500/90 to-purple-500/90 rounded-full flex items-center justify-center text-white font-bold shadow-md">
              {((user?.name || user?.username || user?.email || "U").charAt(0)).toUpperCase()}
            </div>
            <div className="text-left">
              <span className="font-medium text-white text-sm block">
                {user?.name || user?.username || user?.email?.split('@')[0] || "Пользователь"}
              </span>
              {user?.email && <span className="text-white/50 text-xs block">{user.email}</span>}
            </div>
          </button>
        </div>
      </div>

      {/* Основной контент */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Статистика */}
        {shares.length > 0 && (
          <div className="bg-gradient-to-r from-blue-600/20 to-purple-600/20 backdrop-blur-sm rounded-2xl p-4 border border-white/10 mb-4 flex-shrink-0">
            <div className="grid grid-cols-3 gap-3">
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-300">{shares.length}</div>
                <div className="text-xs text-white/60">Всего ссылок</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-300">
                  {shares.filter(s => s.isActive).length}
                </div>
                <div className="text-xs text-white/60">Активных</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-yellow-300">
                  {shares.reduce((sum, s) => sum + (s.downloadCount || 0), 0)}
                </div>
                <div className="text-xs text-white/60">Всего скачиваний</div>
              </div>
            </div>
          </div>
        )}

        {/* Сообщения */}
        {error && (
          <div className="mb-3 p-2 bg-red-500/20 border border-red-500 rounded-xl text-center text-sm flex-shrink-0">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-3 p-2 bg-green-500/20 border border-green-500 rounded-xl text-center text-sm flex-shrink-0">
            {success}
          </div>
        )}

        {loading && (
          <div className="mb-3 p-2 bg-blue-500/20 border border-blue-500 rounded-xl text-center text-sm flex-shrink-0">
            Загрузка списка ссылок...
          </div>
        )}

        {/* Список ссылок */}
        <div className="flex-1 overflow-y-auto space-y-3 pr-2">
          {!loading && shares.length === 0 && (
            <div className="text-center py-12">
              <svg className="w-24 h-24 mx-auto text-white/20 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-white/60 text-lg">У вас пока нет созданных ссылок</p>
              <p className="text-white/40 text-sm mt-2">
                Чтобы создать ссылку, выберите файл в хранилище и нажмите "Создать публичную ссылку"
              </p>
            </div>
          )}

          {shares.map((share) => (
            <div
              key={share.id}
              className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/10 hover:border-blue-500/50 transition-all duration-200"
            >
              <div className="flex flex-col md:flex-row md:items-start justify-between gap-3">
                {/* Левая часть - информация о файле */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-2">
                    {getShareTypeIcon(share.shareType, share.hasPassword)}
                    <h3 className="font-semibold text-white text-lg truncate">
                      {share.fileName || share.file?.name || "Файл"}
                    </h3>
                    {getStatusBadge(share)}
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-2 text-sm">
                    <div className="flex items-center text-white/60">
                      <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                      <span>Размер: {formatBytes(share.fileSize)}</span>
                    </div>
                    <div className="flex items-center text-white/60">
                      <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span>Создана: {formatDate(share.createdAt)}</span>
                    </div>
                    <div className="flex items-center text-white/60 whitespace-nowrap">
                      <svg className="w-4 h-4 mr-1 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span>Истекает: {formatDate(share.expiresAt)}</span>
                    </div>
                  </div>

                  {/* Прогресс скачиваний */}
                  {share.maxDownloads ? (
                    <div className="mt-3">
                      <div className="flex justify-between text-xs mb-1">
                        <span className="text-white/60">Скачивания</span>
                        <span className="text-blue-300">
                          {share.downloadCount || 0} / {share.maxDownloads}
                        </span>
                      </div>
                      <div className="w-full bg-gray-700/50 rounded-full h-1.5">
                        <div
                          className="bg-blue-500 h-1.5 rounded-full transition-all duration-300"
                          style={{ width: `${getProgressPercentage(share)}%` }}
                        />
                      </div>
                    </div>
                  ) : (
                    <div className="mt-3 text-xs text-white/40">
                      Без ограничения скачиваний
                    </div>
                  )}

                  {/* Ссылка */}
                  <div className="mt-3 flex items-center gap-2 bg-black/20 rounded-lg p-2">
                    <span className="text-white/40 text-sm truncate flex-1">
                      {`http://localhost:5173/s?shareToken=${share.shareToken}`}
                    </span>
                    <button
                      onClick={() => copyToClipboard(`http://localhost:5173/s?shareToken=${share.shareToken}`)}
                      className="p-1 hover:bg-white/10 rounded transition-colors"
                      title="Копировать ссылку"
                    >
                      <svg className="w-4 h-4 text-white/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                      </svg>
                    </button>
                  </div>
                </div>

                {/* Правая часть - кнопки действий */}
                <div className="flex flex-row md:flex-col gap-2">
                  {share.isActive ? (
                    <button
                      onClick={() => handleDeactivate(share.id)}
                      disabled={deactivatingId === share.id}
                      className="px-3 py-1.5 bg-red-500/20 hover:bg-red-500/30 rounded-lg text-red-300 text-sm transition-colors flex items-center justify-center gap-1 disabled:opacity-50"
                    >
                      {deactivatingId === share.id ? (
                        <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-red-300"></div>
                      ) : (
                        <>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                          </svg>
                          Деактивировать
                        </>
                      )}
                    </button>
                  ) : (
                    <button
                      onClick={() => handleDelete(share.id)}
                      disabled={deletingId === share.id}
                      className="px-3 py-1.5 bg-gray-500/20 hover:bg-gray-500/30 rounded-lg text-gray-300 text-sm transition-colors flex items-center justify-center gap-1 disabled:opacity-50"
                    >
                      {deletingId === share.id ? (
                        <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-gray-300"></div>
                      ) : (
                        <>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                          Удалить
                        </>
                      )}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}