// src/api.js
const BASE = "https://localhost:8443/api";

/**
 * Helpers
 */
async function parseJsonSafe(res) {
  try {
    return await res.json();
  } catch (e) {
    return null;
  }
}

function pickTokenFromResponse(data) {
  if (!data) return null;
  return (
      data.token ||
      data.accessToken ||
      data.AccessToken ||
      data.access_token ||
      data.jwt ||
      null
  );
}

/**
 * Вспомогательная функция для выполнения запроса с автоматическим обновлением токена
 */
async function fetchWithTokenRefresh(url, options = {}, token) {
  // Если токен не передан явно, берем из localStorage
  let currentToken = token || localStorage.getItem("accessToken");

  if (!currentToken) {
    throw new Error("Требуется авторизация");
  }

  // Добавляем токен в заголовки, если его там еще нет
  const headers = {
    ...options.headers,
    "X-Auth-Token": currentToken
  };

  try {
    const res = await fetch(url, { ...options, headers });

    // Если токен истек (400 или 401 статус), пытаемся обновить
    if (res.status === 400 || res.status === 401) {
      console.log("Token expired, attempting refresh...");
      const refreshToken = localStorage.getItem("refreshToken");

      if (refreshToken) {
        try {
          const newAccessToken = await refreshTokenRequest(refreshToken);
          console.log("Token refreshed successfully");

          // Обновляем токен в заголовках
          headers["X-Auth-Token"] = newAccessToken;

          // Повторяем запрос с новым токеном
          const retryRes = await fetch(url, { ...options, headers });

          if (!retryRes.ok) {
            const errorText = await retryRes.text();
            throw new Error(`HTTP ${retryRes.status}: ${errorText}`);
          }

          return retryRes;
        } catch (refreshError) {
          console.error("Refresh failed:", refreshError);
          throw new Error(`Authentication failed: ${refreshError.message}`);
        }
      } else {
        throw new Error("Refresh token not available");
      }
    }

    return res;
  } catch (error) {
    console.error("Fetch error:", error);
    throw error;
  }
}

export async function loginRequest(email, password) {
  try {
    console.log("Login request to:", `${BASE}/users/auth/login`);

    const res = await fetch(`${BASE}/users/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Auth-Email": email,
        "X-Auth-Password": password
      }
    });

    console.log("Login response status:", res.status, res.statusText);

    if (!res.ok) {
      const text = await res.text().catch(() => "(no body)");
      console.error("Login error:", res.status, text);
      throw new Error(`Login failed: ${res.status} ${text}`);
    }

    const data = await parseJsonSafe(res);
    console.log("Login success response received");

    const accessToken = data?.accessToken || data?.AccessToken || data?.token;
    const refreshToken = data?.refreshToken || data?.RefreshToken;

    if (!accessToken) {
      console.error("No access token in response! Full response:", data);
      throw new Error("Server did not return access token");
    }

    console.log("Access token:", accessToken?.substring(0, 50) + "...");
    console.log("Refresh token:", refreshToken ? "Present" : "Missing");

    if (accessToken) {
      localStorage.setItem("accessToken", accessToken);
    }
    if (refreshToken) {
      localStorage.setItem("refreshToken", refreshToken);
    }

    return accessToken;
  } catch (error) {
    console.error("Login request failed:", error);
    throw error;
  }
}

export async function registerRequest(email, password, username) {
  try {
    console.log("Register request to:", `${BASE}/users/auth/register`);

    const res = await fetch(`${BASE}/users/auth/register`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Auth-Email": email,
        "X-Auth-Password": password,
        "X-Auth-Username": username
      }
    });

    console.log("Register response status:", res.status, res.statusText);

    if (!res.ok) {
      const text = await res.text().catch(() => "(no body)");
      console.error("Register error response:", res.status, text);
      throw new Error(`Register failed: ${res.status} ${text}`);
    }

    const data = await parseJsonSafe(res);
    console.log("Register success:", data);
    return pickTokenFromResponse(data);
  } catch (error) {
    console.error("Register request failed:", error);
    throw error;
  }
}

// В api.js, обновите функцию getUserInfo чтобы она возвращала информацию о хранилище:
export async function getUserInfo(token) {
  console.log("=== getUserInfo DEBUG ===");

  if (!token) {
    throw new Error("Invalid token provided");
  }

  const url = `${BASE}/users/info`;
  console.log("Request URL:", url);

  try {
    const res = await fetchWithTokenRefresh(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json"
      }
    }, token);

    console.log("Response status:", res.status, res.statusText);

    if (!res.ok) {
      const errorText = await res.text();
      console.error("Error response:", errorText);
      throw new Error(`HTTP ${res.status}: ${errorText}`);
    }

    const data = await res.json();
    console.log("Success! User data:", data);

    // ДОБАВЬТЕ ОТЛАДОЧНЫЙ ВЫВОД ДЛЯ ПРОВЕРКИ
    console.log("=== DEBUG: Checking storage fields ===");
    console.log("data.UsedStorage:", data.UsedStorage, typeof data.UsedStorage);
    console.log("data.StorageLimit:", data.StorageLimit, typeof data.StorageLimit);
    console.log("All data keys:", Object.keys(data));

    // Парсим информацию о хранилище из ответа
    const storageInfo = {
      used: 0,
      total: 10 * 1024 * 1024 * 1024, // 10GB по умолчанию
      formattedUsed: '0 Bytes',
      formattedTotal: '10 GB',
      percentage: 0
    };

    // Проверяем поля без пробелов (как в вашем коде сервера)
    if (data.UsedStorage !== undefined) {
      storageInfo.used = Number(data.UsedStorage) || 0;
      console.log("UsedStorage found:", storageInfo.used);
    }

    if (data.StorageLimit !== undefined) {
      storageInfo.total = Number(data.StorageLimit) || 10 * 1024 * 1024 * 1024;
      console.log("StorageLimit found:", storageInfo.total);
    }

    // Также проверяем варианты с маленькой буквы (на всякий случай)
    if (data.usedStorage !== undefined && storageInfo.used === 0) {
      storageInfo.used = Number(data.usedStorage) || 0;
      console.log("usedStorage found:", storageInfo.used);
    }

    if (data.storageLimit !== undefined && storageInfo.total === 10 * 1024 * 1024 * 1024) {
      storageInfo.total = Number(data.storageLimit) || 10 * 1024 * 1024 * 1024;
      console.log("storageLimit found:", storageInfo.total);
    }

    // Проверяем стандартные варианты
    if (data.used !== undefined && storageInfo.used === 0) {
      storageInfo.used = Number(data.used) || 0;
    }

    if (data.total !== undefined && storageInfo.total === 10 * 1024 * 1024 * 1024) {
      storageInfo.total = Number(data.total) || 10 * 1024 * 1024 * 1024;
    }

    // Вычисляем процент и форматируем
    storageInfo.percentage = storageInfo.total > 0 ?
        Math.round((storageInfo.used / storageInfo.total) * 100) : 0;
    storageInfo.formattedUsed = formatBytes(storageInfo.used);
    storageInfo.formattedTotal = formatBytes(storageInfo.total);

    console.log("Parsed storage info:", storageInfo);
    console.log("Percentage:", storageInfo.percentage + "%");

    return {
      ...data,
      storageInfo
    };

  } catch (error) {
    console.error("Fetch error:", error);
    throw error;
  }
}

/**
 * LOGOUT
 */
export async function logoutRequest(token) {
  try {
    console.log("Logout request");

    const res = await fetchWithTokenRefresh(`${BASE}/users/auth/logout`, {
      method: "POST"
    }, token);

    console.log("Logout response status:", res.status);

    // Очищаем токены при выходе
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");

    return res.ok;
  } catch (error) {
    console.error("Logout request failed:", error);
    // В любом случае очищаем токены при ошибке выхода
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    return false;
  }
}

/**
 * getFiles - Согласно документации
 */
export const getFiles = async (token, currentPath = "") => {
  console.log("=== GET FILES WITH GET REQUEST FOR SIZE ===");

  if (!token) throw new Error("Требуется авторизация");

  const params = new URLSearchParams();
  params.append("includeDirectories", "true");
  if (currentPath) params.append("directory", currentPath);

  const listUrl = `${BASE}/files/list?${params.toString()}`;

  try {
    // 1. Получаем список файлов
    const listResponse = await fetchWithTokenRefresh(listUrl, {
      headers: {
        "Accept": "application/json"
      }
    }, token);

    if (!listResponse.ok) {
      const errorText = await listResponse.text();
      throw new Error(`Server error ${listResponse.status}: ${errorText}`);
    }

    const data = await listResponse.json();
    const files = data?.files || data || [];

    console.log(`Found ${files.length} items`);
    console.log("Raw server response:", files); // ДОБАВЬТЕ ЭТУ СТРОКУ

    // 2. Функция для получения информации о файле через GET
    const getFileInfo = async (filePath) => {
      try {
        const infoUrl = `${BASE}/files/info?path=${encodeURIComponent(filePath)}`;
        console.log(`Getting info for: ${filePath}`);

        const infoResponse = await fetchWithTokenRefresh(infoUrl, {
          headers: {
            "Accept": "application/json"
          }
        }, token);

        if (infoResponse.ok) {
          const infoData = await infoResponse.json();
          console.log(`Info for ${filePath}:`, infoData);
          return infoData;
        } else {
          console.warn(`Cannot get info for ${filePath}:`, infoResponse.status);
        }
      } catch (error) {
        console.warn(`Error getting info for ${filePath}:`, error.message);
      }
      return null;
    };

    // 3. Обрабатываем файлы
    const result = [];

    for (let i = 0; i < files.length; i++) {
      const item = files[i];
      const path = item.path || "";

      // УЛУЧШЕННОЕ ИЗВЛЕЧЕНИЕ ИМЕНИ
      let name = "Без имени";

      // Вариант 1: Если у item есть явное поле name
      if (item.name && item.name.trim() !== "") {
        name = item.name;
      }
      // Вариант 2: Извлекаем из path
      else if (path) {
        const pathParts = path.split('/').filter(p => p && p !== '');
        if (pathParts.length > 0) {
          name = pathParts[pathParts.length - 1];
        }
      }
      // Вариант 3: Используем индекс как крайний вариант
      else {
        name = `Объект ${i + 1}`;
      }

      // Определяем тип
      let type = "file";
      let size = 0;
      let fileInfo = null;

      // УЛУЧШЕННОЕ ОПРЕДЕЛЕНИЕ ТИПА
      if (item.type === "folder" || item.type === "directory" || path.endsWith('/')) {
        type = "folder";
      } else {
        type = "file";
        // Это файл - получаем информацию
        fileInfo = await getFileInfo(path);
        if (fileInfo) {
          size = fileInfo.Size || fileInfo.size || 0;
        }
      }

      // Небольшая пауза между запросами
      if (i < files.length - 1 && type === "file") {
        await new Promise(resolve => setTimeout(resolve, 50));
      }

      result.push({
        name: name,
        path: path,
        type: type,
        size: size,
        id: item.id || path || Math.random().toString(),
        fullPath: path,
        _raw: item, // сохраняем сырой ответ сервера для отладки
        _info: fileInfo
      });

      console.log(`Processed ${i+1}/${files.length}:`, {
        name: name,
        path: path,
        type: type,
        size: size,
        rawItem: item
      });
    }

    console.log("Final result:", result);
    return result;

  } catch (err) {
    console.error("Error in getFiles:", err);
    throw err;
  }
};

/**
 * uploadFile
 */
export const uploadFile = async (token, file, path, onProgress) => {
  console.log("=== UPLOAD FILE DEBUG ===");
  console.log("Parameters received:");
  console.log("- Token length:", token?.length);
  console.log("- Token first 20 chars:", token?.substring(0, 20));
  console.log("- File:", file);
  console.log("- File name:", file?.name);
  console.log("- File size:", file?.size);
  console.log("- File type:", file?.type);
  console.log("- Path:", path);

  if (!token) {
    console.error("❌ NO TOKEN PROVIDED!");
    throw new Error("Требуется авторизация");
  }

  if (!file) {
    console.error("❌ NO FILE PROVIDED!");
    throw new Error("Файл не выбран");
  }

  const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
  const useChunkedUpload = file.size > CHUNK_SIZE;

  console.log(`Using ${useChunkedUpload ? 'CHUNKED' : 'SIMPLE'} upload`);

  if (useChunkedUpload) {
    return await uploadFileChunked(token, file, path, onProgress);
  } else {
    return await uploadFileSimple(token, file, path, onProgress);
  }
};

/**
 * Простая загрузка файла (для файлов ≤5MB)
 */
const uploadFileSimple = async (token, file, path, onProgress) => {
  console.log("Using simple upload");

  const url = `${BASE}/files/upload?path=${encodeURIComponent(path)}`;
  console.log("📤 Upload URL:", url);

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      controller.abort();
    }, 30000);

    console.log("🔄 Sending fetch request...");

    const startTime = Date.now();

    const res = await fetchWithTokenRefresh(url, {
      method: "POST",
      headers: {
        "X-File-Tags": "user_upload",
        "X-File-Size": file.size,
        "Content-Type": file.type || "application/octet-stream"
      },
      body: file,
      signal: controller.signal
    }, token);

    clearTimeout(timeoutId);
    const endTime = Date.now();
    console.log(`⏱️ Request took ${endTime - startTime}ms`);

    console.log("📥 Response received:", res.status, res.statusText);

    if (onProgress) onProgress(100);

    if (!res.ok) {
      const responseText = await res.text();
      console.error("❌ Upload failed:", responseText);
      throw new Error(`Upload failed: ${res.status} ${responseText}`);
    }

    const responseText = await res.text();
    try {
      return JSON.parse(responseText);
    } catch (e) {
      return responseText;
    }

  } catch (error) {
    console.error("🔥 Fetch error:", error);
    throw error;
  }
};

/**
 * Надежная чанкованная загрузка файла с использованием Fetch API и ReadableStream
 */
/**
 * Надежная чанкованная загрузка файла с отслеживанием прогресса
 */
const uploadFileChunked = async (token, file, path, onProgress) => {
  console.log("Using chunked upload with progress tracking");

  const url = `${BASE}/files/upload?path=${encodeURIComponent(path)}`;
  const totalSize = file.size;
  const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
  let uploaded = 0;

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      console.error("Upload request timed out!");
      controller.abort();
    }, 120000); // 120 секунд

    // Создаем массив для хранения всех чанков
    const chunks = [];
    for (let offset = 0; offset < totalSize; offset += CHUNK_SIZE) {
      const chunk = file.slice(offset, Math.min(offset + CHUNK_SIZE, totalSize));
      chunks.push(chunk);
    }

    console.log(`Total chunks: ${chunks.length}, Total size: ${totalSize} bytes`);

    // Для отслеживания прогресса используем XMLHttpRequest вместо fetch
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      xhr.open('POST', url, true);
      xhr.setRequestHeader('X-Auth-Token', token);
      xhr.setRequestHeader('X-File-Tags', 'user_upload');
      xhr.setRequestHeader('X-File-Size', totalSize);

      // Отслеживаем прогресс загрузки
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable && onProgress) {
          const progress = Math.round((event.loaded / event.total) * 100);
          console.log(`Upload progress: ${progress}% (${event.loaded}/${event.total} bytes)`);
          onProgress(progress);
        }
      };

      xhr.onload = () => {
        clearTimeout(timeoutId);
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            const response = JSON.parse(xhr.responseText);
            console.log("Upload completed successfully");
            if (onProgress) onProgress(100);
            resolve(response);
          } catch (e) {
            console.log("Upload response (non-JSON):", xhr.responseText);
            if (onProgress) onProgress(100);
            resolve(xhr.responseText);
          }
        } else {
          console.error(`Upload failed: ${xhr.status} ${xhr.statusText}`);
          reject(new Error(`Upload failed: ${xhr.status} ${xhr.responseText}`));
        }
      };

      xhr.onerror = () => {
        clearTimeout(timeoutId);
        console.error("Upload XHR error");
        reject(new Error('Network error during upload'));
      };

      xhr.ontimeout = () => {
        console.error("Upload timeout");
        reject(new Error('Upload timeout'));
      };

      // Отправляем файл как FormData
      const formData = new FormData();
      formData.append('file', file);
      formData.append('path', path);

      xhr.send(formData);
    });

  } catch (error) {
    console.error("Chunked upload error:", error);
    throw error;
  }
};




/**
 * downloadFile
 */
export const downloadFile = async (token, path, filename, fileSize) => {
    console.log("downloadFile request:", { path, filename, fileSize });

    const url = `${BASE}/files?path=${encodeURIComponent(path)}`;

    // TODO: вынести уже в переменную
    const CHUNKED_DOWNLOAD_THRESHOLD = 5 * 1024 * 1024;
    const useChunkedMode = fileSize > CHUNKED_DOWNLOAD_THRESHOLD;

    const headers = {
        "X-Auth-Token": token
    };

    if (useChunkedMode) {
        headers["X-Download-Mode"] = "chunked";
        console.log("Using chunked download mode");
    } else {
        console.log("Using default (aggregated) download mode");
    }

    try {
        const res = await fetch(url, {
            headers: headers
        });

        console.log("downloadFile status:", res.status, res.statusText);

        if (!res.ok) {
            const txt = await res.text().catch(() => "(no body)");
            console.error("Download failed:", res.status, txt);
            throw new Error(`Download failed: ${res.status} ${txt}`);
        }

        // Fetch API автоматически обрабатывает входящий поток (chunked transfer encoding)
        // и собирает его в Blob. Дальнейшая логика остается прежней.
        const blob = await res.blob();
        const urlBlob = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = urlBlob;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(urlBlob);
        document.body.removeChild(a);

    } catch (error) {
        console.error("🔥 Fetch error:", error);
        throw error;
    }
};

/**
 * deleteFile
 */
export const deleteFile = async (token, path) => {
  console.log("deleteFile request:", { path });

  const res = await fetchWithTokenRefresh(
      `${BASE}/files?path=${encodeURIComponent(path)}`,
      {
        method: "DELETE"
      },
      token
  );

  console.log("deleteFile status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Delete failed:", res.status, txt);
    throw new Error(`Delete failed: ${res.status} ${txt}`);
  }
  return true;
};

/**
 * renameFile
 */
export const renameFile = async (token, oldPath, newPath) => {
  console.log("renameFile request:", { oldPath, newPath });

  const url = `${BASE}/files?path=${encodeURIComponent(oldPath)}&newPath=${encodeURIComponent(newPath)}`;

  const res = await fetchWithTokenRefresh(url, {
    method: "PUT"
  }, token);

  console.log("renameFile status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Rename failed:", res.status, txt);
    throw new Error(`Rename failed: ${res.status} ${txt}`);
  }
  return true;
};

/**
 * getFileInfo
 */
export const getFileInfo = async (token, path) => {
  console.log("=== GET FILE INFO DEBUG ===");
  console.log("Token length:", token?.length);
  console.log("Path:", path);

  if (!token) {
    console.error("❌ No token provided");
    throw new Error("Требуется авторизация");
  }

  if (!path) {
    console.error("❌ No path provided");
    throw new Error("Путь к файлу не указан");
  }

  const url = `${BASE}/files/info?path=${encodeURIComponent(path)}`;
  console.log("Request URL:", url);

  try {
    const response = await fetchWithTokenRefresh(url, {
      headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
      }
    }, token);

    console.log("Response status:", response.status, response.statusText);

    if (!response.ok) {
      const errorText = await response.text().catch(() => "(no body)");
      console.error("Error response:", errorText);
      throw new Error(`Failed to get file info: ${response.status} ${errorText}`);
    }

    const data = await response.json();
    console.log("Raw response from server:", data);

    const fileInfo = {
      name: data.name || data.Name || path.split('/').pop() || "unknown",
      path: data.path || data.Path || path,
      size: data.size || data.Size || 0,
      type: data.type || data.Type || "unknown",
      mimeType: data.mimeType || data.MimeType || data.Type || "application/octet-stream",
      visibility: data.visibility || data.Visibility || "private",
      isolated: data.isolated || data.Isolated || false,
      tags: data.tags || data.Tags || "",
      createdAt: data.createdAt || data.CreatedAt || data.created_at,
      updatedAt: data.updatedAt || data.UpdatedAt || data.updated_at,
      lastModified: data.lastModified || data.LastModified,
      formattedSize: formatFileSize(data.size || data.Size || 0),
      _raw: data
    };

    console.log("Formatted file info:", fileInfo);
    return fileInfo;

  } catch (error) {
    console.error("Fetch error in getFileInfo:", error);
    throw error;
  }
};

/**
 * Вспомогательная функция для форматирования размера файла
 */
function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * createFolder - Используем /api/directories как в документации
 */
export const createFolder = async (token, folderPath) => {
  console.log("createFolder request:", { folderPath });

  const res = await fetchWithTokenRefresh(
      `${BASE}/directories?path=${encodeURIComponent(folderPath)}`,
      {
        method: "PUT"
      },
      token
  );

  console.log("createFolder status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Failed to create folder:", res.status, txt);
    throw new Error(`Failed to create folder: ${res.status} ${txt}`);
  }
  return true;
};

/**
 * deleteFolder
 */
export const deleteFolder = async (token, folderPath) => {
  console.log("deleteFolder request:", { folderPath });

  const res = await fetchWithTokenRefresh(
      `${BASE}/directories?path=${encodeURIComponent(folderPath)}`,
      {
        method: "DELETE"
      },
      token
  );

  console.log("deleteFolder status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Failed to delete folder:", res.status, txt);
    throw new Error(`Failed to delete folder: ${res.status} ${txt}`);
  }
  return true;
};

/**
 * updateUserInfo - версия с заголовками
 */
export const updateUserInfo = async (token, updates) => {
  console.log("=== UPDATE USER INFO DEBUG ===");
  console.log("Updates:", updates);

  // Проверка: если меняем пароль, нужен старый пароль
  if (updates.newPassword && !updates.oldPassword) {
    throw new Error("Old password is required when changing password");
  }

  // Создаем тело запроса
  const body = {
    userToken: token
  };

  // Создаем заголовки
  const headers = {
    "Content-Type": "application/json",
    "X-Auth-Token": token
  };

  // Добавляем данные в заголовки
  if (updates.newUsername) {
    body.newName = updates.newUsername; // Для JSON тела
    headers["X-New-Username"] = updates.newUsername; // Для заголовка
  }

  if (updates.oldPassword && updates.newPassword) {
    body.oldPassword = updates.oldPassword; // Для JSON тела
    body.newPassword = updates.newPassword; // Для JSON тела
    headers["X-Old-Password"] = updates.oldPassword; // Для заголовка
    headers["X-New-Password"] = updates.newPassword; // Для заголовка
  }

  console.log("Headers:", headers);
  console.log("Body:", JSON.stringify(body));

  const url = `${BASE}/users/update`;

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: headers,
      body: JSON.stringify(body) // Отправляем оба способа
    });

    if (!res.ok) {
      const errorText = await res.text().catch(() => "(no body)");
      throw new Error(`Update failed: ${res.status} ${errorText}`);
    }

    return await res.json();

  } catch (error) {
    console.error("Update error:", error);
    throw error;
  }
};

export const updateUserInfoComprehensive = async (token, updates) => {
  console.log("=== COMPREHENSIVE UPDATE USER INFO ===");
  console.log("Updates:", updates);

  // Проверяем, что есть что обновлять
  if (!updates.NewUsername && !updates.newPassword) {
    throw new Error("Please provide NewUsername or newPassword to update");
  }

  const body = {
    userToken: token
  };

  // Копируем все поля из updates в body
  if (updates.NewUsername) body.NewUsername = updates.NewUsername;
  if (updates.newPassword) body.newPassword = updates.newPassword;

  // Может быть и другие поля, если сервер их поддерживает
  if (updates.email) body.email = updates.email;
  if (updates.name) body.name = updates.name;

  console.log("Final request body:", JSON.stringify(body));

  const url = `${BASE}/users/update`;

  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Auth-Token": token
    },
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    const errorText = await res.text().catch(() => "(no body)");
    throw new Error(`Update failed: ${res.status} ${errorText}`);
  }

  return await res.json();
};

export async function refreshTokenRequest(refreshToken) {
  try {
    console.log("Refresh token request");

    const res = await fetch(`${BASE}/users/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Refresh-Token": refreshToken
      }
    });

    console.log("Refresh response status:", res.status, res.statusText);

    if (!res.ok) {
      const text = await res.text().catch(() => "(no body)");
      console.error("Refresh error:", res.status, text);
      throw new Error(`Token refresh failed: ${res.status} ${text}`);
    }

    const data = await parseJsonSafe(res);
    console.log("Refresh response:", data);

    const newAccessToken = data?.accessToken || data?.AccessToken || data?.token;
    const newRefreshToken = data?.refreshToken || data?.RefreshToken || refreshToken;

    if (!newAccessToken) {
      throw new Error("No new access token received");
    }

    // Обновляем токены в localStorage
    localStorage.setItem("accessToken", newAccessToken);
    if (newRefreshToken && newRefreshToken !== refreshToken) {
      localStorage.setItem("refreshToken", newRefreshToken);
    }

    return newAccessToken;

  } catch (error) {
    console.error("Refresh token request failed:", error);
    // Очищаем токены при неудачном обновлении
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    throw error;
  }
}

// В конец api.js добавьте:
/**
 * getUserStorageInfo - Получение информации о хранилище пользователя
 */
export const getUserStorageInfo = async (token) => {
  console.log("=== GET USER STORAGE INFO ===");

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  try {
    const url = `${BASE}/users/storage`;
    console.log("Request URL:", url);

    const response = await fetchWithTokenRefresh(url, {
      headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
      }
    }, token);

    console.log("Response status:", response.status, response.statusText);

    if (!response.ok) {
      // Если эндпоинт не существует, попробуем получить из user info
      console.log("Storage endpoint not available, trying user info");
      return null;
    }

    const data = await response.json();
    console.log("Storage info:", data);

    // Нормализуем ответ (сервер может использовать разные названия полей)
    const used = data.used || data.Used || data.usedBytes || 0;
    const total = data.total || data.Total || data.limit || data.Limit
        || data.totalBytes || (10 * 1024 * 1024 * 1024); // 10GB по умолчанию

    return {
      used,
      total,
      percentage: total > 0 ? Math.round((used / total) * 100) : 0,
      formattedUsed: formatBytes(used),
      formattedTotal: formatBytes(total),
      ...data
    };

  } catch (error) {
    console.error("Error getting storage info:", error);
    return null;
  }
}

// Вспомогательная функция для форматирования байтов (добавьте в начало файла)
function formatBytes(bytes) {
  if (!bytes && bytes !== 0) return '0 Bytes';
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}