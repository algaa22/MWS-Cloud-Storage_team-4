import React, { useState, useEffect } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate } from "react-router-dom";
import {
  getTrashFiles,
  restoreFile,
  permanentDeleteFile,
  emptyTrash,
  getUserInfo
} from "../api";

export default function TrashPage() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState("");
  const [selectedFiles, setSelectedFiles] = useState(new Set());
  const [actionInProgress, setActionInProgress] = useState(false);

  useEffect(() => {
    if (!token) {
      navigate("/login");
      return;
    }
    loadTrashFiles();
  }, [token, navigate]);

  const loadTrashFiles = async () => {
    try {
      setLoading(true);
      const trashFiles = await getTrashFiles(token);
      setFiles(trashFiles);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const refreshStorage = async () => {
    console.log("🔄 Refreshing storage from TrashPage");

    // Способ 1: через глобальный метод forceStorageUpdate
    if (window.forceStorageUpdate) {
      console.log("Calling window.forceStorageUpdate from TrashPage");
      await window.forceStorageUpdate();
      console.log("✅ Storage force updated via global method");
      return;
    }

    // Способ 2: через getUserInfo и событие
    try {
      console.log("Getting user info for storage update...");
      const userData = await getUserInfo(token);
      console.log("User data received:", userData);

      // Отправляем событие с данными
      window.dispatchEvent(new CustomEvent('storage-updated', {
        detail: userData
      }));
      console.log("✅ Storage-updated event dispatched");
    } catch (err) {
      console.error("Failed to get user info:", err);

      // Способ 3: через refreshStorageInfo
      if (window.refreshStorageInfo) {
        console.log("Falling back to refreshStorageInfo");
        window.refreshStorageInfo();
      }
    }
  };

  const handleSelectAll = () => {
    if (selectedFiles.size === files.length) {
      setSelectedFiles(new Set());
    } else {
      setSelectedFiles(new Set(files.map(f => f.id)));
    }
  };

  const handleSelectFile = (id) => {
    const newSelected = new Set(selectedFiles);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedFiles(newSelected);
  };

  const handleRestore = async (id) => {
    try {
      setActionInProgress(true);
      console.log(`🔄 Restoring file ${id}...`);

      await restoreFile(token, id);
      console.log("✅ File restored successfully");

      // Оптимистичное обновление UI
      setFiles(prevFiles => prevFiles.filter(f => f.id !== id));
      setSelectedFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });

      // Обновляем информацию о хранилище
      await refreshStorage();

      setSuccess("Файл восстановлен");
      setTimeout(() => setSuccess(""), 2000);
    } catch (err) {
      console.error("Restore error:", err);
      setError(`Ошибка восстановления: ${err.message}`);
      await loadTrashFiles();
    } finally {
      setActionInProgress(false);
    }
  };

  const handleRestoreSelected = async () => {
    if (selectedFiles.size === 0) return;

    const idsToRestore = Array.from(selectedFiles);

    try {
      setActionInProgress(true);
      console.log(`🔄 Restoring ${idsToRestore.length} files...`);

      // Оптимистичное обновление UI
      setFiles(prevFiles => prevFiles.filter(f => !idsToRestore.includes(f.id)));
      setSelectedFiles(new Set());

      let successCount = 0;

      for (const id of idsToRestore) {
        try {
          await restoreFile(token, id);
          successCount++;
        } catch (err) {
          console.error(`Failed to restore ${id}:`, err);
        }
      }

      console.log(`✅ Restored ${successCount} files`);

      // Обновляем информацию о хранилище
      await refreshStorage();

      setSuccess(`Восстановлено ${successCount} файлов`);
      setTimeout(() => setSuccess(""), 2000);
    } catch (err) {
      setError(`Ошибка восстановления: ${err.message}`);
      await loadTrashFiles();
    } finally {
      setActionInProgress(false);
    }
  };

  const handlePermanentDelete = async (id) => {
    if (!window.confirm('Вы уверены? Файл будет удален безвозвратно!')) {
      return;
    }

    try {
      setActionInProgress(true);

      // Оптимистичное удаление из UI
      setFiles(prevFiles => prevFiles.filter(f => f.id !== id));
      setSelectedFiles(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });

      try {
        await permanentDeleteFile(token, id);
        setSuccess("Файл удален навсегда");
      } catch (err) {
        if (err.message.includes('404')) {
          console.log("File already deleted (404), UI already updated");
          setSuccess("Файл удален навсегда");
        } else {
          console.error("Permanent delete error:", err);
          setError(`Ошибка удаления: ${err.message}`);
          await loadTrashFiles();
        }
      }
      setTimeout(() => setSuccess(""), 2000);
    } finally {
      setActionInProgress(false);
    }
  };

  const handlePermanentDeleteSelected = async () => {
    if (selectedFiles.size === 0) return;

    if (!window.confirm(`Удалить ${selectedFiles.size} файлов безвозвратно?`)) {
      return;
    }

    const idsToDelete = Array.from(selectedFiles);

    try {
      setActionInProgress(true);

      // Оптимистичное удаление из UI
      setFiles(prevFiles => prevFiles.filter(f => !idsToDelete.includes(f.id)));
      setSelectedFiles(new Set());

      let successCount = 0;
      let error404Count = 0;
      let otherErrorCount = 0;

      for (const id of idsToDelete) {
        try {
          await permanentDeleteFile(token, id);
          successCount++;
        } catch (err) {
          if (err.message.includes('404')) {
            successCount++;
            error404Count++;
          } else {
            otherErrorCount++;
            console.error(`Failed to delete ${id}:`, err);
          }
        }
      }

      if (otherErrorCount === 0) {
        setSuccess(`Удалено ${successCount} файлов` + (error404Count > 0 ? ` (${error404Count} уже были удалены)` : ''));
      } else {
        setError(`Ошибка удаления: ${otherErrorCount} файлов не удалось удалить`);
        await loadTrashFiles();
      }

      setTimeout(() => setSuccess(""), 2000);
    } catch (err) {
      setError(`Ошибка удаления: ${err.message}`);
      await loadTrashFiles();
    } finally {
      setActionInProgress(false);
    }
  };

  const handleEmptyTrash = async () => {
    if (!window.confirm('Очистить корзину? Все файлы будут удалены безвозвратно!')) {
      return;
    }

    try {
      setActionInProgress(true);

      // Оптимистичное удаление из UI
      setFiles([]);
      setSelectedFiles(new Set());

      try {
        const result = await emptyTrash(token);
        setSuccess(`Корзина очищена. Удалено: ${result.success} файлов`);
      } catch (err) {
        console.error("Empty trash error:", err);
        await loadTrashFiles();
        setError(`Ошибка очистки корзины: ${err.message}`);
      }

      setTimeout(() => setSuccess(""), 2000);
    } finally {
      setActionInProgress(false);
    }
  };

  const formatSize = (bytes) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-6 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-white mx-auto mb-4"></div>
          <p className="text-white/70">Загрузка корзины...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-6 flex flex-col overflow-hidden">
      {/* Верхняя панель */}
      <div className="flex justify-between items-center mb-6 flex-shrink-0">
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
          <h1 className="text-3xl md:text-4xl font-bold text-white">Корзина</h1>
          <p className="text-white/60 text-sm mt-1">Восстановление удаленных файлов</p>
        </div>

        <div className="relative w-32 flex justify-end">
          <div className="flex items-center space-x-3 bg-white/10 backdrop-blur-sm rounded-xl px-4 py-2.5 border border-white/10">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500/90 to-purple-500/90 rounded-full flex items-center justify-center text-white font-bold shadow-md">
              {((user?.username || user?.name || user?.email || "U").charAt(0)).toUpperCase()}
            </div>
            <div className="text-left">
              <span className="font-medium text-white text-sm block">
                {user?.username || user?.name || user?.email?.split('@')[0] || "Пользователь"}
              </span>
              {user?.email && <span className="text-white/50 text-xs block">{user.email}</span>}
            </div>
          </div>
        </div>
      </div>

      {/* Сообщения */}
      {error && (
        <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-center">
          {error}
          <button onClick={loadTrashFiles} className="ml-3 underline hover:no-underline">
            Повторить
          </button>
        </div>
      )}

      {success && (
        <div className="mb-4 p-3 bg-green-500/20 border border-green-500 rounded-xl text-center">
          {success}
        </div>
      )}

      {/* Панель инструментов */}
      <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 mb-6 flex-shrink-0">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center space-x-4">
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                checked={selectedFiles.size === files.length && files.length > 0}
                onChange={handleSelectAll}
                disabled={files.length === 0 || actionInProgress}
                className="w-4 h-4 rounded border-white/30 bg-white/10 checked:bg-blue-500 focus:ring-blue-500 focus:ring-offset-0"
              />
              <span>Выбрать все</span>
            </label>
            {selectedFiles.size > 0 && (
              <span className="text-white/70 text-sm">Выбрано: {selectedFiles.size}</span>
            )}
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              onClick={handleRestoreSelected}
              disabled={selectedFiles.size === 0 || actionInProgress}
              className="px-4 py-2 bg-green-600 hover:bg-green-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
            >
              <span>↺</span>
              <span>Восстановить выбранные</span>
            </button>

            <button
              onClick={handlePermanentDeleteSelected}
              disabled={selectedFiles.size === 0 || actionInProgress}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
            >
              <span>×</span>
              <span>Удалить выбранные</span>
            </button>

            <button
              onClick={handleEmptyTrash}
              disabled={files.length === 0 || actionInProgress}
              className="px-4 py-2 bg-gray-600 hover:bg-gray-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Очистить корзину
            </button>
          </div>
        </div>
      </div>

      {/* Список файлов */}
      <div className="flex-1 overflow-auto bg-white/5 backdrop-blur-sm rounded-2xl p-4">
        {files.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center">
            <div className="text-6xl mb-4 opacity-50">🗑️</div>
            <p className="text-xl mb-2">Корзина пуста</p>
            <p className="text-white/70">Удаленные файлы появятся здесь</p>
          </div>
        ) : (
          <table className="w-full">
            <thead className="sticky top-0 bg-gray-800/95 backdrop-blur-sm">
              <tr className="border-b border-white/10">
                <th className="p-4 text-left w-10"></th>
                <th className="p-4 text-left">Имя</th>
                <th className="p-4 text-left">Тип</th>
                <th className="p-4 text-left">Размер</th>
                <th className="p-4 text-left">Дата удаления</th>
                <th className="p-4 text-left">Действия</th>
              </tr>
            </thead>
            <tbody>
              {files.map(file => (
                <tr key={file.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                  <td className="p-4">
                    <input
                      type="checkbox"
                      checked={selectedFiles.has(file.id)}
                      onChange={() => handleSelectFile(file.id)}
                      disabled={actionInProgress}
                      className="w-4 h-4 rounded border-white/30 bg-white/10 checked:bg-blue-500 focus:ring-blue-500 focus:ring-offset-0"
                    />
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-3">
                      <span className="text-2xl">{file.type === 'folder' ? '📁' : '📄'}</span>
                      <span className="font-medium truncate max-w-[200px]">{file.name}</span>
                    </div>
                  </td>
                  <td className="p-4 text-white/70">
                    {file.type === 'folder' ? 'Папка' : 'Файл'}
                  </td>
                  <td className="p-4 text-white/70">
                    {file.type === 'file' ? formatSize(file.size) : '—'}
                  </td>
                  <td className="p-4 text-white/70">
                    {formatDate(file.deletedAt)}
                  </td>
                  <td className="p-4">
                    <div className="flex space-x-2">
                      <button
                        onClick={() => handleRestore(file.id)}
                        disabled={actionInProgress}
                        className="p-2 bg-green-600 hover:bg-green-700 rounded-lg transition-colors disabled:opacity-50"
                        title="Восстановить"
                      >
                        ↺
                      </button>
                      <button
                        onClick={() => handlePermanentDelete(file.id)}
                        disabled={actionInProgress}
                        className="p-2 bg-red-600 hover:bg-red-700 rounded-lg transition-colors disabled:opacity-50"
                        title="Удалить навсегда"
                      >
                        ×
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Нижняя панель с информацией */}
      {files.length > 0 && (
        <div className="mt-4 pt-4 border-t border-white/10 text-white/60 text-sm flex-shrink-0">
          Всего файлов: {files.length} |
          Размер: {formatSize(files.reduce((sum, f) => sum + (f.size || 0), 0))}
        </div>
      )}
    </div>
  );
}