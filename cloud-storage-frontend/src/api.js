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

/**
 * Get user info
 */
// В файле api.js исправьте функцию getUserInfo:

/**
 * Get user info - используем правильный endpoint из документации
 */
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
  console.log("getFiles called with path:", currentPath || "(root)");

  if (!token) {
    console.error("No token provided to getFiles");
    throw new Error("Требуется авторизация");
  }

  // Собираем параметры согласно документации
  const params = new URLSearchParams();
  params.append("includeDirectories", "true");

  if (currentPath) {
    params.append("directory", currentPath);
  }

  const url = `${BASE}/files/list?${params.toString()}`;
  console.log("Request URL:", url);

  try {
    const response = await fetch(url, {
      headers: {
        "X-Auth-Token": token,
        "Accept": "application/json"
      }
    });

    console.log("Response status:", response.status, response.statusText);

    if (!response.ok) {
      const errorText = await response.text().catch(() => "(no body)");
      console.error("Server error:", response.status, errorText);
      throw new Error(`Server error ${response.status}: ${errorText}`);
    }

    const data = await parseJsonSafe(response);
    console.log("Raw response:", data);

    // Обрабатываем ответ
    if (Array.isArray(data)) {
      return data.map(item => ({
        name: item.name || "",
        path: item.path || "",
        type: item.type || (item.name && item.name.endsWith("/") ? "folder" : "file"),
        size: item.size || 0,
        id: item.id || item.path || Math.random().toString(),
        fullPath: item.path || ""
      }));
    } else if (data && Array.isArray(data.files)) {
      return data.files.map(item => ({
        name: item.name || "",
        path: item.path || "",
        type: item.type || (item.name && item.name.endsWith("/") ? "folder" : "file"),
        size: item.size || 0,
        id: item.id || item.path || Math.random().toString(),
        fullPath: item.path || ""
      }));
    }

    console.warn("Unexpected response format:", data);
    return [];

  } catch (err) {
    console.error("Fetch error in getFiles:", err);
    throw err;
  }
};

/**
 * uploadFile
 */
export const uploadFile = async (token, file, path, onProgress) => {
  console.log("uploadFile request:", { path, file: file?.name });

  const formData = new FormData();
  formData.append("file", file);

  const url = `${BASE}/files/upload?path=${encodeURIComponent(path)}`;
  console.log("Upload URL:", url);

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: {
        "X-Auth-Token": token,
        "X-File-Tags": "user_upload"
      },
      body: formData
    });

    console.log("uploadFile status:", res.status, res.statusText);

    if (!res.ok) {
      const txt = await res.text().catch(() => "(no body)");
      console.error("Upload failed:", res.status, txt);
      throw new Error(`Upload failed: ${res.status} ${txt}`);
    }

    return await res.json();
  } catch (err) {
    console.error("Upload error:", err);
    throw err;
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
      "X-Auth-Token": token,
      "X-Download-Mode": "chunked"
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
export const getFileInfo = async (token, path) => {
  console.log("getFileInfo request:", { path });

  const res = await fetch(`${BASE}/files/info?path=${encodeURIComponent(path)}`, {
    headers: {
      "X-Auth-Token": token,
      "Accept": "application/json"
    }
  });

  console.log("getFileInfo status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Failed to get file info:", res.status, txt);
    throw new Error(`Failed to get file info: ${res.status} ${txt}`);
  }
  return res.json();
};

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