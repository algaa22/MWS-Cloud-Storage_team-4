// components/TrashBrowser.jsx
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

export default function TrashBrowser() {
  const { user, token } = useAuth();
  const navigate = useNavigate();
  const [rootItems, setRootItems] = useState([]); // Корневые элементы (parentId = null)
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState("");
  const [actionInProgress, setActionInProgress] = useState(false);
  const [expandedFolders, setExpandedFolders] = useState(new Set()); // Развернутые папки
  const [selectedItems, setSelectedItems] = useState(new Set());
  const [itemsMap, setItemsMap] = useState(new Map()); // Быстрый доступ к элементам по ID

  useEffect(() => {
    if (!token) {
      navigate("/login");
      return;
    }
    loadTrashHierarchy();
  }, [token, navigate]);

  const loadTrashHierarchy = async () => {
    try {
      setLoading(true);
      // Загружаем все файлы из корзины (recursive = true)
      const allItems = await getTrashFiles(token, null, true);

      // Строим иерархию
      const map = new Map();
      const roots = [];

      // Сначала создаем Map всех элементов
      allItems.forEach(item => {
        map.set(item.id, { ...item, children: [] });
      });

      // Строим дерево
      map.forEach(item => {
        if (item.parentId && map.has(item.parentId)) {
          const parent = map.get(item.parentId);
          parent.children.push(item);
        } else if (!item.parentId) {
          roots.push(item);
        }
      });

      // Сортируем: папки сверху
      const sortItems = (items) => {
        items.sort((a, b) => {
          if (a.type === 'folder' && b.type !== 'folder') return -1;
          if (a.type !== 'folder' && b.type === 'folder') return 1;
          return a.name.localeCompare(b.name);
        });
        items.forEach(item => {
          if (item.children.length > 0) {
            sortItems(item.children);
          }
        });
      };

      sortItems(roots);

      setItemsMap(map);
      setRootItems(roots);
      setError(null);
    } catch (err) {
      console.error("Error loading trash hierarchy:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const refreshStorage = async () => {
    if (window.forceStorageUpdate) {
      await window.forceStorageUpdate();
      return;
    }
    try {
      const userData = await getUserInfo(token);
      window.dispatchEvent(new CustomEvent('storage-updated', { detail: userData }));
    } catch (err) {
      if (window.refreshStorageInfo) {
        window.refreshStorageInfo();
      }
    }
  };

  const toggleFolder = (folderId) => {
    setExpandedFolders(prev => {
      const newSet = new Set(prev);
      if (newSet.has(folderId)) {
        newSet.delete(folderId);
      } else {
        newSet.add(folderId);
      }
      return newSet;
    });
  };

  const handleSelectAll = () => {
    const collectAllIds = (items) => {
      let ids = [];
      items.forEach(item => {
        ids.push(item.id);
        if (item.children.length > 0) {
          ids = ids.concat(collectAllIds(item.children));
        }
      });
      return ids;
    };

    const allIds = collectAllIds(rootItems);
    if (selectedItems.size === allIds.length && allIds.length > 0) {
      setSelectedItems(new Set());
    } else {
      setSelectedItems(new Set(allIds));
    }
  };

  const handleSelectItem = (id) => {
    const newSelected = new Set(selectedItems);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedItems(newSelected);
  };

  const handleRestore = async (id) => {
    try {
      setActionInProgress(true);
      await restoreFile(token, id);

      // Удаляем элемент из локального состояния
      removeItemFromTree(id);

      await refreshStorage();
      setSuccess("Файл восстановлен");
      setTimeout(() => setSuccess(""), 2000);
    } catch (err) {
      console.error("Restore error:", err);
      setError(`Ошибка восстановления: ${err.message}`);
      await loadTrashHierarchy();
    } finally {
      setActionInProgress(false);
    }
  };

  const removeItemFromTree = (id) => {
    const removeRecursive = (items) => {
      for (let i = 0; i < items.length; i++) {
        if (items[i].id === id) {
          items.splice(i, 1);
          return true;
        }
        if (items[i].children.length > 0) {
          if (removeRecursive(items[i].children)) {
            return true;
          }
        }
      }
      return false;
    };

    removeRecursive(rootItems);
    setItemsMap(prev => {
      const newMap = new Map(prev);
      newMap.delete(id);
      return newMap;
    });
    setSelectedItems(prev => {
      const newSet = new Set(prev);
      newSet.delete(id);
      return newSet;
    });
  };

  const handleRestoreSelected = async () => {
    if (selectedItems.size === 0) return;

    const idsToRestore = Array.from(selectedItems);

    try {
      setActionInProgress(true);

      // Удаляем выбранные элементы из UI
      idsToRestore.forEach(id => removeItemFromTree(id));

      let successCount = 0;
      for (const id of idsToRestore) {
        try {
          await restoreFile(token, id);
          successCount++;
        } catch (err) {
          console.error(`Failed to restore ${id}:`, err);
        }
      }

      await refreshStorage();
      setSuccess(`Восстановлено ${successCount} файлов`);
      setTimeout(() => setSuccess(""), 2000);
    } catch (err) {
      setError(`Ошибка восстановления: ${err.message}`);
      await loadTrashHierarchy();
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
      removeItemFromTree(id);

      try {
        await permanentDeleteFile(token, id);
        setSuccess("Файл удален навсегда");
      } catch (err) {
        if (!err.message.includes('404')) {
          console.error("Permanent delete error:", err);
          setError(`Ошибка удаления: ${err.message}`);
          await loadTrashHierarchy();
        } else {
          setSuccess("Файл удален навсегда");
        }
      }
      setTimeout(() => setSuccess(""), 2000);
    } finally {
      setActionInProgress(false);
    }
  };

  const handlePermanentDeleteSelected = async () => {
    if (selectedItems.size === 0) return;

    if (!window.confirm(`Удалить ${selectedItems.size} файлов безвозвратно?`)) {
      return;
    }

    const idsToDelete = Array.from(selectedItems);

    try {
      setActionInProgress(true);

      idsToDelete.forEach(id => removeItemFromTree(id));

      let successCount = 0;
      for (const id of idsToDelete) {
        try {
          await permanentDeleteFile(token, id);
          successCount++;
        } catch (err) {
          if (!err.message.includes('404')) {
            console.error(`Failed to delete ${id}:`, err);
          } else {
            successCount++;
          }
        }
      }

      setSuccess(`Удалено ${successCount} файлов`);
      setTimeout(() => setSuccess(""), 2000);
    } catch (err) {
      setError(`Ошибка удаления: ${err.message}`);
      await loadTrashHierarchy();
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
      setRootItems([]);
      setSelectedItems(new Set());

      try {
        const result = await emptyTrash(token);
        setSuccess(`Корзина очищена. Удалено: ${result.success} файлов`);
      } catch (err) {
        console.error("Empty trash error:", err);
        await loadTrashHierarchy();
        setError(`Ошибка очистки корзины: ${err.message}`);
      }
      setTimeout(() => setSuccess(""), 2000);
    } finally {
      setActionInProgress(false);
    }
  };

  const formatSize = (bytes) => {
    if (!bytes || bytes === 0) return '—';
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

  const countTotalItems = (items) => {
    let count = items.length;
    items.forEach(item => {
      if (item.children.length > 0) {
        count += countTotalItems(item.children);
      }
    });
    return count;
  };

  const calculateTotalSize = (items) => {
    let size = 0;
    items.forEach(item => {
      if (item.type === 'file') {
        size += item.size || 0;
      }
      if (item.children.length > 0) {
        size += calculateTotalSize(item.children);
      }
    });
    return size;
  };

  // Компонент отображения элемента дерева
  const TreeItem = ({ item, level = 0 }) => {
    const isExpanded = expandedFolders.has(item.id);
    const isSelected = selectedItems.has(item.id);
    const hasChildren = item.children && item.children.length > 0;

    return (
      <React.Fragment>
        <tr className={`border-b border-white/5 hover:bg-white/5 transition-colors ${
          item.type === 'folder' ? 'bg-white/5' : ''
        }`}>
          <td className="p-4" style={{ paddingLeft: `${20 + level * 20}px` }}>
            <div className="flex items-center space-x-2">
              {/* Индикатор разворачивания для папок */}
              {item.type === 'folder' && (
                <button
                  onClick={() => toggleFolder(item.id)}
                  className="w-6 h-6 flex items-center justify-center hover:bg-white/10 rounded transition-colors"
                  disabled={actionInProgress}
                >
                  {isExpanded ? '▼' : '▶'}
                </button>
              )}

              {/* Чекбокс */}
              <input
                type="checkbox"
                checked={isSelected}
                onChange={() => handleSelectItem(item.id)}
                disabled={actionInProgress}
                className="w-4 h-4 rounded border-white/30 bg-white/10 checked:bg-blue-500 focus:ring-blue-500 focus:ring-offset-0"
              />
            </div>
           </td>

          <td className="p-4">
            <div className="flex items-center space-x-3">
              <span className="text-2xl">{item.type === 'folder' ? '📁' : '📄'}</span>
              <span className="font-medium truncate max-w-[200px]">{item.name}</span>
            </div>
           </td>

          <td className="p-4 text-white/70">
            {item.type === 'folder' ? `Папка (${item.children.length})` : 'Файл'}
           </td>

          <td className="p-4 text-white/70">
            {item.type === 'file' ? formatSize(item.size) : '—'}
           </td>

          <td className="p-4 text-white/70">
            {formatDate(item.deletedAt)}
           </td>

          <td className="p-4">
            <div className="flex space-x-2">
              <button
                onClick={() => handleRestore(item.id)}
                disabled={actionInProgress}
                className="p-2 bg-green-600 hover:bg-green-700 rounded-lg transition-colors disabled:opacity-50"
                title="Восстановить"
              >
                ↺
              </button>
              <button
                onClick={() => handlePermanentDelete(item.id)}
                disabled={actionInProgress}
                className="p-2 bg-red-600 hover:bg-red-700 rounded-lg transition-colors disabled:opacity-50"
                title="Удалить навсегда"
              >
                ×
              </button>
            </div>
           </td>
         </tr>

        {/* Дочерние элементы */}
        {item.type === 'folder' && isExpanded && hasChildren && (
          item.children.map(child => (
            <TreeItem key={child.id} item={child} level={level + 1} />
          ))
        )}
      </React.Fragment>
    );
  };

  const totalItems = countTotalItems(rootItems);
  const totalSize = calculateTotalSize(rootItems);

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
            </div>
          </div>
        </div>
      </div>

      {/* Сообщения */}
      {error && (
        <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-center flex-shrink-0">
          {error}
          <button onClick={loadTrashHierarchy} className="ml-3 underline hover:no-underline">
            Повторить
          </button>
        </div>
      )}

      {success && (
        <div className="mb-4 p-3 bg-green-500/20 border border-green-500 rounded-xl text-center flex-shrink-0">
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
                checked={selectedItems.size === totalItems && totalItems > 0}
                onChange={handleSelectAll}
                disabled={totalItems === 0 || actionInProgress}
                className="w-4 h-4 rounded border-white/30 bg-white/10 checked:bg-blue-500 focus:ring-blue-500 focus:ring-offset-0"
              />
              <span>Выбрать все</span>
            </label>
            {selectedItems.size > 0 && (
              <span className="text-white/70 text-sm">Выбрано: {selectedItems.size}</span>
            )}
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              onClick={handleRestoreSelected}
              disabled={selectedItems.size === 0 || actionInProgress}
              className="px-4 py-2 bg-green-600 hover:bg-green-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
            >
              <span>↺</span>
              <span>Восстановить выбранные</span>
            </button>

            <button
              onClick={handlePermanentDeleteSelected}
              disabled={selectedItems.size === 0 || actionInProgress}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
            >
              <span>×</span>
              <span>Удалить выбранные</span>
            </button>

            <button
              onClick={handleEmptyTrash}
              disabled={totalItems === 0 || actionInProgress}
              className="px-4 py-2 bg-gray-600 hover:bg-gray-700 rounded-xl font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Очистить корзину
            </button>
          </div>
        </div>
      </div>

      {/* Дерево файлов */}
      <div className="flex-1 overflow-auto bg-white/5 backdrop-blur-sm rounded-2xl p-4">
        {totalItems === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center">
            <div className="text-6xl mb-4 opacity-50">🗑️</div>
            <p className="text-xl mb-2">Корзина пуста</p>
            <p className="text-white/70">Удаленные файлы появятся здесь</p>
          </div>
        ) : (
          <table className="w-full">
            <thead className="sticky top-0 bg-gray-800/95 backdrop-blur-sm z-10">
              <tr className="border-b border-white/10">
                <th className="p-4 text-left w-12"></th>
                <th className="p-4 text-left">Имя</th>
                <th className="p-4 text-left">Тип</th>
                <th className="p-4 text-left">Размер</th>
                <th className="p-4 text-left">Дата удаления</th>
                <th className="p-4 text-left">Действия</th>
              </tr>
            </thead>
            <tbody>
              {rootItems.map(item => (
                <TreeItem key={item.id} item={item} level={0} />
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Нижняя панель с информацией */}
      {totalItems > 0 && (
        <div className="mt-4 pt-4 border-t border-white/10 text-white/60 text-sm flex-shrink-0">
          Всего файлов: {totalItems} |
          Общий размер: {formatSize(totalSize)} |
          Папок: {rootItems.filter(i => i.type === 'folder').length}
        </div>
      )}
    </div>
  );
}