import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../AuthContext";
import { useNavigate } from "react-router-dom";

import {
  getFiles,
  downloadFile as apiDownloadFile,
  deleteFile as apiDeleteFile,
  getFileInfo as apiGetFileInfo,
  createFolder as apiCreateFolder,
  deleteFolder as apiDeleteFolder,
  getUserInfo,
  getFileTags,
  getAllUserTags,
  uploadFileWithTags,
  updateFileMetadata,
  moveFolder,
  BASE,
  fetchWithTokenRefresh
} from "../api.js";

export default function FileBrowser() {
  const { user, logout, token } = useAuth();
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const [success, setSuccess] = useState("");
  const [files, setFiles] = useState([]);
  const [folders, setFolders] = useState([]);
  const [currentPath, setCurrentPath] = useState("");
  const [selectedItem, setSelectedItem] = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showItemMenu, setShowItemMenu] = useState(false);
  const [itemMenuPosition, setItemMenuPosition] = useState({ x: 0, y: 0 });
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showFolderModal, setShowFolderModal] = useState(false);
  const [navHistory, setNavHistory] = useState([]);
  const [showInfoModal, setShowInfoModal] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [newFolderName, setNewFolderName] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [fileInfoData, setFileInfoData] = useState(null);
  const [uploadTags, setUploadTags] = useState([]);
  const [fileTags, setFileTags] = useState([]);
  const [availableTags, setAvailableTags] = useState([]);
  const [editingFileName, setEditingFileName] = useState(false);
  const [newFileName, setNewFileName] = useState("");
  const [fileVisibility, setFileVisibility] = useState('private');
  const [isSavingChanges, setIsSavingChanges] = useState(false);
  const [renameText, setRenameText] = useState("");
  const [searchTags, setSearchTags] = useState([]);
  const [searchInput, setSearchInput] = useState("");
  const [isSearching, setIsSearching] = useState(false);

  const [storageInfo, setStorageInfo] = useState({
    used: 0,
    total: 10 * 1024 * 1024 * 1024,
    percentage: 0,
    formattedUsed: '0 Bytes',
    formattedTotal: '10 GB'
  });
  const [storageLoading, setStorageLoading] = useState(true);

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
      setStorageLoading(true);

      try {
        await fetchFiles();
        await loadStorageInfo();
      } catch (error) {
        console.error("Error loading data:", error);
      } finally {
        setLoading(false);
        setStorageLoading(false);
      }
    };

    console.log("Calling fetchFiles...");
    loadData();
  }, [token, currentPath]);

  const handleAddSearchTag = () => {
    if (searchInput.trim() && !searchTags.includes(searchInput.trim())) {
      setSearchTags([...searchTags, searchInput.trim()]);
      setSearchInput("");
    }
  };

  // Функция удаления тега
  const handleRemoveSearchTag = (tagToRemove) => {
    setSearchTags(searchTags.filter(tag => tag !== tagToRemove));
  };

  const handleClearSearchTags = () => {
    setSearchTags([]);
    fetchFiles();
  };

  const handleSearchByTags = async () => {
    if (searchTags.length === 0) {
      await fetchFiles();
      return;
    }

    setIsSearching(true);
    try {
      const params = new URLSearchParams();
      params.append("includeDirectories", "true");

      searchTags.forEach(tag => {
        params.append("tags", tag);
      });

      const url = `${BASE}/files/list?${params.toString()}`;
      console.log("Search URL:", url);

      const response = await fetchWithTokenRefresh(url, {
        headers: {
          "Accept": "application/json"
        }
      }, token);

      if (!response.ok) {
        throw new Error(`Search failed: ${response.status}`);
      }

      const data = await response.json();
      const items = data?.files || data || [];

      if (!Array.isArray(items)) {
        setFiles([]);
        setFolders([]);
        return;
      }

      const processed = items.map(item => ({
        name: item.name || "Без имени",
        type: item.isDirectory ? "folder" : "file",
        size: item.size || 0,
        id: item.id,
        _raw: item
      }));

      setFiles(processed.filter(f => f.type === "file"));
      setFolders(processed.filter(f => f.type === "folder"));

    } catch (error) {
      console.error("Search error:", error);
      setError("Ошибка при поиске по тегам");
    } finally {
      setIsSearching(false);
    }
  };

  const loadStorageInfo = async () => {
    if (!token) return;

    try {
      setStorageLoading(true);
      const userData = await getUserInfo(token);
      console.log("User info loaded:", userData);

      let storageData = {
        used: 0,
        total: 10 * 1024 * 1024 * 1024,
      };

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
      if (files.length > 0) {
        calculateStorageFromFiles();
      }
    } finally {
      setStorageLoading(false);
    }
  };

  const calculateStorageFromFiles = () => {
    if (files.length === 0 && folders.length === 0) return;

    let totalUsed = 0;
    files.forEach(file => {
      if (file.type === 'file' && file.size) {
        totalUsed += file.size;
      }
    });

    const totalLimit = storageInfo.total || (10 * 1024 * 1024 * 1024);
    const percentage = totalLimit > 0 ? Math.round((totalUsed / totalLimit) * 100) : 0;

    setStorageInfo(prev => ({
      ...prev,
      used: totalUsed,
      percentage,
      formattedUsed: formatFileSize(totalUsed)
    }));
  };

  useEffect(() => {
    if (files.length > 0) {
      calculateStorageFromFiles();
    }
  }, [files]);

  const getItemName = (item) => {
    if (item.name && item.name !== "") {
      return item.name;
    }
    return item.id || "Объект";
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
        return {
          ...it,
          name: it.name || "",
          type: it.type,
          fileCount: it.fileCount || it.count || 0,
          tags: Array.isArray(it.tags) ? it.tags.join(',') : (it.tags || ""),
          fullPath: it.name || ""
        };
      });

      console.log("Normalized data:", normalized);

      const foldersList = normalized.filter((it) => it.type === "folder");
      const filesList = normalized.filter((it) => it.type === "file");

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

  const handleGoHome = () => {
    setCurrentPath("");
    setNavHistory([]);
  };

  const handleGoBack = () => {
    if (navHistory.length > 1) {
      const newHistory = navHistory.slice(0, -1);
      const prevFolder = newHistory[newHistory.length - 1];
      setNavHistory(newHistory);
      setCurrentPath(prevFolder ? prevFolder.id : "");
    } else {
      setNavHistory([]);
      setCurrentPath("");
    }
  };


  const handleItemClick = (item, event) => {
    console.log("Clicked item:", item);

   if (item.type === "folder") {
     setNavHistory(prev => [...prev, { id: item.id, name: item.name }]);
     setCurrentPath(item.id);
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

  const handleDeleteFolder = async (folder) => {
    if (!folder || !folder.id) return;

    const confirmMessage = folder.fileCount
      ? `Удалить папку "${getItemName(folder)}" с ${folder.fileCount} файлами? Это действие нельзя отменить.`
      : `Удалить папку "${getItemName(folder)}"?`;

    if (window.confirm(confirmMessage)) {
      try {
        await apiDeleteFolder(token, folder.id);
        await fetchFiles();
        await loadStorageInfo();
        setError("");
      } catch (err) {
        console.error("Delete folder error:", err);
        setError(`Ошибка при удалении папки: ${err.message}`);
      }
    }
  };

  const handleFileAction = async (action) => {
    if (!selectedItem) return;

    setShowItemMenu(false);

    try {
      switch (action) {
        case "download":
          if (selectedItem.type === "file") {
            await apiDownloadFile(token, selectedItem.id, selectedItem.name, selectedItem.size);
          } else {
            setError("Папки нельзя скачать");
          }
          break;
        case "rename":
          setRenameText(selectedItem.name || "");
          break;
        case "delete":
          const itemType = selectedItem.type === "folder" ? "папку" : "файл";
          const itemName = getItemName(selectedItem);

          if (window.confirm(`Удалить ${itemType} "${itemName}"?`)) {
            if (selectedItem.type === "folder") {
              await apiDeleteFolder(token, selectedItem.id);
            } else {
              await apiDeleteFile(token, selectedItem.id, false);
            }
            await fetchFiles();
            await loadStorageInfo();
          }
          break;
        case "info":
          const info = selectedItem.type === "file"
            ? await apiGetFileInfo(token, selectedItem.id)
            : {
                name: selectedItem.name,
                id: selectedItem.id,
                type: "folder",
                size: 0,
                visibility: "private",
                isDirectory: true
              };

          let fileTagsList = [];
          if (selectedItem.type === "file") {
            try {
              const tagsData = await getFileTags(token, selectedItem.id);
              fileTagsList = tagsData.tags || [];
            } catch (err) {
              console.log("Не удалось загрузить теги, используем пустой массив");
              fileTagsList = [];
            }
          }

          await loadAvailableTags();

          setFileInfoData({
            ...info,
            readableType: selectedItem.type === "file"
              ? getReadableFileType(info.mimeType || info.type || info.mime_type)
              : "Папка",
            formattedDate: formatDateForDisplay(info.updatedAt || info.lastModified || info.modified),
            item: selectedItem,
            tags: fileTagsList,
            visibility: info.visibility || info.Visibility || "private"
          });

          setEditingFileName(false);
          setNewFileName(selectedItem.name || "");
          setFileTags(fileTagsList);
          setFileVisibility(info.visibility || info.Visibility || "private");

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

  const loadAvailableTags = async () => {
    try {
      const tags = await getAllUserTags(token);
      setAvailableTags(tags);
      return tags;
    } catch (error) {
      console.error("Failed to load tags:", error);
      setAvailableTags([]);
      return [];
    }
  };

  const handleSaveFileName = async () => {
    if (!fileInfoData || !fileInfoData.item || !newFileName.trim() || newFileName === fileInfoData.name) {
      return;
    }

    setIsSavingChanges(true);
    setError("");

    try {
      const item = fileInfoData.item;

      const metadataUpdates = {
        newName: newFileName.trim()
      };

      if (item.type === "folder") {
        await moveFolder(token, item.id, { newName: newFileName.trim() });
      } else {
        await updateFileMetadata(token, item.id, metadataUpdates);
      }

      setFileInfoData({
        ...fileInfoData,
        name: newFileName
      });

      await fetchFiles();
      await loadStorageInfo();

      setSuccess(`Файл переименован в "${newFileName}"`);
      setEditingFileName(false);

      setTimeout(() => {
        setSuccess("");
      }, 2000);

    } catch (error) {
      console.error("Failed to save file name:", error);
      setError(`Ошибка переименования: ${error.message}`);
    } finally {
      setIsSavingChanges(false);
    }
  };

  const handleSaveVisibility = async (visibility) => {
    if (!fileInfoData || !fileInfoData.item || fileInfoData.item.type !== "file") {
      return;
    }

    setIsSavingChanges(true);
    setError("");

    try {
      const item = fileInfoData.item;

      const metadataUpdates = {
        visibility: visibility
      };

      await updateFileMetadata(token, item.id, metadataUpdates);

      setFileInfoData({
        ...fileInfoData,
        visibility: visibility
      });

      setSuccess(`Видимость изменена на ${visibility === 'public' ? 'публичный' : 'приватный'}`);

      setTimeout(() => {
        setSuccess("");
      }, 2000);

    } catch (error) {
      console.error("Failed to save visibility:", error);
      setError(`Ошибка изменения видимости: ${error.message}`);
      setFileVisibility(fileInfoData.visibility || 'private');
    } finally {
      setIsSavingChanges(false);
    }
  };

  const handleSaveTags = async (tags) => {
    if (!fileInfoData || !fileInfoData.item || fileInfoData.item.type !== "file") {
      return;
    }

    setIsSavingChanges(true);
    setError("");

    try {
      const item = fileInfoData.item;

      const metadataUpdates = {
        tags: tags
      };

      await updateFileMetadata(token, item.id, metadataUpdates);

      setFileInfoData({
        ...fileInfoData,
        tags: tags
      });

      setSuccess(`Теги обновлены`);

      setTimeout(() => {
        setSuccess("");
      }, 2000);

    } catch (error) {
      console.error("Failed to save tags:", error);
      setError(`Ошибка сохранения тегов: ${error.message}`);
      setFileTags(fileInfoData.tags || []);
    } finally {
      setIsSavingChanges(false);
    }
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      setUploading(true);
      setUploadProgress(0);

      const parentId = currentPath || null;

      await uploadFileWithTags(token, file, parentId, (progress) => {
        setUploadProgress(progress);
      }, uploadTags);

      console.log("Upload completed successfully");
      await fetchFiles();
      await loadStorageInfo();
      setShowUploadModal(false);
      setUploadTags([]);

    } catch (err) {
      console.error("Upload error:", err);
      setError(`Ошибка при загрузке файла: ${err.message}`);
    } finally {
      setUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const renderModernNavigation = () => {
    return (
      <div className="mb-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div className="flex-1">
            <div className="flex items-center flex-wrap gap-2">
              <button
                onClick={handleGoHome}
                className="flex items-center bg-white/10 hover:bg-white/20 text-white px-4 py-2.5 rounded-xl transition-all duration-200 group"
              >
                <svg className="w-5 h-5 mr-2 group-hover:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
                <span className="font-medium">Главная</span>
              </button>

              {navHistory.length > 0 && (
                <div className="flex items-center flex-wrap gap-1">
                  {navHistory.map((folder, index) => {
                    const isLast = index === navHistory.length - 1;

                    return (
                      <React.Fragment key={folder.id}>
                        <div className="text-white/30 mx-1">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                          </svg>
                        </div>

                        {isLast ? (
                          <div className="flex items-center bg-gradient-to-r from-blue-500/20 to-purple-500/20 text-white px-4 py-2.5 rounded-xl border border-white/10">
                            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 19a2 2 0 01-2-2V7a2 2 0 012-2h4l2-2h4l2 2h4a2 2 0 012 2v10a2 2 0 01-2 2H5z" />
                            </svg>
                            <span className="font-medium">{folder.name}</span>
                          </div>
                        ) : (
                          <button
                            onClick={() => {
                              // Переходим к выбранной папке
                              const newHistory = navHistory.slice(0, index + 1);
                              setNavHistory(newHistory);
                              setCurrentPath(folder.id);
                            }}
                            className="flex items-center bg-white/5 hover:bg-white/10 text-white/90 hover:text-white px-4 py-2.5 rounded-xl transition-all duration-200 group"
                          >
                            <svg className="w-5 h-5 mr-2 group-hover:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 19a2 2 0 01-2-2V7a2 2 0 012-2h4l2-2h4l2 2h4a2 2 0 012 2v10a2 2 0 01-2 2H5z" />
                            </svg>
                            <span className="font-medium">{folder.name}</span>
                          </button>
                        )}
                      </React.Fragment>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2">
            {navHistory.length > 0 && (
              <button
                onClick={handleGoBack}
                className="flex items-center bg-white/10 hover:bg-white/20 text-white px-4 py-2.5 rounded-xl transition-all duration-200 group"
                title="Вернуться назад"
              >
                <svg className="w-5 h-5 mr-2 group-hover:-translate-x-0.5 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
                <span className="font-medium">Назад</span>
              </button>
            )}
          </div>
        </div>
      </div>
    );
  };

  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return;

    try {
      const parentId = currentPath || null;
      await apiCreateFolder(token, newFolderName.trim(), parentId);
      setNewFolderName("");
      setShowFolderModal(false);
      await fetchFiles();
    } catch (err) {
      console.error("Create folder error:", err);
      setError("Ошибка при создании папки");
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
    value *= 100;
    const roundedUp = Math.ceil(value * 100) / 100;
    let formatted = roundedUp.toFixed(2);

    if (formatted.endsWith('.00')) {
      return formatted.slice(0, -3);
    }

    formatted = formatted.replace(/(\.\d)0$/, '$1');

    return formatted;
  };

  const getFileIcon = (item) => {
    if (item.type === "folder" || item.isDirectory) {
      return "📁";
    }

    const fileName = item.name || "";
    const extension = fileName.split(".").pop().toLowerCase();

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

  const getProgressBarColor = (percentage) => {
    if (percentage < 50) return 'bg-green-500';
    if (percentage < 75) return 'bg-yellow-500';
    if (percentage < 90) return 'bg-orange-500';
    return 'bg-red-500';
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-4">

      <header className="flex justify-between items-center mb-10">
        <div className="w-32"></div>

        <div className="text-center">
          <h1 className="text-3xl md:text-4xl font-bold text-white">
            MWS Cloud Storage
          </h1>
          <p className="text-white/60 text-sm mt-1">Ваше персональное облако</p>
        </div>

        <div className="relative">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="flex items-center space-x-3 bg-white/10 hover:bg-white/15 backdrop-blur-sm rounded-xl px-4 py-2.5 transition-all duration-200 border border-white/10 hover:border-white/20"
          >
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500/90 to-purple-500/90 rounded-full flex items-center justify-center text-white font-bold shadow-md">
              {((user?.name || user?.username || user?.email || "U").charAt(0)).toUpperCase()}
            </div>
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
                    <div className="flex items-center space-x-3">
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
                  </div>

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
                    onClick={() => {
                      setShowUserMenu(false);
                      navigate("/tariffs");
                    }}
                    className="flex items-center w-full text-left px-4 py-2.5 hover:bg-white/10 transition-colors border-t border-white/10"
                  >
                    <svg className="w-4 h-4 mr-3 text-yellow-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span className="text-sm">Тарифы и оплата</span>
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

            <div className="flex items-center space-x-6">
              <div className="text-center">
                <div className="text-xl font-bold text-blue-300">{folders.length}</div>
                <div className="text-xs text-white/60">папок</div>
              </div>

              <div className="h-8 w-px bg-white/20"></div>

              <div className="text-center">
                <div className="text-xl font-bold text-green-300">{files.length}</div>
                <div className="text-xs text-white/60">файлов</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white/10 backdrop-blur-xl rounded-2xl p-6 min-h-[60vh] mb-6">

        {renderModernNavigation()}

        {/* Поиск по тегам */}
        <div className="mb-4">
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <span className="absolute left-2 top-1/2 transform -translate-y-1/2 text-white/50 w-4 h-4 flex items-center justify-center">
                 🔍
               </span>
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={async (e) => {
                  if (e.key === 'Enter' && searchInput.trim()) {
                    const newTag = searchInput.trim();
                    if (!searchTags.includes(newTag)) {
                      const updatedTags = [...searchTags, newTag];
                      setSearchTags(updatedTags);
                      setSearchInput("");

                      setIsSearching(true);
                      try {
                        const params = new URLSearchParams();
                        params.append("includeDirectories", "true");
                        params.append("recursive", "true");
                        params.append("tags", updatedTags.join(','));

                        const response = await fetchWithTokenRefresh(
                          `${BASE}/files/list?${params.toString()}`,
                          { headers: { "Accept": "application/json" } },
                          token
                        );

                         const responseText = await response.text();
                                console.log("RAW SEARCH RESPONSE:", responseText);

                         let data;

                         try {
                           data = JSON.parse(responseText);
                         } catch (e) {
                           console.error("Failed to parse JSON:", e);
                           return;
                         }

                         if (!response.ok) throw new Error(`Search failed: ${response.status}`);

                         const items = data?.files || data || [];

                         if (!Array.isArray(items)) {
                           return;
                         }

                         const processed = items.map(item => ({
                           name: item.name || "Без имени",
                           type: item.isDirectory ? "folder" : "file",
                           size: item.size || 0,
                           id: item.id,
                           _raw: item
                         }));

                         setFiles(processed.filter(f => f.type === "file"));
                         setFolders(processed.filter(f => f.type === "folder"));
                      } catch (error) {
                        console.error("Search error:", error);
                        setError("Ошибка при поиске по тегам");
                      } finally {
                        setIsSearching(false);
                      }
                    }
                  }
                }}
                placeholder="Введите тег и нажмите Enter"
                className="w-full p-2 pl-8 pr-8 rounded-lg bg-white/20 text-white text-sm placeholder-white/50 focus:outline-none focus:ring-1 focus:ring-blue-500/50"
                disabled={isSearching}
              />
              {isSearching && (
                <div className="absolute right-2 top-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-white"></div>
                </div>
              )}
            </div>

            {searchTags.length > 0 && (
              <button
                onClick={async () => {
                  setSearchTags([]);
                  setSearchInput("");
                  await fetchFiles();
                }}
                className="text-sm text-white/50 hover:text-white px-2 whitespace-nowrap"
                title="Очистить все теги"
              >
                ✕
              </button>
            )}
          </div>

          {/* Выбранные теги */}
          {searchTags.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-2">
              {searchTags.map((tag, index) => (
                <div key={index} className="flex items-center bg-blue-500/30 text-blue-300 px-2 py-0.5 rounded-full text-xs">
                  <span>{tag}</span>
                  <button
                    onClick={async () => {
                      const updatedTags = searchTags.filter((_, i) => i !== index);
                      setSearchTags(updatedTags);

                      if (updatedTags.length === 0) {
                        await fetchFiles();
                      } else {
                        setIsSearching(true);
                        try {
                          const params = new URLSearchParams();
                          params.append("includeDirectories", "true");
                          params.append("recursive", "true");
                          params.append("tags", updatedTags.join(','));

                          const response = await fetchWithTokenRefresh(
                            `${BASE}/files/list?${params.toString()}`,
                            { headers: { "Accept": "application/json" } },
                            token
                          );

                          const responseText = await response.text();
                          console.log("RAW SEARCH RESPONSE:", responseText);

                          if (!response.ok) throw new Error(`Search failed: ${response.status}`);

                          const data = JSON.parse(responseText);
                          const items = data?.files || data || [];

                          if (!Array.isArray(items)) {
                            return;
                          }

                          const processed = items.map(item => ({
                            name: item.name || "Без имени",
                            type: item.isDirectory ? "folder" : "file",
                            size: item.size || 0,
                            id: item.id,
                            _raw: item
                          }));

                          setFiles(processed.filter(f => f.type === "file"));
                          setFolders(processed.filter(f => f.type === "folder"));
                        } catch (error) {
                          console.error("Search error:", error);
                          setError("Ошибка при поиске по тегам");
                        } finally {
                          setIsSearching(false);
                        }
                      }
                    }}
                    className="ml-1 text-blue-300 hover:text-white hover:bg-blue-500/40 rounded-full w-4 h-4 flex items-center justify-center"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

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
                <div className="text-6xl mb-4 opacity-50">
                  {searchTags.length > 0 ? "👻" : "📁"}
                </div>
                <p className="text-xl mb-2">
                  {searchTags.length > 0 ? "Ничего не найдено" : "Папка пуста"}
                </p>
                <p className="text-white/70">
                  {searchTags.length > 0
                    ? ""
                    : "Загрузите файл или создайте папку"}
                </p>
              </div>
            ) : (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
              {folders.map((folder) => (
                <div key={folder.id || folder.fullPath}
                  className="bg-white/5 hover:bg-white/10 rounded-xl p-4 cursor-pointer transition-all hover:scale-105 group relative"
                >
                  <div
                    onClick={(e) => handleItemClick(folder, e)}
                    className="mb-2"
                  >
                    <div className="text-4xl mb-2 group-hover:scale-110 transition-transform">📁</div>
                    <p className="truncate text-sm font-medium">
                      {getItemName(folder)}
                    </p>
                    {folder.fileCount ? (
                      <p className="text-xs text-white/50 mt-1">{folder.fileCount} файлов</p>
                    ) : (
                      <p className="text-xs text-white/30 mt-1">Папка</p>
                    )}
                  </div>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteFolder(folder);
                    }}
                    className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 p-1.5 rounded-lg"
                    title="Удалить папку"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              ))}

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
                    {getFileIcon(file)}
                  </div>
                  <p className="truncate text-sm font-medium">{file.name}</p>
                  <p className="text-xs text-white/50 mt-1">{formatFileSize(file.size)}</p>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

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

      {showUploadModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
            <h3 className="text-xl font-bold mb-4">Загрузить файл</h3>

            <div className="mb-4">
              <label className="block text-sm text-white/70 mb-2">Теги (через запятую):</label>
              <input
                type="text"
                placeholder="работа, проект, важное"
                defaultValue={uploadTags.join(', ')}
                onBlur={(e) => {
                  const tags = e.target.value.split(',').map(tag => tag.trim()).filter(tag => tag);
                  setUploadTags(tags);
                }}
                className="w-full p-3 rounded-xl bg-white/20 mb-2 text-white"
              />
              <div className="text-xs text-white/50">
                Введите теги через запятую
              </div>

              {uploadTags.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-2">
                  {uploadTags.map((tag, index) => (
                    <div key={index} className="flex items-center bg-blue-500/30 text-blue-300 px-2 py-1 rounded-full text-sm">
                      <span>{tag}</span>
                      <button
                        type="button"
                        onClick={() => {
                          setUploadTags(prev => prev.filter((_, i) => i !== index));
                        }}
                        className="ml-1 text-blue-300 hover:text-white"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
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
                Загрузить
              </button>
            </div>
          </div>
        </div>
      )}

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

      {showInfoModal && fileInfoData && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
          <div className="bg-gradient-to-br from-gray-900 to-blue-900 rounded-2xl p-6 w-full max-w-md border border-white/10 shadow-2xl">
            {error && (
              <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-center text-sm">
                {error}
              </div>
            )}

            {success && (
              <div className="mb-4 p-3 bg-green-500/20 border border-green-500 rounded-xl text-center text-sm">
                {success}
              </div>
            )}

            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-bold text-white flex items-center">
                <span className="mr-2">📄</span>
                Информация о файле
              </h3>
              <button
                onClick={() => {
                  setShowInfoModal(false);
                  setFileInfoData(null);
                  setFileTags([]);
                }}
                className="text-white/70 hover:text-white text-2xl bg-white/10 w-8 h-8 rounded-full flex items-center justify-center hover:bg-white/20 transition-colors"
              >
                ×
              </button>
            </div>

            <div className="flex items-center space-x-4 p-4 bg-white/10 rounded-xl mb-4">
              <div className="text-4xl">{getFileIcon(fileInfoData)}</div>
              <div className="flex-1 min-w-0">
                {editingFileName ? (
                  <div className="space-y-2">
                    <input
                      type="text"
                      value={newFileName}
                      onChange={(e) => setNewFileName(e.target.value)}
                      onKeyDown={async (e) => {
                        if (e.key === 'Enter' && newFileName.trim() && newFileName !== fileInfoData.name) {
                          try {
                            await handleSaveFileName();
                          } catch (error) {
                            console.error("Failed to save file name:", error);
                          }
                        } else if (e.key === 'Escape') {
                          setNewFileName(fileInfoData.name);
                          setEditingFileName(false);
                        }
                      }}
                      className="w-full p-2 bg-white/20 rounded-lg text-white focus:outline-none focus:ring-1 focus:ring-blue-500/50 focus:bg-white/25"
                      placeholder="Новое имя файла"
                      autoFocus
                    />
                    <div className="flex space-x-2">
                      <button
                        onClick={handleSaveFileName}
                        disabled={!newFileName.trim() || newFileName === fileInfoData.name}
                        className="px-3 py-1 bg-blue-600 hover:bg-blue-700 rounded-lg text-sm disabled:opacity-50 transition-colors"
                      >
                        Сохранить
                      </button>
                      <button
                        onClick={() => {
                          setNewFileName(fileInfoData.name);
                          setEditingFileName(false);
                        }}
                        className="px-3 py-1 bg-white/20 hover:bg-white/30 rounded-lg text-sm transition-colors"
                      >
                        Отмена
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="flex items-center justify-between">
                      <div className="font-bold text-lg truncate">{fileInfoData.name}</div>
                      <button
                        onClick={() => {
                          setNewFileName(fileInfoData.name);
                          setEditingFileName(true);
                        }}
                        className="text-blue-400 hover:text-blue-300 ml-2 flex-shrink-0 p-1 hover:bg-blue-500/20 rounded transition-colors"
                        title="Редактировать имя"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 mb-4">
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
                <div className="font-medium relative">
                  <div className="relative">
                    <select
                      value={fileVisibility}
                      onChange={async (e) => {
                        const newVisibility = e.target.value;
                        setFileVisibility(newVisibility);
                        if (fileInfoData.item && fileInfoData.item.type === "file") {
                          try {
                            await handleSaveVisibility(newVisibility);
                          } catch (error) {
                            console.error("Failed to save visibility:", error);
                          }
                        }
                      }}
                      className="w-full bg-white/10 hover:bg-white/15 text-white rounded-lg p-2 text-sm appearance-none cursor-pointer border border-white/10 focus:border-blue-500/50 focus:outline-none focus:ring-1 focus:ring-blue-500/30 transition-all"
                    >
                      <option value="private" className="bg-gray-800 text-white">Приватный</option>
                      <option value="public" className="bg-gray-800 text-white">Публичный</option>
                    </select>
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-white/70">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white/10 p-3 rounded-xl">
                <div className="text-sm text-white/60 mb-1">Дата изменения</div>
                <div className="font-medium">{fileInfoData.formattedDate}</div>
              </div>
            </div>

            <div className="bg-white/10 p-4 rounded-xl mb-6">
              <div className="text-sm text-white/60 mb-3">Теги файла</div>

              <div className="mb-3">
                <input
                  id="tagInput"
                  type="text"
                  placeholder="Добавить тег"
                  onKeyDown={async (e) => {
                    if (e.key === 'Enter' && e.target.value.trim()) {
                      const newTag = e.target.value.trim();

                      if (!fileTags.includes(newTag)) {
                        const updatedTags = [...fileTags, newTag];
                        setFileTags(updatedTags);
                        e.target.value = '';

                        if (fileInfoData.item && fileInfoData.item.type === "file") {
                          try {
                            await handleSaveTags(updatedTags);
                          } catch (error) {
                            console.error("Failed to save tags:", error);
                            setFileTags(fileTags.filter(tag => tag !== newTag));
                          }
                        }
                      }
                    }
                  }}
                  className="w-full p-2 bg-white/20 rounded-lg text-white text-sm placeholder-white/50 focus:outline-none focus:ring-1 focus:ring-blue-500/50 focus:bg-white/25 transition-all"
                />
                <div className="text-xs text-white/50 mt-1">
                  Нажмите Enter для добавления тега
                </div>
              </div>

               <div className="flex flex-wrap gap-2">
                  {fileTags.map((tag, idx) => (
                    <div key={idx} className="flex items-center bg-gradient-to-r from-blue-500/30 to-cyan-500/30 text-blue-200 px-3 py-1.5 rounded-full text-sm group border border-blue-500/20 hover:border-blue-400/30 transition-all">
                      <span className="font-medium">{tag}</span>
                      <button
                        type="button"
                        onClick={async () => {
                          const updatedTags = fileTags.filter(t => t !== tag);
                          setFileTags(updatedTags);

                          if (fileInfoData.item && fileInfoData.item.type === "file") {
                            try {
                              await handleSaveTags(updatedTags);
                            } catch (error) {
                              console.error("Failed to save tags:", error);
                              setFileTags([...updatedTags, tag]);
                            }
                          }
                        }}
                      className="ml-2 text-blue-300 hover:text-white text-xs bg-blue-500/40 hover:bg-blue-500/60 w-5 h-5 rounded-full flex items-center justify-center transition-colors"
                      title="Удалить тег"
                    >
                      ×
                    </button>
                  </div>
                ))}
                {fileTags.length === 0 && (
                  <div className="text-white/50 text-sm italic bg-white/5 p-3 rounded-lg w-full text-center">
                    Нет тегов
                  </div>
                )}
              </div>
            </div>

            <div className="flex justify-end">
              <button
                onClick={() => {
                  setShowInfoModal(false);
                  setFileInfoData(null);
                  setFileTags([]);
                }}
                className="px-5 py-2.5 bg-gradient-to-r from-blue-600/80 to-cyan-600/80 hover:from-blue-600 hover:to-cyan-600 text-white rounded-xl font-medium transition-all hover:scale-105 active:scale-95 shadow-lg"
              >
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}

      {showItemMenu && selectedItem && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setShowItemMenu(false)} />
          <div className="fixed bg-gray-800 rounded-xl shadow-2xl py-2 z-50 min-w-[200px]" style={{ left: itemMenuPosition.x, top: itemMenuPosition.y }}>
            {selectedItem.type === "file" && (
              <button onClick={() => handleFileAction("download")} className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors">
                📥 Скачать
              </button>
            )}
            <button onClick={() => handleFileAction("info")} className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors">
              ℹ️ Информация
            </button>
            <div className="border-t border-white/20 my-1" />
            <button
              onClick={() => handleFileAction("delete")}
              className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors text-red-300"
            >
              {selectedItem.type === "folder" ? "🗑️ Удалить папку" : "🗑️ Удалить файл"}
            </button>
          </div>
        </>
      )}
    </div>
  );
}