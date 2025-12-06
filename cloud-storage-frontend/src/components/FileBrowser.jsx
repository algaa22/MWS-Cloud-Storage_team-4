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
  createFolder as apiCreateFolder
} from "../api.js";

export default function FileBrowser() {
  const { user, logout, token } = useAuth();
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [files, setFiles] = useState([]);
  const [folders, setFolders] = useState([]);
  // Variant B: currentPath ends with '/', root = ''
  const [currentPath, setCurrentPath] = useState("");
  const [selectedItem, setSelectedItem] = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showItemMenu, setShowItemMenu] = useState(false);
  const [itemMenuPosition, setItemMenuPosition] = useState({ x: 0, y: 0 });
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showFolderModal, setShowFolderModal] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [newFolderName, setNewFolderName] = useState("");
  const [renameText, setRenameText] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // normalize helper: ensure folder path ends with slash, root is ''
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

    console.log("Calling fetchFiles...");
    fetchFiles();

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, currentPath]); // Зависимость от token и currentPath

  const computeFullPathForItem = (item) => {
    // If item already has fullPath, use it
    if (item.fullPath && typeof item.fullPath === "string" && item.fullPath !== "") {
      return item.fullPath;
    }

    const name = item.name || "";
    const parent = item.path || "";

    // If parent is empty => top-level
    if (!parent || parent === "." || parent === "/") {
      return item.type === "folder" ? `${name}/` : `${name}`;
    }

    // Ensure parent ends with '/'
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

      // Normalize items: compute fullPath and keep name/type/size/fileCount
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

      // Filter only items that belong to currentPath (robustness)
      // Because server returns items for this directory, but double-check:
      const filtered = normalized.filter((it) => {
        // If currentPath is root ('') accept items whose fullPath does not contain a parent prefix
        if (!currentPath) return true;
        return it.fullPath.startsWith(currentPath);
      });

      // Split into files and folders (folders first)
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
      // item.fullPath is like 'folder123/' or 'parent/sub/'
      const folderPath = normalizeCurrentPath(item.fullPath);
      setCurrentPath(folderPath);
    } else {
      // For file - open context menu
      setSelectedItem(item);
      setShowItemMenu(true);
      setItemMenuPosition({ x: event.clientX, y: event.clientY });
    }
  };

  const handleFileAction = async (action) => {
    if (!selectedItem) return;

    try {
      switch (action) {
        case "download":
          await apiDownloadFile(token, selectedItem.fullPath, selectedItem.name);
          break;
        case "rename":
          setRenameText(selectedItem.name);
          break;
        case "delete":
          if (window.confirm(`Удалить "${selectedItem.name}"?`)) {
            await apiDeleteFile(token, selectedItem.fullPath);
            await fetchFiles();
          }
          break;
        case "info":
          const info = await apiGetFileInfo(token, selectedItem.fullPath);
          alert(
              `Информация о файле:\nИмя: ${info.name}\nРазмер: ${formatFileSize(
                  info.size || 0
              )}\nТип: ${info.type || info.mime_type || "—"}\nДата: ${info.modified ? new Date(info.modified).toLocaleString() : "—"}`
          );
          break;
      }
    } catch (err) {
      console.error("handleFileAction error:", err);
      setError(`Ошибка: ${err.message}`);
    } finally {
      setShowItemMenu(false);
      setSelectedItem(null);
    }
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      setUploading(true);
      setUploadProgress(0);

      // currentPath already ends with '/', root = ''
      const targetPath = currentPath ? `${currentPath}${file.name}` : file.name;
      await apiUploadFile(token, file, targetPath, (progress) => {
        setUploadProgress(progress);
      });

      await fetchFiles();
      setShowUploadModal(false);
    } catch (err) {
      console.error("Upload error:", err);
      setError("Ошибка при загрузке файла");
    } finally {
      setUploading(false);
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return;

    try {
      // currentPath ends with '/' or is ''
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
      const oldFull = item.fullPath; // folder: ends with '/', file: no trailing '/'
      let newFull;
      if (isFolder) {
        // replace last segment before trailing slash
        newFull = oldFull.replace(/[^\/]+\/$/, `${renameText}/`);
      } else {
        // replace last segment after last slash
        newFull = oldFull.replace(/[^\/]+$/, renameText);
      }

      await apiRenameFile(token, oldFull, newFull);
      setRenameText("");
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
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
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

  // Breadcrumb helpers for variant B (trailing slash)
  const renderBreadcrumbs = () => {
    if (!currentPath) return null;
    // remove trailing slash for splitting
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

  return (
      <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 text-white p-4">
        {/* Header */}
        <header className="flex justify-between items-center mb-8">
          <h1 className="text-2xl font-bold">Облачное хранилище</h1>

          <div className="relative">
            <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center space-x-2 bg-white/20 backdrop-blur-sm rounded-xl px-4 py-2 hover:bg-white/30 transition-colors"
            >
              <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                {user?.username?.[0]?.toUpperCase() || "U"}
              </div>
              <span>{user?.username || "Пользователь"}</span>
            </button>

            {showUserMenu && (
                <div className="absolute right-0 mt-2 w-48 bg-white/20 backdrop-blur-xl rounded-xl shadow-2xl py-2 z-50">
                  <div className="px-4 py-2 border-b border-white/20">
                    <p className="font-medium">{user?.email}</p>
                    <p className="text-sm text-white/70">Хранилище: 2.4 GB / 10 GB</p>
                  </div>
                  <button
                      onClick={() => {
                        /* profile */
                      }}
                      className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors"
                  >
                    Настройки профиля
                  </button>
                  <button
                      onClick={handleLogout}
                      className="block w-full text-left px-4 py-2 hover:bg-white/10 transition-colors text-red-300"
                  >
                    Выйти
                  </button>
                </div>
            )}
          </div>
        </header>

        {/* Breadcrumbs */}
        <div className="mb-6">
          <button
              onClick={() => setCurrentPath("")}
              className="text-white/70 hover:text-white"
          >
            Главная
          </button>
          {currentPath && renderBreadcrumbs()}
        </div>

        {/* Main area */}
        <div className="bg-white/10 backdrop-blur-xl rounded-2xl p-6 min-h-[60vh] mb-6">
          {error && (
              <div className="mb-4 p-3 bg-red-500/20 border border-red-500 rounded-xl text-center">
                {error}
              </div>
          )}

          {loading ? (
              <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-white"></div>
              </div>
          ) : files.length === 0 && folders.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 text-center">
                <div className="text-6xl mb-4">📁</div>
                <p className="text-xl mb-2">Файлов нет</p>
                <p className="text-white/70">Загрузите файл, и он появится здесь!</p>
              </div>
          ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
                {/* Folders */}
                {folders.map((folder) => (
                    <div
                        key={folder.id || folder.fullPath}
                        onClick={(e) => handleItemClick(folder, e)}
                        className="bg-white/5 hover:bg-white/10 rounded-xl p-4 cursor-pointer transition-all hover:scale-105"
                    >
                      <div className="text-4xl mb-2">📁</div>
                      <p className="truncate text-sm">
                        {folder.name || folder.fullPath.split("/").filter(Boolean).pop()}
                      </p>
                      {folder.fileCount ? (
                          <p className="text-xs text-white/50 mt-1">{folder.fileCount} файлов</p>
                      ) : null}
                    </div>
                ))}

                {/* Files */}
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
                        className="bg-white/5 hover:bg-white/10 rounded-xl p-4 cursor-pointer transition-all hover:scale-105"
                    >
                      <div className="text-4xl mb-2">{getFileIcon(file.name)}</div>
                      <p className="truncate text-sm">{file.name}</p>
                      <p className="text-xs text-white/50 mt-1">{formatFileSize(file.size)}</p>
                    </div>
                ))}
              </div>
          )}
        </div>

        {/* Bottom buttons */}
        <div className="fixed bottom-6 left-1/2 transform -translate-x-1/2 flex space-x-4">
          <button
              onClick={() => setShowUploadModal(true)}
              disabled={uploading}
              className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-xl font-medium transition-colors flex items-center space-x-2 disabled:opacity-50"
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
              className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-xl font-medium transition-colors flex items-center space-x-2"
          >
            <span>📁</span>
            <span>Создать папку</span>
          </button>
        </div>

        {/* Upload modal */}
        {showUploadModal && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
                <h3 className="text-xl font-bold mb-4">Загрузить файл</h3>
                <input type="file" ref={fileInputRef} onChange={handleFileUpload} className="w-full mb-4" />
                {uploading && (
                    <div className="w-full bg-gray-700 rounded-full h-2 mb-4">
                      <div className="bg-blue-500 h-2 rounded-full transition-all duration-300" style={{ width: `${uploadProgress}%` }} />
                    </div>
                )}
                <div className="flex justify-end space-x-3">
                  <button onClick={() => setShowUploadModal(false)} className="px-4 py-2 rounded-xl bg-white/20 hover:bg-white/30" disabled={uploading}>
                    Отмена
                  </button>
                  <button onClick={() => fileInputRef.current?.click()} className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700" disabled={uploading}>
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
        {selectedItem && renameText !== "" && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-800 rounded-2xl p-6 w-full max-w-md">
                <h3 className="text-xl font-bold mb-4">Переименовать</h3>
                <input type="text" value={renameText} onChange={(e) => setRenameText(e.target.value)} className="w-full p-3 rounded-xl bg-white/20 mb-4 text-white" />
                <div className="flex justify-end space-x-3">
                  <button onClick={() => setRenameText("")} className="px-4 py-2 rounded-xl bg-white/20 hover:bg-white/30">Отмена</button>
                  <button onClick={handleRename} className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700">Сохранить</button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}
