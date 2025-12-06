// src/api.js
const BASE = "http://localhost:8081/api";

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
 * LOGIN - Используем только заголовки, как требует сервер
 */
// Исправленная функция loginRequest в api.js:
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

    // ВОТ КЛЮЧЕВАЯ ЧАСТЬ: получаем AccessToken точно как пришел
    const token = data?.AccessToken;

    if (!token) {
      console.error("No AccessToken in response!");
      throw new Error("Server did not return AccessToken");
    }

    console.log("=== TOKEN DEBUG ===");
    console.log("Original token from server:", token);
    console.log("Token length:", token.length);
    console.log("Token type:", typeof token);

    // Проверяем, не обернут ли токен в кавычки
    if ((token.startsWith('"') && token.endsWith('"')) ||
        (token.startsWith("'") && token.endsWith("'"))) {
      console.log("Token is wrapped in quotes, removing them");
      const unwrapped = token.slice(1, -1);
      console.log("Unwrapped token:", unwrapped);
      console.log("Unwrapped length:", unwrapped.length);
      return unwrapped;
    }

    // Возвращаем токен как есть
    return token;

  } catch (error) {
    console.error("Login request failed:", error);
    throw error;
  }
}

/**
 * REGISTER - Используем заголовки
 */
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
  console.log("Token received:", token);
  console.log("Token length:", token?.length);

  // Проверяем, может токен в кавычках?
  if (token && typeof token === 'string') {
    console.log("First char:", token[0]);
    console.log("Last char:", token[token.length - 1]);

    // Если токен в кавычках, удаляем их
    let cleanToken = token;
    if ((token.startsWith('"') && token.endsWith('"')) ||
        (token.startsWith("'") && token.endsWith("'"))) {
      cleanToken = token.substring(1, token.length - 1);
      console.log("Removed quotes, new token:", cleanToken);
    }

    // Также убираем возможные переносы строк
    cleanToken = cleanToken.replace(/\n/g, '').replace(/\r/g, '').trim();
    console.log("Final token to send:", cleanToken);

    const url = `${BASE}/users/info`;
    console.log("Request URL:", url);

    try {
      const res = await fetch(url, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "X-Auth-Token": cleanToken
        }
      });

      console.log("Response status:", res.status, res.statusText);

      if (!res.ok) {
        const errorText = await res.text().catch(() => "(no body)");
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

  throw new Error("Invalid token provided");
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

  const formData = new FormData();
  formData.append("file", file);

  // Посмотрим, что внутри FormData
  console.log("FormData entries:");
  for (let [key, value] of formData.entries()) {
    console.log(`  ${key}:`, value);
  }

  const url = `${BASE}/files/upload?path=${encodeURIComponent(path || "/")}`;
  console.log("📤 Upload URL:", url);

  try {
    // Создаем контроллер для отслеживания запроса
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      console.warn("⚠️ Upload timeout after 30 seconds");
      controller.abort();
    }, 30000);

    console.log("🔄 Sending fetch request...");

    const startTime = Date.now();
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "X-Auth-Token": token,
        "X-File-Tags": "user_upload"
        // НЕ добавляем Content-Type для FormData!
      },
      body: formData,
      signal: controller.signal
    });

    clearTimeout(timeoutId);
    const endTime = Date.now();
    console.log(`⏱️ Request took ${endTime - startTime}ms`);

    console.log("📥 Response received:");
    console.log("- Status:", response.status, response.statusText);
    console.log("- Headers:", Object.fromEntries(response.headers.entries()));
    console.log("- OK?:", response.ok);

    // Получаем ответ как текст сначала
    const responseText = await response.text();
    console.log("- Response text length:", responseText.length);
    console.log("- Response text (first 500 chars):", responseText.substring(0, 500));

    if (!response.ok) {
      console.error("❌ Upload failed!");
      console.error("Full error response:", responseText);
      throw new Error(`Upload failed: ${response.status} ${responseText}`);
    }

    // Пытаемся парсить как JSON
    let result;
    try {
      result = JSON.parse(responseText);
      console.log("✅ Upload successful! Parsed result:", result);
    } catch (e) {
      console.log("⚠️ Response is not valid JSON, returning as text");
      result = responseText;
    }

    return result;

  } catch (error) {
    console.error("🔥 Fetch error in uploadFile:", error);
    console.error("Error name:", error.name);
    console.error("Error message:", error.message);

    if (error.name === 'AbortError') {
      throw new Error("Upload timeout - возможно, файл слишком большой или проблемы с сетью");
    }
    throw error;
  }
};

/**
 * downloadFile
 */
export const downloadFile = async (token, path, filename) => {
  console.log("downloadFile request:", { path, filename });

  const url = `${BASE}/files?path=${encodeURIComponent(path)}`;
  const res = await fetch(url, {
    headers: {
      "X-Auth-Token": token
    }
  });

  console.log("downloadFile status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Download failed:", res.status, txt);
    throw new Error(`Download failed: ${res.status} ${txt}`);
  }

  const blob = await res.blob();
  const urlBlob = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = urlBlob;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(urlBlob);
  document.body.removeChild(a);
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

  const res = await fetch(`${BASE}/files?path=${encodeURIComponent(oldPath)}`, {
    method: "PUT",
    headers: {
      "X-Auth-Token": token,
      "X-File-New-Path": newPath
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