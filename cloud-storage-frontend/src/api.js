const BASE = "https://localhost:8443/api";

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


async function fetchWithTokenRefresh(url, options = {}, token) {
  let currentToken = token || localStorage.getItem("accessToken");

  if (!currentToken) {
    throw new Error("Требуется авторизация");
  }

  const headers = {
    ...options.headers,
    "X-Auth-Token": currentToken
  };

  try {
    const res = await fetch(url, { ...options, headers });

    if (res.status === 400 || res.status === 401) {
      console.log("Token expired, attempting refresh...");
      const refreshToken = localStorage.getItem("refreshToken");

      if (refreshToken) {
        try {
          const newAccessToken = await refreshTokenRequest(refreshToken);
          console.log("Token refreshed successfully");

          headers["X-Auth-Token"] = newAccessToken;

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

    console.log("=== DEBUG: Checking storage fields ===");
    console.log("data.UsedStorage:", data.UsedStorage, typeof data.UsedStorage);
    console.log("data.StorageLimit:", data.StorageLimit, typeof data.StorageLimit);
    console.log("All data keys:", Object.keys(data));

    const storageInfo = {
      used: 0,
      total: 10 * 1024 * 1024 * 1024, // 10GB по умолчанию
      formattedUsed: '0 Bytes',
      formattedTotal: '10 GB',
      percentage: 0
    };

    if (data.UsedStorage !== undefined) {
      storageInfo.used = Number(data.UsedStorage) || 0;
      console.log("UsedStorage found:", storageInfo.used);
    }

    if (data.StorageLimit !== undefined) {
      storageInfo.total = Number(data.StorageLimit) || 10 * 1024 * 1024 * 1024;
      console.log("StorageLimit found:", storageInfo.total);
    }

    if (data.usedStorage !== undefined && storageInfo.used === 0) {
      storageInfo.used = Number(data.usedStorage) || 0;
      console.log("usedStorage found:", storageInfo.used);
    }

    if (data.storageLimit !== undefined && storageInfo.total === 10 * 1024 * 1024 * 1024) {
      storageInfo.total = Number(data.storageLimit) || 10 * 1024 * 1024 * 1024;
      console.log("storageLimit found:", storageInfo.total);
    }

    if (data.used !== undefined && storageInfo.used === 0) {
      storageInfo.used = Number(data.used) || 0;
    }

    if (data.total !== undefined && storageInfo.total === 10 * 1024 * 1024 * 1024) {
      storageInfo.total = Number(data.total) || 10 * 1024 * 1024 * 1024;
    }

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

export const getFiles = async (token, currentPath = "") => {
  console.log("=== GET FILES WITH GET REQUEST FOR SIZE ===");

  if (!token) throw new Error("Требуется авторизация");

  const params = new URLSearchParams();
  params.append("includeDirectories", "true");
  if (currentPath) params.append("directory", currentPath);

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

    const result = [];

    for (let i = 0; i < files.length; i++) {
      const item = files[i];
      const path = item.path || "";

      let name = "Без имени";

      if (item.name && item.name.trim() !== "") {
        name = item.name;
      }
      else if (path) {
        const pathParts = path.split('/').filter(p => p && p !== '');
        if (pathParts.length > 0) {
          name = pathParts[pathParts.length - 1];
        }
      }
      else {
        name = `Объект ${i + 1}`;
      }

      let type = "file";
      let size = 0;
      let fileInfo = null;

      if (item.type === "folder" || item.type === "directory" || path.endsWith('/')) {
        type = "folder";
      } else {
        type = "file";
        fileInfo = await getFileInfo(path);
        if (fileInfo) {
          size = fileInfo.Size || fileInfo.size || 0;
        }
      }

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
        _raw: item,
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

function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

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

export const updateUserInfo = async (token, updates) => {
  console.log("=== UPDATE USER INFO DEBUG ===");
  console.log("Updates:", updates);

  if (updates.newPassword && !updates.oldPassword) {
    throw new Error("Old password is required when changing password");
  }

  const body = {
    userToken: token
  };

  const headers = {
    "Content-Type": "application/json",
    "X-Auth-Token": token
  };

  if (updates.newUsername) {
    body.newName = updates.newUsername;
    headers["X-New-Username"] = updates.newUsername;
  }

  if (updates.oldPassword && updates.newPassword) {
    body.oldPassword = updates.oldPassword;
    body.newPassword = updates.newPassword;
    headers["X-Old-Password"] = updates.oldPassword;
    headers["X-New-Password"] = updates.newPassword;
  }

  console.log("Headers:", headers);
  console.log("Body:", JSON.stringify(body));

  const url = `${BASE}/users/update`;

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: headers,
      body: JSON.stringify(body)
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

export const uploadFileWithTags = async (token, file, path, onProgress, tags = []) => {
  console.log("=== UPLOAD FILE WITH TAGS ===");
  console.log("Tags:", tags);

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  if (!file) {
    throw new Error("Файл не выбран");
  }

  const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
  const useChunkedUpload = file.size > CHUNK_SIZE;

  console.log(`File size: ${file.size}, Using ${useChunkedUpload ? 'CHUNKED' : 'SIMPLE'} upload`);

  const tagsString = Array.isArray(tags) ? tags.join(',') : tags;

  if (useChunkedUpload) {
    console.log("Using chunked upload with tags");
    return await uploadFileChunkedWithTags(token, file, path, onProgress, tagsString);
  } else {
    console.log("Using simple upload with tags");
    return await uploadFileSimpleWithTags(token, file, path, onProgress, tagsString);
  }
};

const uploadFileSimpleWithTags = async (token, file, path, onProgress, tagsString) => {
  console.log("Using simple upload with tags");

  const url = `${BASE}/files/upload?path=${encodeURIComponent(path)}`;
  console.log("Upload URL:", url);
  console.log("Tags:", tagsString);

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      controller.abort();
    }, 30000);

    const res = await fetchWithTokenRefresh(url, {
      method: "POST",
      headers: {
        "X-File-Tags": tagsString || "user_upload",
        "X-File-Size": file.size,
        "Content-Type": file.type || "application/octet-stream"
      },
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

const uploadFileChunkedWithTags = async (token, file, path, onProgress, tagsString) => {
  console.log("Using chunked upload with tags");

  const url = `${BASE}/files/upload?path=${encodeURIComponent(path)}`;
  const totalSize = file.size;

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.open('POST', url);
    xhr.setRequestHeader('X-Auth-Token', token);
    xhr.setRequestHeader('X-File-Tags', tagsString || "user_upload");
    xhr.setRequestHeader('X-File-Size', totalSize);

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

    xhr.timeout = 300000; // 5 минут

    const formData = new FormData();
    formData.append('file', file);
    formData.append('path', path);

    xhr.send(formData);
  });
};

export const getFileTags = async (token, path) => {
  console.log("=== GET FILE TAGS ===");

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  if (!path) {
    throw new Error("Путь к файлу не указан");
  }

  const url = `${BASE}/files/info?path=${encodeURIComponent(path)}`;

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
          const tagsData = await getFileTags(token, file.fullPath);
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

function formatBytes(bytes) {
  if (!bytes && bytes !== 0) return '0 Bytes';
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export const updateFileMetadata = async (token, path, updates) => {
  console.log("=== UPDATE FILE METADATA (FIXED HEADERS) ===");
  console.log("Path:", path);
  console.log("Updates:", updates);

  if (!token) {
    throw new Error("Требуется авторизация");
  }

  if (!path) {
    throw new Error("Путь к файлу не указан");
  }

  let url = `${BASE}/files?path=${encodeURIComponent(path)}`;

  if (updates.newPath) {
    url += `&newPath=${encodeURIComponent(updates.newPath)}`;
  }

  const recursive = updates.recursive !== undefined ? updates.recursive : false;
  url += `&recursive=${recursive}`;

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