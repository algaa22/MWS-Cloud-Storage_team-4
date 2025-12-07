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

export async function getUserInfo(token) {
  console.log("=== getUserInfo DEBUG ===");

  if (!token) {
    throw new Error("Invalid token provided");
  }

  const url = `${BASE}/users/info`;
  console.log("Request URL:", url);

  try {
    const res = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "X-Auth-Token": token
      }
    });

    console.log("Response status:", res.status, res.statusText);

    if (res.status === 400) {
      console.log("Token expired, attempting refresh...");
      const refreshToken = localStorage.getItem("refreshToken");

      if (refreshToken) {
        try {
          const newAccessToken = await refreshTokenRequest(refreshToken);
          console.log("Token refreshed, retrying user info...");

          // Повторяем запрос с новым токеном
          const retryRes = await fetch(url, {
            method: "GET",
            headers: {
              "Content-Type": "application/json",
              "X-Auth-Token": newAccessToken
            }
          });

          if (!retryRes.ok) {
            const errorText = await retryRes.text();
            throw new Error(`HTTP ${retryRes.status}: ${errorText}`);
          }

          const data = await retryRes.json();
          console.log("Success after refresh!");
          return data;

        } catch (refreshError) {
          console.error("Refresh failed:", refreshError);
          throw new Error(`Authentication failed: ${refreshError.message}`);
        }
      }
    }

    if (!res.ok) {
      const errorText = await res.text();
      console.error("Error response:", errorText);
      throw new Error(`HTTP ${res.status}: ${errorText}`);
    }

    const data = await res.json();
    console.log("Success! User data:", data);
    return data;

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

    const res = await fetch(`${BASE}/users/auth/logout`, {
      method: "POST",
      headers: {
        "X-Auth-Token": token
      }
    });

    console.log("Logout response status:", res.status);
    return res.ok;
  } catch (error) {
    console.error("Logout request failed:", error);
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
    const listResponse = await fetch(listUrl, {
      headers: {
        "X-Auth-Token": token,
        "Accept": "application/json"
      }
    });

    if (!listResponse.ok) {
      const errorText = await listResponse.text();
      throw new Error(`Server error ${listResponse.status}: ${errorText}`);
    }

    const data = await listResponse.json();
    const files = data?.files || data || [];

    console.log(`Found ${files.length} items`);

    // 2. Функция для получения информации о файле через GET
    const getFileInfo = async (filePath) => {
      try {
        const infoUrl = `${BASE}/files/info?path=${encodeURIComponent(filePath)}`;
        console.log(`Getting info for: ${filePath}`);

        const infoResponse = await fetch(infoUrl, {
          headers: {
            "X-Auth-Token": token,
            "Accept": "application/json"
          }
        });

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
      const name = path.split('/').pop() || `file_${i}`;

      // Определяем тип
      let type = "file";
      let size = 0;
      let fileInfo = null;

      if (path.endsWith('/')) {
        // Это папка
        type = "folder";
      } else {
        // Это файл - получаем информацию
        fileInfo = await getFileInfo(path);
        if (fileInfo) {
          size = fileInfo.Size || fileInfo.size || 0;
        }

        // Небольшая пауза между запросами
        if (i < files.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 50));
        }
      }

      result.push({
        name: name,
        path: path,
        type: type,
        size: size,
        id: item.id || path || Math.random().toString(),
        fullPath: path,
        _raw: item,
        _info: fileInfo // сохраняем полную информацию для отладки
      });

      console.log(`Processed ${i+1}/${files.length}: ${name} (${type}, ${size} bytes)`);
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
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "X-Auth-Token": token,
        "X-File-Tags": "user_upload",
        "X-File-Size": file.size,
        "Content-Type": file.type || "application/octet-stream"
      },
      body: file,
      signal: controller.signal
    });

    clearTimeout(timeoutId);
    const endTime = Date.now();
    console.log(`⏱️ Request took ${endTime - startTime}ms`);

    console.log("📥 Response received:", response.status, response.statusText);

    if (onProgress) onProgress(100);

    if (!response.ok) {
      const responseText = await response.text();
      console.error("❌ Upload failed:", responseText);
      throw new Error(`Upload failed: ${response.status} ${responseText}`);
    }

    const responseText = await response.text();
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
const uploadFileChunked = async (token, file, path, onProgress) => {
    console.log("Using modern fetch stream upload");

    const url = `${BASE}/files/upload?path=${encodeURIComponent(path)}`;
    const totalSize = file.size;
    let uploaded = 0;
    // Определите CHUNK_SIZE где-то глобально или передайте сюда
    const CHUNK_SIZE = 5 * 1024 * 1024; // Пример: 5MB

    try {
        const controller = new AbortController();
        // Добавляем таймаут для всего запроса (например, 2 минуты)
        const timeoutId = setTimeout(() => {
            console.error("Upload request timed out!");
            controller.abort();
        }, 120000); // 120 секунд

        const fileStream = new ReadableStream({
          async start(controller) {
            console.log("Stream started");

            for (let offset = 0; offset < totalSize; offset += CHUNK_SIZE) {
              console.log(`Preparing chunk from ${offset}`);
              const chunk = file.slice(offset, Math.min(offset + CHUNK_SIZE, totalSize));
              const arrayBuffer = await chunk.arrayBuffer();

              console.log(`Enqueuing chunk of ${arrayBuffer.byteLength} bytes`);
              controller.enqueue(new Uint8Array(arrayBuffer));

              // Добавьте небольшую задержку между чанками
              await new Promise(resolve => setTimeout(resolve, 1));
            }

            console.log("All chunks sent, closing stream");
            controller.close();
          }
        });

        console.log("📤 Sending request with stream body...");

        const response = await fetch(url, {
            method: "POST",
            headers: {
                "X-Auth-Token": token,
                "X-File-Tags": "user_upload",
                "X-File-Size": file.size,
                "Content-Type": "application/octet-stream" // Явно указываем тип
                // Браузер сам выберет Transfer-Encoding (Content-Length или chunked)
            },
            body: fileStream,
            signal: controller.signal,
            duplex: "half" // Необходимо для потоковой отправки тела
        });

        clearTimeout(timeoutId); // Очищаем таймаут после получения ответа

        console.log("📥 Response received:", response.status, response.statusText);

        if (onProgress) onProgress(100);

        if (!response.ok) {
            const errorText = await response.text();
            console.error("❌ Upload failed:", errorText);
            throw new Error(`Upload failed: ${response.status} ${errorText}`);
        }

        // Возвращаем JSON-ответ от сервера
        return await response.json();

    } catch (error) {
        console.error("🔥 Fetch error:", error);
        // Добавьте сюда логику очистки таймаута, если ошибка произошла до fetch()
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

  const res = await fetch(`${BASE}/files?path=${encodeURIComponent(path)}`, {
    method: "DELETE",
    headers: { "X-Auth-Token": token }
  });

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

  const res = await fetch(`${BASE}/files?path=${encodeURIComponent(oldPath)}&newPath=${encodeURIComponent(newPath)}`, {
    method: "PUT",
    headers: {
      "X-Auth-Token": token
    }
  });

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
/**
 * getFileInfo - Получение полной информации о файле
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
    const response = await fetch(url, {
      headers: {
        "X-Auth-Token": token,
        "Accept": "application/json",
        "Content-Type": "application/json"
      }
    });

    console.log("Response status:", response.status, response.statusText);
    console.log("Response headers:", Object.fromEntries(response.headers.entries()));

    if (!response.ok) {
      const errorText = await response.text().catch(() => "(no body)");
      console.error("Error response:", errorText);
      throw new Error(`Failed to get file info: ${response.status} ${errorText}`);
    }

    const data = await response.json();
    console.log("Raw response from server:", data);

    // ВАЖНО: Сервер возвращает данные с заглавными буквами!
    // "Path", "Type", "Size", "Visibility" и т.д.

    // Форматируем ответ в удобный вид
    const fileInfo = {
      // Основная информация
      name: data.name || data.Name || path.split('/').pop() || "unknown",
      path: data.path || data.Path || path,
      size: data.size || data.Size || 0,
      type: data.type || data.Type || "unknown",
      mimeType: data.mimeType || data.MimeType || data.Type || "application/octet-stream",

      // Дополнительная информация
      visibility: data.visibility || data.Visibility || "private",
      isolated: data.isolated || data.Isolated || false,
      tags: data.tags || data.Tags || "",

      // Даты (если есть)
      createdAt: data.createdAt || data.CreatedAt || data.created_at,
      updatedAt: data.updatedAt || data.UpdatedAt || data.updated_at,
      lastModified: data.lastModified || data.LastModified,

      // Размер в читаемом формате
      formattedSize: formatFileSize(data.size || data.Size || 0),

      // Исходные данные для отладки
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

  const res = await fetch(`${BASE}/directories?path=${encodeURIComponent(folderPath)}`, {
    method: "PUT", // Согласно документации: PUT для создания папки
    headers: { "X-Auth-Token": token }
  });

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

  const res = await fetch(`${BASE}/directories?path=${encodeURIComponent(folderPath)}`, {
    method: "DELETE",
    headers: { "X-Auth-Token": token }
  });

  console.log("deleteFolder status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Failed to delete folder:", res.status, txt);
    throw new Error(`Failed to delete folder: ${res.status} ${txt}`);
  }
  return true;
};

/**
 * updateUserInfo
 */
export const updateUserInfo = async (token, updates) => {
  console.log("updateUserInfo request:", updates);

  const headers = {
    "Content-Type": "application/json",
    "X-Auth-Token": token
  };

  if (updates.newUsername) {
    headers["X-New-Username"] = updates.newUsername;
  }
  if (updates.oldPassword && updates.newPassword) {
    headers["X-Old-Password"] = updates.oldPassword;
    headers["X-New-Password"] = updates.newPassword;
  }

  const res = await fetch(`${BASE}/users/update`, {
    method: "POST",
    headers
  });

  console.log("updateUserInfo status:", res.status);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    throw new Error(`Update failed: ${res.status} ${txt}`);
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
    throw error;
  }
}