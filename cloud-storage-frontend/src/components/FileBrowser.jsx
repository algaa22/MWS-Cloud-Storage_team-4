// src/components/FileBrowser.jsx
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate } from "react-router-dom";
import {
  getFiles,
  uploadFile as apiUploadFile,
  downloadFile as apiDownloadFile,
  deleteFile as apiDeleteFile,
  renameFile as apiRenameFile,
  getFileInfo as apiGetFileInfo,
  createFolder as apiCreateFolder,
  getUserInfo // Добавляем новый метод
} from "../api.js";

export default function FileBrowser() {
  const { user, logout, token } = useAuth();
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [files, setFiles] = useState([]);
  const [folders, setFolders] = useState([]);
  const [currentPath, setCurrentPath] = useState("");
  const [selectedItem, setSelectedItem] = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showItemMenu, setShowItemMenu] = useState(false);
  const [itemMenuPosition, setItemMenuPosition] = useState({ x: 0, y: 0 });
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showFolderModal, setShowFolderModal] = useState(false);
  const [showInfoModal, setShowInfoModal] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [newFolderName, setNewFolderName] = useState("");
  const [renameText, setRenameText] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [fileInfoData, setFileInfoData] = useState(null);


  // Добавляем состояние для информации о памяти
  const [storageInfo, setStorageInfo] = useState({
    used: 0,
    total: 10 * 1024 * 1024 * 1024, // 10GB по умолчанию
    percentage: 0,
    formattedUsed: '0 Bytes',
    formattedTotal: '10 GB'
  });
  const [storageLoading, setStorageLoading] = useState(true);

  const normalizeCurrentPath = (p) => {
    if (!p || p === "" || p === "/") return "";
    return p.endsWith("/") ? p : p + "/";
  };

  useEffect(() => {
    if (!token) {
      navigate("/login");
      return;
    }
  }, [token, navigate]);

  useEffect(() => {
    console.log("FileBrowser: useEffect for fetching files triggered");
    console.log("Token exists:", !!token);
    console.log("Current path:", currentPath);

    if (!token) {
      console.log("No token, skipping fetchFiles");
      return;
    }

    const loadData = async () => {
      setLoading(true);
      setStorageLoading(true); // <-- Начинаем загрузку хранилища

      try {
        await fetchFiles();
        await loadStorageInfo(); // Ждем завершения
      } catch (error) {
        console.error("Error loading data:", error);
      } finally {
        setLoading(false);
        setStorageLoading(false); // <-- Завершаем загрузку
      }
    };

    console.log("Calling fetchFiles...");
    loadData();
  }, [token, currentPath]);

  // Функция для загрузки информации о хранилище
  const loadStorageInfo = async () => {
    if (!token) return;

    try {
      setStorageLoading(true);
      const userData = await getUserInfo(token);
      console.log("User info loaded:", userData);

      //setUserDetails(userData);

      // Извлекаем информацию о хранилище
      let storageData = {
        used: 0,
        total: 10 * 1024 * 1024 * 1024, // 10GB по умолчанию
      };

      // Проверяем разные варианты получения данных о хранилище
      if (userData.storageInfo) {
        storageData = userData.storageInfo;
      } else if (userData.storage) {
        storageData.used = userData.storage.used || userData.storage.Used || 0;
        storageData.total = userData.storage.total || userData.storage.Total || userData.storage.limit || storageData.total;
      } else if (userData.storageUsed !== undefined || userData.usedStorage !== undefined) {
        storageData.used = userData.storageUsed || userData.usedStorage || userData.used || 0;
        storageData.total = userData.storageTotal || userData.totalStorage || userData.storageLimit || userData.total || storageData.total;
      }

      if (userData.freeSpace !== undefined && userData.storageLimit !== undefined) {
        storageData.used = userData.storageLimit - userData.freeSpace;
        storageData.total = userData.storageLimit;
      }

      // Вычисляем остальные значения
      const percentage = storageData.total > 0 ? Math.round((storageData.used / storageData.total) * 100) : 0;

      setStorageInfo({
        used: storageData.used,
        total: storageData.total,
        percentage,
        formattedUsed: formatFileSize(storageData.used),
        formattedTotal: formatFileSize(storageData.total)
      });

    } catch (err) {
      console.error("Error loading user info:", err);
      // Если ошибка, показываем данные из файлов
      if (files.length > 0) {
        calculateStorageFromFiles();
      }
    } finally {
      setStorageLoading(false);
    }
  };

  // Функция для вычисления занятого места из файлов
  const calculateStorageFromFiles = () => {
    if (files.length === 0 && folders.length === 0) return;

    let totalUsed = 0;
    files.forEach(file => {
      if (file.type === 'file' && file.size) {
        totalUsed += file.size;
      }
    });

    // Применяем лимит из storageInfo или используем значение по умолчанию
    const totalLimit = storageInfo.total || (10 * 1024 * 1024 * 1024); // 10GB по умолчанию
    const percentage = totalLimit > 0 ? Math.round((totalUsed / totalLimit) * 100) : 0;

    setStorageInfo(prev => ({
      ...prev,
      used: totalUsed,
      percentage,
      formattedUsed: formatFileSize(totalUsed)
    }));
  };

  // Обновляем расчет памяти при изменении файлов
  useEffect(() => {
    if (files.length > 0) {
      calculateStorageFromFiles();
    }
  }, [files]);

  const computeFullPathForItem = (item) => {
    if (item.fullPath && typeof item.fullPath === "string" && item.fullPath !== "") {
      return item.fullPath;
    }

    const name = item.name || "";
    const parent = item.path || "";

    if (!parent || parent === "." || parent === "/") {
      return item.type === "folder" ? `${name}/` : `${name}`;
    }

    const parentNormalized = parent.endsWith("/") ? parent : parent + "/";

    return item.type === "folder" ? `${parentNormalized}${name}/` : `${parentNormalized}${name}`;
  };

  const fetchFiles = async () => {
    try {
      setLoading(true);
      console.log("fetchFiles called with path:", currentPath || "(root)");

      if (!token || token.trim() === "") {
        setError("Требуется авторизация");
        setLoading(false);
        return;
      }

      const data = await getFiles(token, currentPath);
      console.log("Raw getFiles data:", data);

      if (!Array.isArray(data)) {
        setError("Некорректный формат данных от сервера");
        setFiles([]);
        setFolders([]);
        return;
      }

      const normalized = data.map((it) => {
        const type = it.type || (it.name && it.name.endsWith("/") ? "folder" : "file");
        const rawName = it.name || "";
        const rawPath = it.path || "";
        const fullPath = computeFullPathForItem({ ...it, type });
        return {
          ...it,
          name: rawName,
          path: rawPath,
          type,
          size: it.size || 0,
          fileCount: it.fileCount || it.count || 0,
          fullPath,
          id: it.id || fullPath
        };
      });

      const filtered = normalized.filter((it) => {
        if (!currentPath) return true;
        return it.fullPath.startsWith(currentPath);
      });

      const foldersList = filtered.filter((it) => it.type === "folder");
      const filesList = filtered.filter((it) => it.type !== "folder");

      console.log("Processed:", foldersList.length, "folders,", filesList.length, "files");
      setFiles(filesList);
      setFolders(foldersList);
      setError("");
    } catch (err) {
      console.error("Error in fetchFiles:", err);
      setError(`Не удалось загрузить файлы: ${err.message}`);
      setFiles([]);
      setFolders([]);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const handleItemClick = (item, event) => {
    console.log("Clicked item:", item);

    if (item.type === "folder") {
      const folderPath = normalizeCurrentPath(item.fullPath);
      setCurrentPath(folderPath);
    } else {
      setSelectedItem(item);
      setShowItemMenu(true);
      setItemMenuPosition({ x: event.clientX, y: event.clientY });
    }
  };

  const getReadableFileType = (mimeType) => {
    const typeMap = {
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Документ Word',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'Таблица Excel',
      'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'Презентация',
      'application/pdf': 'PDF документ',
      'image/jpeg': 'Изображение JPEG',
      'image/png': 'Изображение PNG',
      'image/gif': 'Изображение GIF',
      'text/plain': 'Текстовый файл',
      'application/msword': 'Документ Word',
      'application/vnd.ms-excel': 'Таблица Excel',
      'application/zip': 'Архив ZIP',
      'application/x-rar-compressed': 'Архив RAR',
      'audio/mpeg': 'Аудио MP3',
      'video/mp4': 'Видео MP4',
      'application/octet-stream': 'Бинарный файл'
    };

    return typeMap[mimeType] || mimeType || "—";
  };

  const formatDateForDisplay = (dateString) => {
    if (!dateString) return "—";
    try {
      const date = new Date(dateString);
      return date.toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  };

  const handleFileAction = async (action) => {
    if (!selectedItem) return;

    setShowItemMenu(false);

    try {
      switch (action) {
        case "download":
          await apiDownloadFile(token, selectedItem.fullPath, selectedItem.name, selectedItem.size);
          break;
        case "rename":
          setRenameText(selectedItem.name || "");
          break;
        case "delete":
          if (window.confirm(`Удалить "${selectedItem.name}"?`)) {
            await apiDeleteFile(token, selectedItem.fullPath);
            await fetchFiles();
            await loadStorageInfo(); // Обновляем информацию о памяти
          }
          break;
        case "info":
          const info = await apiGetFileInfo(token, selectedItem.fullPath);
          const formattedInfo = {
            ...info,
            readableType: getReadableFileType(info.mimeType || info.type || info.mime_type),
            formattedDate: formatDateForDisplay(info.updatedAt || info.lastModified || info.modified)
          };
          setFileInfoData(formattedInfo);
          setShowInfoModal(true);
          break;
      }
    } catch (err) {
      console.error("handleFileAction error:", err);
      setError(`Ошибка: ${err.message}`);
    } finally {
      if (action !== "rename") {
        setShowItemMenu(false);
        setSelectedItem(null);
      }
    }
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    console.log("File selected:", {
      name: file.name,
      size: file.size,
      type: file.type
    });

    try {
      setUploading(true);
      setUploadProgress(0);

      const targetPath = currentPath ? `${currentPath}${file.name}` : file.name;
      console.log("Target path:", targetPath);
      console.log("File size before upload:", file.size, "bytes");

      await apiUploadFile(token, file, targetPath, (progress) => {
        console.log("Upload progress:", progress);
        setUploadProgress(progress);
      });

      console.log("Upload completed successfully");
      await fetchFiles();
      await loadStorageInfo(); // Обновляем информацию о памяти
      setShowUploadModal(false);

    } catch (err) {
      console.error("Upload error:", err);
      console.error("Error details:", err.message);
      setError(`Ошибка при загрузке файла: ${err.message}`);
    } finally {
      setUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return;

    try {
      const folderPath = currentPath ? `${currentPath}${newFolderName}/` : `${newFolderName}/`;
      await apiCreateFolder(token, folderPath);
      setNewFolderName("");
      setShowFolderModal(false);
      await fetchFiles();
    } catch (err) {
      console.error("Create folder error:", err);
      setError("Ошибка при создании папки");
    }
  };

  const handleRename = async () => {
    if (!selectedItem || !renameText.trim()) return;

    try {
      const item = selectedItem;
      const isFolder = item.type === "folder";
      const oldFull = item.fullPath;
      let newFull;
      if (isFolder) {
        newFull = oldFull.replace(/[^\/]+\/$/, `${renameText}/`);
      } else {
        newFull = oldFull.replace(/[^\/]+$/, renameText);
      }

      await apiRenameFile(token, oldFull, newFull);
      setRenameText(null);
      setSelectedItem(null);

      await fetchFiles();
    } catch (err) {
      console.error("Rename error:", err);
      setError("Ошибка при переименовании");
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes && bytes !== 0) return "—";
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    const value = parseFloat((bytes / Math.pow(k, i)).toFixed(2));
    const unit = sizes[i];

    return `${value} ${unit}`;
  };

  const formatPercentage = (value) => {
      const roundedUp = Math.ceil(value * 100) / 100;
      let formatted = roundedUp.toFixed(2);

      if (formatted.endsWith('.00')) {
        return formatted.slice(0, -3);
      }

      formatted = formatted.replace(/(\.\d)0$/, '$1');

      return formatted;
  };

  const getFileIcon = (fileName) => {
    const extension = (fileName || "").split(".").pop().toLowerCase();
    const iconMap = {
      pdf: "📄",
      jpg: "🖼️",
      jpeg: "🖼️",
      png: "🖼️",
      gif: "🖼️",
      txt: "📝",
      doc: "📄",
      docx: "📄",
      xls: "📊",
      xlsx: "📊",
      ppt: "📽️",
      pptx: "📽️",
      zip: "📦",
      rar: "📦",
      mp3: "🎵",
      mp4: "🎬",
      avi: "🎬",
      mkv: "🎬"
    };
    return iconMap[extension] || "📄";
  };

  const renderBreadcrumbs = () => {
    if (!currentPath) return null;
    const trimmed = currentPath.replace(/\/$/, "");
    const parts = trimmed.split("/").filter(Boolean);
    return parts.map((part, index, arr) => {
      const label = part;
      const pathTo = arr.slice(0, index + 1).join("/") + "/";
      return (
          <React.Fragment key={index}>
            <span className="mx-2">/</span>
            <button
                onClick={() => setCurrentPath(normalizeCurrentPath(pathTo))}
                className="text-white/70 hover:text-white"
            >
              {label}
            </button>
          </React.Fragment>
      );
    });
  };

  // Функция для получения цвета прогресс-бара в зависимости от заполненности
  const getProgressBarColor = (percentage) => {
    if (percentage < 50) return 'bg-green-500';
    if (percentage < 75) return 'bg-yellow-500';
    if (percentage < 90) return 'bg-orange-500';
    return 'bg-red-500';
  };

  return (
      <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-4">

        {/* Верхняя панель с заголовком и пользователем в одну строку */}
        <header className="flex justify-between items-center mb-10">
          {/* Заголовок слева */}
          {/* Пустой блок для балансировки слева */}
          <div className="w-32"></div>

          {/* Заголовок по центру */}
          <div className="text-center">
            <h1 className="text-3xl md:text-4xl font-bold text-white">
              MWS Cloud Storage
            </h1>
            <p className="text-white/60 text-sm mt-1">Безопасное хранение файлов</p>
          </div>

          {/* Пользователь справа */}
          <div className="relative">
            <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center space-x-3 bg-white/10 hover:bg-white/15 backdrop-blur-sm rounded-xl px-4 py-2.5 transition-all duration-200 border border-white/10 hover:border-white/20"
            >
              {/* Иконка с первой буквой */}
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500/90 to-purple-500/90 rounded-full flex items-center justify-center text-white font-bold shadow-md">
                {((user?.name || user?.username || user?.email || "U").charAt(0)).toUpperCase()}
              </div>
              {/* Имя пользователя */}
              <div className="text-left">
            <span className="font-medium text-white text-sm block">
              {user?.name || user?.username || user?.email?.split('@')[0] || "Пользователь"}
            </span>
                {user?.email && <span className="text-white/50 text-xs block">{user.email}</span>}
              </div>
              <svg className="w-4 h-4 text-white/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            {showUserMenu && (
                <div className="absolute right-0 mt-2 w-72 bg-gray-800/95 backdrop-blur-xl rounded-xl shadow-2xl py-3 z-50 border border-white/10">
                  <div className="px-4 py-3 border-b border-white/10">
                    {/* Иконка и имя в меню */}
                    <div className="flex items-center space-x-3 mb-3">
                      <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-500 rounded-full flex items-center justify-center text-white font-bold text-lg shadow-lg">
                        {((user?.name || user?.username || user?.email || "U").charAt(0)).toUpperCase()}
                      </div>
                      <div>
                        <p className="font-semibold text-white">
                          {user?.name || user?.username || user?.email?.split('@')[0] || "Пользователь"}
                        </p>
                        {user?.email && <p className="text-white/60 text-xs mt-0.5">{user.email}</p>}
                      </div>
                    </div>
                    {/* Информация о хранилище */}
                    <div className="space-y-2">
                      <div className="flex justify-between text-sm">
                        <span className="text-white/80">Хранилище:</span>
                        <span className="text-blue-300 font-medium">{storageInfo.formattedUsed}</span>
                      </div>
                      <div className="w-full bg-gray-700/60 rounded-full h-2 overflow-hidden">
                        <div
                            className={`h-2 rounded-full transition-all duration-300 ${getProgressBarColor(storageInfo.percentage)}`}
                            style={{ width: `${Math.min(storageInfo.percentage, 100)}%` }}
                        />
                      </div>
                      <div className="flex justify-between text-xs text-white/50">
                        <span>Лимит: {storageInfo.formattedTotal}</span>
                        <span>Осталось: {formatFileSize(storageInfo.total - storageInfo.used)}</span>
                      </div>
                    </div>
                  </div>

                  {/* Кнопка настроек профиля */}
                  <button
                      onClick={() => {
                        setShowUserMenu(false);
                        navigate("/settings");
                      }}
                      className="flex items-center w-full text-left px-4 py-2.5 hover:bg-white/10 transition-colors border-t border-white/10"
                  >
                    <svg className="w-4 h-4 mr-3 text-blue-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    <span className="text-sm">Настройки профиля</span>
                  </button>

                  <button
                      onClick={handleLogout}
                      className="flex items-center w-full text-left px-4 py-2.5 hover:bg-white/10 transition-colors text-red-300 border-t border-white/10"
                  >
                    <svg className="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    <span className="text-sm">Выйти</span>
                  </button>
                </div>
            )}
          </div>
        </header>

        {/* Панель информации о памяти */}
        <div className="mb-6 bg-white/10 backdrop-blur-sm rounded-xl p-4">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div className="flex-1">
              <div className="flex items-center justify-between mb-2">
                <div className="text-lg font-medium">Ваше хранилище</div>
                <div className="text-sm">
                  {storageLoading ? (
                      <span className="text-white/70 animate-pulse">Загрузка...</span>
                  ) : (
                      <>
                        <span className="text-blue-300">{storageInfo.formattedUsed}</span>
                        <span className="text-white/60"> / </span>
                        <span>{storageInfo.formattedTotal}</span>
                      </>
                  )}
                </div>
              </div>

              {/* Основной прогресс-бар */}
              <div className="w-full bg-gray-700/50 rounded-full h-3 overflow-hidden">
                {storageLoading ? (
                    <div className="h-3 bg-gradient-to-r from-blue-500/30 to-purple-500/30 animate-pulse rounded-full w-full"></div>
                ) : (
                    <div
                        className={`h-3 rounded-full transition-all duration-500 ${getProgressBarColor(storageInfo.percentage)}`}
                        style={{ width: `${Math.min(storageInfo.percentage, 100)}%` }}
                    />
                )}
              </div>

              <div className="flex justify-between text-sm mt-1">
                <span className="text-white/60">
          {storageLoading ? "..." : `Осталось: ${formatFileSize(storageInfo.total - storageInfo.used)}`}
        </span>
              </div>
            </div>

            <div className="flex items-center space-x-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-300">
                  {storageLoading ? "0" : formatPercentage(storageInfo.used / storageInfo.total)}%
                </div>
                <div className="text-xs text-white/60">использовано</div>
              </div>

              <div className="hidden md:block h-8 w-px bg-white/20" />

              <div className="text-center">
                <div className="text-xl font-bold">{files.length + folders.length}</div>
                <div className="text-xs text-white/60">файлов и папок</div>
              </div>
            </div>
          </div>
        </div>

        {/* Основная область с файлами */}
        <div className="bg-white/10 backdrop-blur-xl rounded-2xl p-6 min-h-[60vh] mb-6">
          {error && (
              <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-center">
                {error}
              </div>
          )}

          {loading ? (
              <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-white"></div>
                <span className="ml-3">Загрузка файлов...</span>
              </div>
          ) : files.length === 0 && folders.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 text-center">
                <div className="text-6xl mb-4 opacity-50">📁</div>
                <p className="text-xl mb-2">Папка пуста</p>
                <p className="text-white/70">Загрузите файл или создайте папку</p>

                {/* Информация о памяти для пустой папки */}
                {storageInfo.percentage > 0 && (
                    <div className="mt-6 bg-white/5 rounded-xl p-4 max-w-md w-full">
                      <div className="text-sm text-white/60 mb-2">Использование хранилища:</div>
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm">{storageInfo.formattedUsed}</span>
                        <span className="text-sm">{storageInfo.formattedTotal}</span>
                      </div>
                      <div className="w-full bg-gray-700/50 rounded-full h-2">
                        <div
                            className="h-2 rounded-full bg-blue-500 transition-all duration-300"
                            style={{ width: `${Math.min(storageInfo.percentage, 100)}%` }}
                        />
                      </div>
                    </div>
                )}
              </div>
          ) : (
              <>
                {/* Статистика текущей папки */}
                <div className="mb-6 p-4 bg-white/5 rounded-xl">
                  <div className="flex flex-wrap gap-6">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-blue-300">{folders.length}</div>
                      <div className="text-sm text-white/60">Папок</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-green-300">{files.length}</div>
                      <div className="text-sm text-white/60">Файлов</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-yellow-300">
                        {storageLoading ? "..." : formatFileSize(storageInfo.used)}
                      </div>
                      <div className="text-sm text-white/60">Использовано памяти:</div>
                    </div>
                  </div>
                </div>

                {/* Сетка файлов и папок */}
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
                  {/* Папки */}
                  {folders.map((folder) => (
                      <div
                          key={folder.id || folder.fullPath}
                          onClick={(e) => handleItemClick(folder, e)}
                          className="bg-white/5 hover:bg-white/10 rounded-xl p-4 cursor-pointer transition-all hover:scale-105 group"
                      >
                        <div className="text-4xl mb-2 group-hover:scale-110 transition-transform">📁</div>
                        <p className="truncate text-sm font-medium">
                          {(() => {
                            const path = folder.fullPath || "";
                            const parts = path.split("/").filter(p => p);
                            return parts.length > 0 ? parts[parts.length - 1] : "Папка";
                          })()}
                        </p>
                        {folder.fileCount ? (
                            <p className="text-xs text-white/50 mt-1">{folder.fileCount} файлов</p>
                        ) : (
                            <p className="text-xs text-white/30 mt-1">Папка</p>
                        )}
                      </div>
                  ))}

                  {/* Файлы */}
                  {files.map((file) => (
                      <div
                          key={file.id || file.fullPath}
                          onClick={(e) => handleItemClick(file, e)}
                          onContextMenu={(e) => {
                            e.preventDefault();
                            setSelectedItem(file);
                            setShowItemMenu(true);
                            setItemMenuPosition({ x: e.clientX, y: e.clientY });
                          }}
                          className="bg-white/5 hover:bg-white/10 rounded-xl p-4 cursor-pointer transition-all hover:scale-105 group"
                      >
                        <div className="text-4xl mb-2 group-hover:scale-110 transition-transform">
                          {getFileIcon(file.name)}
                        </div>
                        <p className="truncate text-sm font-medium">{file.name}</p>
                        <p className="text-xs text-white/50 mt-1">{formatFileSize(file.size)}</p>
                      </div>
                  ))}
                </div>
              </>
          )}
        </div>

        {/* Кнопки внизу */}
        <div className="fixed bottom-6 left-1/2 transform -translate-x-1/2 flex space-x-4">
          <button
              onClick={() => setShowUploadModal(true)}
              disabled={uploading}
              className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-xl font-medium transition-colors flex items-center space-x-2 disabled:opacity-50 shadow-lg"
          >
            {uploading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
                  <span>{uploadProgress}%</span>
                </>
            ) : (
                <>
                  <span>📤</span>
                  <span>Загрузить файл</span>
                </>
            )}
          </button>

          <button
              onClick={() => setShowFolderModal(true)}
              className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-xl font-medium transition-colors flex items-center space-x-2 shadow-lg"
          >
            <span>📁</span>
            <span>Создать папку</span>
          </button>
        </div>


        {/* Модальное окно загрузки с информацией о памяти */}
        {showUploadModal && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
                <h3 className="text-xl font-bold mb-4">Загрузить файл</h3>

                {/* Информация о доступном месте */}
                <div className="mb-4 p-3 bg-white/10 rounded-xl">
                  <div className="flex justify-between text-sm mb-1">
                    <span>Доступно:</span>
                    <span className="font-medium text-green-300">
              {storageLoading ? "..." : formatFileSize(storageInfo.total - storageInfo.used)}
            </span>
                  </div>
                  <div className="flex justify-between text-xs text-white/60 mb-2">
                    <span>Использовано: {storageLoading ? "..." : storageInfo.formattedUsed}</span>
                    <span>Лимит: {storageLoading ? "..." : storageInfo.formattedTotal}</span>
                  </div>
                  <div className="w-full bg-gray-700 rounded-full h-2">
                    {storageLoading ? (
                        <div className="h-2 bg-gradient-to-r from-blue-500/30 to-green-500/30 animate-pulse rounded-full w-full"></div>
                    ) : (
                        <div
                            className={`h-2 rounded-full ${getProgressBarColor(storageInfo.percentage)}`}
                            style={{ width: `${formatPercentage(storageInfo.used / storageInfo.total)}%` }}
                        />
                    )}
                  </div>
                </div>

                <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleFileUpload}
                    className="w-full mb-4 p-3 bg-white/20 rounded-xl cursor-pointer hover:bg-white/30 transition-colors"
                />
                {uploading && (
                    <div className="w-full bg-gray-700 rounded-full h-2 mb-4">
                      <div className="bg-blue-500 h-2 rounded-full transition-all duration-300" style={{ width: `${uploadProgress}%` }} />
                    </div>
                )}
                <div className="flex justify-end space-x-3">
                  <button
                      onClick={() => setShowUploadModal(false)}
                      className="px-4 py-2 rounded-xl bg-white/20 hover:bg-white/30 transition-colors"
                      disabled={uploading || storageLoading}
                  >
                    Отмена
                  </button>
                  <button
                      onClick={() => fileInputRef.current?.click()}
                      className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 transition-colors"
                      disabled={uploading || storageLoading}
                  >
                    Выбрать файл
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* Create folder modal */}
        {showFolderModal && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
                <h3 className="text-xl font-bold mb-4">Создать папку</h3>
                <input type="text" placeholder="Название папки" value={newFolderName} onChange={(e) => setNewFolderName(e.target.value)} className="w-full p-3 rounded-xl bg-white/20 mb-4 text-white" />
                <div className="flex justify-end space-x-3">
                  <button onClick={() => setShowFolderModal(false)} className="px-4 py-2 rounded-xl bg-white/20 hover:bg-white/30">
                    Отмена
                  </button>
                  <button onClick={handleCreateFolder} className="px-4 py-2 rounded-xl bg-green-600 hover:bg-green-700">
                    Создать
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* File Info Modal */}
        {showInfoModal && fileInfoData && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gradient-to-br from-gray-900 to-blue-900 rounded-2xl p-6 w-full max-w-md border border-white/10 shadow-2xl">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-xl font-bold text-white flex items-center">
                    <span className="mr-2">📄</span>
                    Информация о файле
                  </h3>
                  <button
                      onClick={() => {
                        setShowInfoModal(false);
                        setFileInfoData(null);
                      }}
                      className="text-white/70 hover:text-white text-2xl bg-white/10 w-8 h-8 rounded-full flex items-center justify-center"
                  >
                    ×
                  </button>
                </div>

                <div className="space-y-4">
                  {/* File header */}
                  <div className="flex items-center space-x-4 p-4 bg-white/10 rounded-xl">
                    <div className="text-4xl">{getFileIcon(fileInfoData.name)}</div>
                    <div className="flex-1 min-w-0">
                      <div className="font-bold text-lg truncate">{fileInfoData.name}</div>
                      <div className="text-sm text-white/60 truncate">{fileInfoData.path}</div>
                    </div>
                  </div>

                  {/* Info grid */}
                  <div className="grid grid-cols-2 gap-3">
                    <div className="bg-white/10 p-3 rounded-xl">
                      <div className="text-sm text-white/60 mb-1">Размер</div>
                      <div className="font-medium">{formatFileSize(fileInfoData.size)}</div>
                    </div>

                    <div className="bg-white/10 p-3 rounded-xl">
                      <div className="text-sm text-white/60 mb-1">Тип файла</div>
                      <div className="font-medium">{fileInfoData.readableType}</div>
                    </div>

                    <div className="bg-white/10 p-3 rounded-xl">
                      <div className="text-sm text-white/60 mb-1">Видимость</div>
                      <div className="font-medium">
                    <span className={fileInfoData.visibility === 'public' ? 'text-green-400' : 'text-blue-400'}>
                      {fileInfoData.visibility === 'public' ? 'Публичный' : 'Приватный'}
                    </span>
                      </div>
                    </div>

                    <div className="bg-white/10 p-3 rounded-xl">
                      <div className="text-sm text-white/60 mb-1">Дата изменения</div>
                      <div className="font-medium">{fileInfoData.formattedDate}</div>
                    </div>
                  </div>

                  {/* Tags */}
                  {fileInfoData.tags && (
                      <div className="bg-white/10 p-3 rounded-xl">
                        <div className="text-sm text-white/60 mb-2">Теги</div>
                        <div className="flex flex-wrap gap-2">
                          {fileInfoData.tags.split(',').map((tag, idx) => (
                              <span key={idx} className="bg-blue-500/30 text-blue-300 px-3 py-1 rounded-full text-sm">
                        {tag.trim()}
                      </span>
                          ))}
                        </div>
                      </div>
                  )}

                  {/* Additional info */}
                  <div className="bg-white/10 p-3 rounded-xl">
                    <div className="text-sm text-white/60 mb-2">Дополнительно</div>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div>MIME-тип:</div>
                      <div className="text-white/80">{fileInfoData.mimeType || fileInfoData.type || "—"}</div>

                      {fileInfoData.isolated !== undefined && (
                          <>
                            <div>Изолирован:</div>
                            <div className={fileInfoData.isolated ? 'text-yellow-400' : 'text-green-400'}>
                              {fileInfoData.isolated ? 'Да' : 'Нет'}
                            </div>
                          </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="mt-6 flex justify-end">
                  <button
                      onClick={() => {
                        setShowInfoModal(false);
                        setFileInfoData(null);
                      }}
                      className="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 rounded-xl font-medium transition-colors flex items-center"
                  >
                    <span className="mr-2">✓</span>
                    Закрыть
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* Context menu */}
        {showItemMenu && selectedItem && (
            <>
              <div className="fixed inset-0 z-40" onClick={() => setShowItemMenu(false)} />
              <div className="fixed bg-gray-800 rounded-xl shadow-2xl py-2 z-50 min-w-[200px]" style={{ left: itemMenuPosition.x, top: itemMenuPosition.y }}>
                <button onClick={() => handleFileAction("download")} className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors">📥 Скачать</button>
                <button onClick={() => handleFileAction("rename")} className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors">✏️ Переименовать</button>
                <button onClick={() => handleFileAction("info")} className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors">ℹ️ Информация</button>
                <div className="border-t border-white/20 my-1" />
                <button onClick={() => handleFileAction("delete")} className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors text-red-300">🗑️ Удалить</button>
              </div>
            </>
        )}

        {/* Rename modal */}
        {selectedItem && renameText !== null && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
                <h3 className="text-xl font-bold mb-4">Переименовать</h3>
                <input type="text" value={renameText} onChange={(e) => setRenameText(e.target.value)} className="w-full p-3 rounded-xl bg-white/20 mb-4 text-white" />
                <div className="flex justify-end space-x-3">
                  <button onClick={() => {
                    setRenameText(null);
                    setSelectedItem(null);
                  }} className="px-4 py-2 rounded-xl bg-white/20 hover:bg-white/30">
                    Отмена
                  </button>
                  <button onClick={handleRename} className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700">Сохранить</button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}