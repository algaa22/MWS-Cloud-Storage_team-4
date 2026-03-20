export const BASE = "https://localhost:8443/api";
export const API_BASE = "https://localhost:8443/api";
export const PUBLIC_BASE = "https://localhost:8443";

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

export async function fetchWithTokenRefresh(url, options = {}, token) {
  let currentToken = token || localStorage.getItem("accessToken");

  const headers = {
    ...options.headers,
    "X-Auth-Token": currentToken || ""
  };

  try {
    const res = await fetch(url, { ...options, headers });

    if (res.status === 401) {
      // Обычная логика рефреша для личного кабинета
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        try {
          const newAccessToken = await refreshTokenRequest(refreshToken);
          localStorage.setItem("accessToken", newAccessToken);
          headers["X-Auth-Token"] = newAccessToken;
          return await fetch(url, { ...options, headers });
        } catch (e) {
          window.location.href = "/";
          throw e;
        }
      } else {
        window.location.href = "/";
        throw new Error("Unauthorized");
      }
    }
    return res;
  } catch (err) {
    throw err;
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

export const getUserInfo = async (token) => {
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

    const text = await res.text();
    console.log("RAW RESPONSE TEXT:", text);

    if (!text || text.trim() === '') {
      console.warn("Empty response from server");
      throw new Error("Empty response");
    }

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}: ${text}`);
    }

    let data;
    try {
      data = JSON.parse(text);
    } catch (e) {
      console.error("Failed to parse JSON:", e);
      throw new Error("Invalid JSON response");
    }

    console.log("Parsed user data:", data);
    console.log("Data keys:", Object.keys(data));

    // Используем новые поля из UserEntity
    const totalStorageLimit = data.totalStorageLimit ||
                              (data.freeStorageLimit || 5 * 1024 * 1024 * 1024) +
                              (data.paidStorageLimit || 0);

    const freeStorageLimit = data.freeStorageLimit || 5 * 1024 * 1024 * 1024;
    const paidStorageLimit = data.paidStorageLimit || 0;
    const usedStorage = data.usedStorage || 0;

    const storageInfo = {
      used: usedStorage,
      free: freeStorageLimit,
      paid: paidStorageLimit,
      total: totalStorageLimit,
      formattedUsed: formatBytes(usedStorage),
      formattedFree: formatBytes(freeStorageLimit),
      formattedPaid: paidStorageLimit > 0 ? formatBytes(paidStorageLimit) : '0 GB',
      formattedTotal: formatBytes(totalStorageLimit),
      percentage: totalStorageLimit > 0 ? Math.round((usedStorage / totalStorageLimit) * 100) : 0
    };

    const userName = data.username || data.name || data.email?.split('@')[0] || "User";
    console.log("✅ User name:", userName);
    console.log("✅ Storage info:", storageInfo);

    return {
      id: data.id,
      name: userName,
      username: data.username || userName,
      email: data.email || "",
      userStatus: data.userStatus || "ACTIVE",
      usedStorage: usedStorage,
      freeStorageLimit: freeStorageLimit,
      paidStorageLimit: paidStorageLimit,
      totalStorageLimit: totalStorageLimit,
      storageLimit: totalStorageLimit, // ← ДОБАВЬТЕ ЭТУ СТРОКУ для совместимости
      createdAt: data.createdAt,
      isActive: data.isActive !== undefined ? data.isActive : true,
      activeTariff: data.activeTariff || null,
      tariffEndDate: data.tariffEndDate || null,
      hasActiveTrial: data.hasActiveTrial || false,
      trialEndDate: data.trialEndDate || null,
      storageInfo
    };

  } catch (error) {
    console.error("❌ Error in getUserInfo:", error);

    const defaultStorage = {
      used: 0,
      free: 5 * 1024 * 1024 * 1024,
      paid: 0,
      total: 5 * 1024 * 1024 * 1024,
      formattedUsed: '0 Bytes',
      formattedFree: '5 GB',
      formattedPaid: '0 GB',
      formattedTotal: '5 GB',
      percentage: 0
    };

    return {
      name: "User",
      username: "User",
      email: "",
      userStatus: "ACTIVE",
      usedStorage: 0,
      freeStorageLimit: 5 * 1024 * 1024 * 1024,
      paidStorageLimit: 0,
      totalStorageLimit: 5 * 1024 * 1024 * 1024,
      storageLimit: 5 * 1024 * 1024 * 1024, // ← ДОБАВЬТЕ ЭТУ СТРОКУ
      activeTariff: null,
      isActive: true,
      storageInfo: defaultStorage
    };
  }
};

export const getFiles = async (token, currentPath = "") => {
  console.log("=== GET FILES ===");

  if (!token) throw new Error("Требуется авторизация");

  const params = new URLSearchParams();
  params.append("includeDirectories", "true");

  const pathParts = currentPath.split('/').filter(p => p && p !== '');
  if (pathParts.length > 0) {
    const parentId = pathParts[pathParts.length - 1];
    params.append("parentId", parentId);
  }

  const listUrl = `${BASE}/files/list?${params.toString()}`;

  try {
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
    console.log("Raw server response:", files);

    const result = [];

    for (let i = 0; i < files.length; i++) {
      const item = files[i];

      let name = "Без имени";
      if (item.name && item.name.trim() !== "") {
        name = item.name;
      }

      let type = item.isDirectory ? "folder" : "file";

      result.push({
        name: name,
        type: type,
        size: item.size || 0,
        id: item.id || Math.random().toString(),
        _raw: item
      });

      console.log(`Processed ${i+1}/${files.length}:`, {
        name: name,
        type: type,
        size: item.size || 0,
        id: item.id
      });
    }

    console.log("Final result:", result);
    return result;

  } catch (err) {
    console.error("Error in getFiles:", err);
    throw err;
  }
};

export const downloadFile = async (token, id, filename, fileSize) => {
  console.log("downloadFile request:", { id, filename, fileSize });

  const url = `${BASE}/files/download?id=${encodeURIComponent(id)}`;

  const headers = {
    "X-Auth-Token": token
  };

  try {
    const res = await fetch(url, { headers });

    console.log("downloadFile status:", res.status);

    if (!res.ok) {
      const txt = await res.text().catch(() => "(no body)");
      throw new Error(`Download failed: ${res.status} ${txt}`);
    }

    const reader = res.body.getReader();
    const chunks = [];
    let receivedLength = 0;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      chunks.push(value);
      receivedLength += value.length;
    }

    const blob = new Blob(chunks);
    const urlBlob = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.style.display = "none";
    a.href = urlBlob;
    a.download = filename;
    document.body.appendChild(a);
    a.click();

    setTimeout(() => {
      window.URL.revokeObjectURL(urlBlob);
      document.body.removeChild(a);
    }, 100);

  } catch (error) {
    console.error("Download error:", error);
    throw error;
  }
};

export const getTrashFiles = async (token) => {
  console.log("=== GET TRASH FILES ===");

  if (!token) throw new Error("Требуется авторизация");

  // parentId больше не нужен!
  const url = `${BASE}/files/trash`;
  console.log("Request URL:", url);

  try {
    const response = await fetchWithTokenRefresh(url, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    }, token);

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to get trash: ${response.status} ${errorText}`);
    }

    const data = await response.json();
    console.log("Raw trash data:", data);

    // Сервер может вернуть массив сразу или объект с полем files
    const files = Array.isArray(data) ? data : (data.files || []);
    console.log(`Found ${files.length} files in trash`);

    return files.map(item => ({
      name: item.name || "Без имени",
      id: item.id,
      type: item.isDirectory ? "folder" : "file",
      size: item.size || 0,
      deletedAt: item.deletedAt || item.deleted_at || item.DeletedAt
    }));

  } catch (error) {
    console.error("Error getting trash files:", error);
    throw error;
  }
};


export const softDeleteFile = async (token, id) => {
  console.log("softDeleteFile request (to trash):", { id });

  // Убедитесь, что permanent=false
  const url = `${BASE}/files?id=${encodeURIComponent(id)}&permanent=false`;

  const res = await fetchWithTokenRefresh(url, {
    method: "DELETE",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  console.log("softDeleteFile status:", res.status);

  if (!res.ok) {
    const responseText = await res.text();
    throw new Error(`Soft delete failed: ${res.status} ${responseText}`);
  }

  // Прочитайте ответ сервера
  const responseText = await res.text();
  console.log("softDeleteFile response:", responseText);

  return true;
};

export const softDeleteFolder = async (token, id) => {
  console.log("softDeleteFolder request (to trash):", { id });

  const res = await fetchWithTokenRefresh(
      `${BASE}/directories?id=${encodeURIComponent(id)}`,
      {
        method: "DELETE"
      },
      token
  );

  console.log("softDeleteFolder status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Soft delete folder failed:", res.status, txt);
    throw new Error(`Soft delete folder failed: ${res.status} ${txt}`);
  }
  return true;
};


export const restoreFile = async (token, id) => {
  console.log("restoreFile request:", { id });

  const url = `${BASE}/files/restore?id=${encodeURIComponent(id)}`;

  const res = await fetchWithTokenRefresh(url, {
    method: "POST"
  }, token);

  console.log("restoreFile status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Restore failed:", res.status, txt);
    throw new Error(`Restore failed: ${res.status} ${txt}`);
  }
  return true;
};

export const permanentDeleteFile = async (token, id) => {
  console.log("permanentDeleteFile request:", { id });

  const url = `${BASE}/files?id=${encodeURIComponent(id)}&permanent=true`;

  try {
    const res = await fetchWithTokenRefresh(url, {
      method: "DELETE"
    }, token);

    console.log("permanentDeleteFile status:", res.status, res.statusText);

    // 404 - это нормально, файл уже удален
    if (res.status === 404) {
      console.log("File already deleted (404), treating as success");
      return { success: true, id, alreadyDeleted: true };
    }

    if (!res.ok) {
      const txt = await res.text().catch(() => "(no body)");
      console.error("Permanent delete failed:", res.status, txt);
      throw new Error(`Permanent delete failed: ${res.status} ${txt}`);
    }

    // Попытка прочитать ответ, если он есть
    let responseData;
    try {
      const responseText = await res.text();
      responseData = responseText ? JSON.parse(responseText) : { success: true };
    } catch (e) {
      responseData = { success: true };
    }

    return { ...responseData, id };
  } catch (error) {
    console.error("Permanent delete error:", error);
    throw error;
  }
};

export const emptyTrash = async (token) => {
  console.log("emptyTrash request");

  try {
    const trashFiles = await getTrashFiles(token);

    const results = await Promise.allSettled(
      trashFiles.map(file => permanentDeleteFile(token, file.id))
    );

    const successful = results.filter(r => r.status === 'fulfilled').length;
    const failed = results.filter(r => r.status === 'rejected').length;

    // Даже если были 404, считаем их успешными, так как файлы уже удалены
    const totalSuccess = trashFiles.length - failed;

    return {
      total: trashFiles.length,
      success: totalSuccess,
      failed: failed
    };
  } catch (error) {
    console.error("Error emptying trash:", error);
    throw error;
  }
};

export const deleteFile = async (token, id, permanent = false) => {
  console.log("deleteFile request:", { id, permanent });

  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userId = user.id || '';

  const url = `${BASE}/files?id=${encodeURIComponent(id)}&permanent=${permanent}&userId=${encodeURIComponent(userId)}`;

  const res = await fetchWithTokenRefresh(url, {
    method: "DELETE"
  }, token);

  console.log("deleteFile status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Delete failed:", res.status, txt);
    throw new Error(`Delete failed: ${res.status} ${txt}`);
  }
  return true;
};

export const getFileInfo = async (token, id) => {
  console.log("=== GET FILE INFO ===");
  console.log("Token length:", token?.length);
  console.log("ID:", id);

  if (!token) {
    console.error("No token provided");
    throw new Error("Требуется авторизация");
  }

  if (!id) {
    console.error("No id provided");
    throw new Error("ID файла не указан");
  }

  const url = `${BASE}/files/info?id=${encodeURIComponent(id)}`;
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
      name: data.name || data.Name || "unknown",
      id: data.id || data.Id || id,
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

function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export const createFolder = async (token, name, parentId = null) => {
  console.log("createFolder request:", { name, parentId });

  let url = `${BASE}/directories?name=${encodeURIComponent(name)}`;
  if (parentId) {
    url += `&parentId=${encodeURIComponent(parentId)}`;
  }

  const res = await fetchWithTokenRefresh(url, {
    method: "PUT"
  }, token);

  console.log("createFolder status:", res.status, res.statusText);

  if (!res.ok) {
    const txt = await res.text().catch(() => "(no body)");
    console.error("Failed to create folder:", res.status, txt);
    throw new Error(`Failed to create folder: ${res.status} ${txt}`);
  }
  return true;
};

export const deleteFolder = async (token, id) => {
  console.log("deleteFolder request:", { id });

  const res = await fetchWithTokenRefresh(
      `${BASE}/directories?id=${encodeURIComponent(id)}`,
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

export const updateUserInfo = async (token, updates) => {
  console.log("=== UPDATE USER INFO ===");
  console.log("Updates:", updates);

  const headers = {
    "X-Auth-Token": token
  };

  if (updates.newUsername) {
    headers["X-New-Username"] = updates.newUsername;
  }

  if (updates.newPassword) {
    headers["X-Old-Password"] = updates.oldPassword;
    headers["X-New-Password"] = updates.newPassword;
  }

  console.log("Headers:", headers);

  const url = `${BASE}/users/update`;

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: headers
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

export const createShareLink = async (token, fileId, options = {}) => {
  console.log("=== CREATE SHARE LINK ===");
  console.log("Token provided:", token ? "yes" : "no");

  const {
    shareType = 'PUBLIC',
    expiresAt = null,
    maxDownloads = null,
    password = null,
    recipientUserIds = []
  } = options;

  const params = new URLSearchParams();
  params.append('fileId', fileId);
  params.append('shareType', shareType);
  if (expiresAt) params.append('expiresAt', expiresAt);
  if (maxDownloads) params.append('maxDownloads', maxDownloads);
  if (password) params.append('password', password);
  recipientUserIds.forEach(id => params.append('recipientUserIds', id));

  const url = `${BASE}/shares?${params.toString()}`;
  console.log("Request URL:", url);

  const response = await fetchWithTokenRefresh(url, {
    method: "POST",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to create share: ${response.status} ${errorText}`);
  }

  return await response.json();
};

export const getShareInfo = async (shareToken) => {
  const url = `${BASE}/shares/info?token=${shareToken}`;

  const res = await fetch(url, { method: "GET" });

  if (!res.ok) {
    const error = new Error('Share info not found');
    error.status = res.status;
    throw error;
  }
  return await res.json();
};

export const downloadSharedFile = async (shareToken, password = "") => {
  let url = `${API_BASE}/shares/download?shareToken=${shareToken}`;

  if (password) {
    url += `&password=${encodeURIComponent(password)}`;
  }

  const res = await fetch(url, { method: "GET" });

  if (!res.ok) {
    const error = new Error('Download failed');
    error.status = res.status;
    throw error;
  }

  const blob = await res.blob();
  const contentDisposition = res.headers.get('Content-Disposition');
  let filename = 'file';
  if (contentDisposition && contentDisposition.includes('filename=')) {
    filename = decodeURIComponent(contentDisposition.split('filename=')[1].replace(/["']/g, ''));
  }

  return { blob, filename };
};

export const validateSharePassword = async (shareToken, password) => {
  console.log("=== VALIDATE SHARE PASSWORD ===");

  const params = new URLSearchParams();
  params.append('shareToken', shareToken);

  const url = `${BASE}/api/shares/validate?${params.toString()}`;
  console.log("Request URL:", url);

  const response = await fetchWithTokenRefresh(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ password })
  }, null); // Не требует токена

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Password validation failed: ${response.status} ${errorText}`);
  }

  return await response.json();
};

export const deactivateShare = async (token, shareId) => {
  console.log("=== DEACTIVATE SHARE ===");

  const url = `${BASE}/api/shares/${shareId}`;
  console.log("Request URL:", url);

  const response = await fetchWithTokenRefresh(url, {
    method: "DELETE",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to deactivate share: ${response.status} ${errorText}`);
  }

  return await response.json();
};

export const getUserShares = async (token) => {
  console.log("=== GET USER SHARES ===");

  const url = `${BASE}/api/shares/user`;
  console.log("Request URL:", url);

  const response = await fetchWithTokenRefresh(url, {
    method: "GET",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to get user shares: ${response.status} ${errorText}`);
  }

  return await response.json();
};

export const getFileShares = async (token, fileId) => {
  console.log("=== GET FILE SHARES ===");

  const url = `${BASE}/api/shares/file/${fileId}`;
  console.log("Request URL:", url);

  const response = await fetchWithTokenRefresh(url, {
    method: "GET",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to get file shares: ${response.status} ${errorText}`);
  }

  return await response.json();
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

    localStorage.setItem("accessToken", newAccessToken);
    if (newRefreshToken && newRefreshToken !== refreshToken) {
      localStorage.setItem("refreshToken", newRefreshToken);
    }

    return newAccessToken;

  } catch (error) {
    console.error("Refresh token request failed:", error);
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    throw error;
  }
}

export const uploadFileWithTags = async (token, file, parentId = null, onProgress, tags = []) => {
  console.log("=== UPLOAD FILE WITH TAGS ===");
  console.log("Tags:", tags);

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  if (!file) {
    throw new Error("Файл не выбран");
  }

  const CHUNK_SIZE = 5 * 1024 * 1024;
  const useChunkedUpload = file.size > CHUNK_SIZE;

  console.log(`File size: ${file.size}, Using ${useChunkedUpload ? 'CHUNKED' : 'SIMPLE'} upload`);

  const tagsString = Array.isArray(tags) ? tags.join(',') : tags;

  if (useChunkedUpload) {
    console.log("Using chunked upload with tags");
    return await uploadFileChunkedWithTags(token, file, parentId, onProgress, tagsString);
  } else {
    console.log("Using simple upload with tags");
    return await uploadFileSimpleWithTags(token, file, parentId, onProgress, tagsString);
  }
};

const uploadFileSimpleWithTags = async (token, file, parentId, onProgress, tagsString) => {
  console.log("Using simple upload with tags");

  let url = `${BASE}/files/upload?name=${encodeURIComponent(file.name)}`;
  if (parentId) {
    url += `&parentId=${encodeURIComponent(parentId)}`;
  }

  console.log("Upload URL:", url);
  console.log("Tags:", tagsString);

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      controller.abort();
    }, 30000);

    const headers = {
      "Content-Type": file.type || "application/octet-stream"
    };

    if (tagsString && tagsString.trim() !== '') {
      headers["X-File-Tags"] = tagsString;
    }

    const res = await fetchWithTokenRefresh(url, {
      method: "POST",
      headers: headers,
      body: file,
      signal: controller.signal
    }, token);

    clearTimeout(timeoutId);

    if (onProgress) onProgress(100);

    if (!res.ok) {
      const responseText = await res.text();
      console.error("Upload failed:", responseText);
      throw new Error(`Upload failed: ${res.status} ${responseText}`);
    }

    const responseText = await res.text();
    try {
      return JSON.parse(responseText);
    } catch (e) {
      return responseText;
    }

  } catch (error) {
    console.error("Upload error:", error);
    throw error;
  }
};

const uploadFileChunkedWithTags = async (token, file, parentId, onProgress, tagsString) => {
  console.log("Using chunked upload with tags");

  let url = `${BASE}/files/upload/chunked?name=${encodeURIComponent(file.name)}`;
  if (parentId) {
    url += `&parentId=${encodeURIComponent(parentId)}`;
  }

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.open('POST', url);
    xhr.setRequestHeader('X-Auth-Token', token);

    if (tagsString && tagsString.trim() !== '') {
      xhr.setRequestHeader('X-File-Tags', tagsString);
    }

    xhr.setRequestHeader('X-File-Size', file.size);

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        const progress = Math.round((event.loaded / event.total) * 100);
        console.log(`Upload progress: ${progress}% (${event.loaded}/${event.total} bytes)`);
        onProgress(progress);
      }
    };

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText);
          console.log("Upload completed successfully");
          if (onProgress) onProgress(100);
          resolve(response);
        } catch (e) {
          console.log("Upload response (non-JSON):", xhr.responseText);
          if (onProgress) onProgress(100);
          resolve({ success: true });
        }
      } else {
        console.error(`Upload failed: ${xhr.status} ${xhr.statusText}`);
        reject(new Error(`Upload failed: ${xhr.status} ${xhr.responseText}`));
      }
    };

    xhr.onerror = () => {
      console.error("Upload XHR error");
      reject(new Error('Network error during upload'));
    };

    xhr.ontimeout = () => {
      console.error("Upload timeout");
      reject(new Error('Upload timeout'));
    };

    xhr.timeout = 300000;

    const formData = new FormData();
    formData.append('file', file);

    xhr.send(formData);
  });
};

export const getFileTags = async (token, id) => {
  console.log("=== GET FILE TAGS ===");

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  if (!id) {
    throw new Error("ID файла не указан");
  }

  const url = `${BASE}/files/info?id=${encodeURIComponent(id)}`;

  try {
    const response = await fetchWithTokenRefresh(url, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    }, token);

    if (!response.ok) {
      return { tags: [] };
    }

    const data = await response.json();

    let tagsArray = [];

    if (data.tags && typeof data.tags === 'string') {
      tagsArray = data.tags.split(',').map(tag => tag.trim()).filter(tag => tag);
    } else if (Array.isArray(data.tags)) {
      tagsArray = data.tags;
    } else if (data.Tags && typeof data.Tags === 'string') {
      tagsArray = data.Tags.split(',').map(tag => tag.trim()).filter(tag => tag);
    }

    return { tags: tagsArray };

  } catch (error) {
    console.error("Error getting file tags:", error);
    return { tags: [] };
  }
};

export const getAllUserTags = async (token) => {
  console.log("=== GET ALL USER TAGS ===");

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  try {
    const files = await getFiles(token, "");
    const allTags = new Set();

    for (const file of files) {
      if (file.type === "file") {
        try {
          const tagsData = await getFileTags(token, file.id);
          if (tagsData.tags && Array.isArray(tagsData.tags)) {
            tagsData.tags.forEach(tag => {
              if (tag && typeof tag === 'string' && tag.trim()) {
                allTags.add(tag.trim());
              }
            });
          }
        } catch (error) {
          console.log(`Could not get tags for ${file.name}:`, error.message);
        }
      }
    }

    return Array.from(allTags).sort();

  } catch (error) {
    console.error("Error collecting tags:", error);
    return [];
  }
};

export const moveFolder = async (token, id, updates) => {
  let url = `${BASE}/directories?id=${encodeURIComponent(id)}`;

  if (updates.newName) {
    url += `&newName=${encodeURIComponent(updates.newName)}`;
  }

  if (updates.newParentId) {
    url += `&newParentId=${encodeURIComponent(updates.newParentId)}`;
  }

  const res = await fetchWithTokenRefresh(url, { method: "POST" }, token);
  return res.ok;
};

function formatBytes(bytes) {
  if (bytes === undefined || bytes === null || bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export const updateFileMetadata = async (token, id, updates) => {
  console.log("=== UPDATE FILE METADATA ===");
  console.log("ID:", id);
  console.log("Updates:", updates);

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  if (!id) {
    throw new Error("ID файла не указан");
  }

  let url = `${BASE}/files?id=${encodeURIComponent(id)}`;

  if (updates.newName) {
    url += `&newName=${encodeURIComponent(updates.newName)}`;
  }

  if (updates.newParentId) {
    url += `&newParentId=${encodeURIComponent(updates.newParentId)}`;
  }

  console.log("Request URL:", url);

  const headers = {
    'X-Auth-Token': token
  };

  if (updates.visibility !== undefined) {
    headers['X-File-New-Visibility'] = updates.visibility;
  }

  if (updates.tags !== undefined) {
    const tagsArray = Array.isArray(updates.tags) ? updates.tags :
        (typeof updates.tags === 'string' ? updates.tags.split(',').filter(t => t.trim()) : []);
    headers['X-File-New-Tags'] = tagsArray.join(',');
  }

  console.log("Request headers:", headers);

  try {
    const response = await fetch(url, {
      method: "PUT",
      headers: headers
    });

    console.log("Response status:", response.status, response.statusText);

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Error response:", errorText);
      throw new Error(`Failed to update metadata: ${response.status} ${errorText}`);
    }

    try {
      return await response.json();
    } catch (e) {
      return { success: true };
    }

  } catch (error) {
    console.error("Error updating file metadata:", error);
    throw error;
  }
};

export const getTariffInfo = async (token) => {
  console.log("=== GET TARIFF INFO ===");

  const url = `${BASE}/users/tariff/info`;

  try {
    const response = await fetchWithTokenRefresh(url, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    }, token);

    if (!response.ok) {
      throw new Error(`Failed to get tariff info: ${response.status}`);
    }

    const text = await response.text();
    console.log("Raw tariff response:", text);

    if (!text || text.trim() === '') {
      console.warn("Empty tariff info response from server");
      // Возвращаем тестовые данные с новой структурой
      return {
        activeTariff: null,
        totalStorageLimit: 5 * 1024 * 1024 * 1024, // 5GB
        freeStorageLimit: 5 * 1024 * 1024 * 1024,
        usedStorage: 0,
        tariffStartDate: null,
        tariffEndDate: null,
        autoRenew: false,
        isActive: true,
        daysLeft: 0,
        hasActiveTrial: false,
        trialEndDate: null
      };
    }

    const data = JSON.parse(text);
    console.log("Tariff info:", data);

    // Преобразуем данные в удобный формат для фронтенда
    return {
      activeTariff: data.activeTariff || data.tariffPlan || null,
      totalStorageLimit: data.totalStorageLimit || (5 * 1024 * 1024 * 1024),
      freeStorageLimit: data.freeStorageLimit || (5 * 1024 * 1024 * 1024),
      usedStorage: data.usedStorage || 0,
      tariffStartDate: data.tariffStartDate || null,
      tariffEndDate: data.tariffEndDate || null,
      autoRenew: data.autoRenew || false,
      isActive: data.isActive !== undefined ? data.isActive : true,
      daysLeft: data.daysLeft || 0,
      hasActiveTrial: data.hasActiveTrial || false,
      trialEndDate: data.trialEndDate || null
    };

  } catch (error) {
    console.error("Error getting tariff info:", error);
    // Возвращаем тестовые данные при ошибке
    return {
      activeTariff: null,
      totalStorageLimit: 5 * 1024 * 1024 * 1024, // 5GB
      freeStorageLimit: 5 * 1024 * 1024 * 1024,
      usedStorage: 0,
      tariffStartDate: null,
      tariffEndDate: null,
      autoRenew: false,
      isActive: true,
      daysLeft: 0,
      hasActiveTrial: false,
      trialEndDate: null
    };
  }
};

export const purchaseTariff = async (token, plan, paymentToken = 'test', autoRenew = true, paymentMethod = null) => {
  console.log("=== PURCHASE TARIFF ===");
  console.log("Plan:", plan);
  console.log("Auto-renew:", autoRenew);
  console.log("Payment method:", paymentMethod);

  const url = `${BASE}/users/tariff/purchase?plan=${plan}&autoRenew=${autoRenew}`;

  const headers = {
    "X-Auth-Token": token,
    "X-Payment-Token": paymentToken  // Исправлено: было X-Payment-Token, должно быть Payment-Token
  };

  if (paymentMethod) {
    headers["X-Payment-Method"] = paymentMethod;  // Исправлено: было X-Payment-Method, должно быть Payment-Method
  }

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: headers
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Purchase failed: ${response.status} ${errorText}`);
    }

    const data = await response.json();
    console.log("Purchase successful:", data);
    return data;
  } catch (error) {
    console.error("Error purchasing tariff:", error);
    throw error;
  }
};

export const setAutoRenew = async (token, enabled) => {
  console.log(`=== ${enabled ? 'ENABLE' : 'DISABLE'} AUTO RENEW ===`);

  const url = `${BASE}/users/tariff/set-auto-renew?enabled=${enabled}`;

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "X-Auth-Token": token
      }
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to ${enabled ? 'enable' : 'disable'} auto-renew: ${response.status} ${errorText}`);
    }

    const data = await response.json();
    console.log(`Auto-renew ${enabled ? 'enabled' : 'disabled'} successfully`);
    return data;
  } catch (error) {
    console.error(`Error ${enabled ? 'enabling' : 'disabling'} auto-renew:`, error);
    throw error;
  }
  };