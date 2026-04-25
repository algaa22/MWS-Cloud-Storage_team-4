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
    ...options.headers
  };

  if (currentToken) {
    headers["X-Auth-Token"] = currentToken;
  }

  console.log("fetchWithTokenRefresh - URL:", url);
  console.log("fetchWithTokenRefresh - Has token:", !!currentToken);

  try {
    const res = await fetch(url, { ...options, headers });
    console.log("Response status:", res.status);

    if (res.status === 401) {
      console.log("Got 401, trying to refresh...");
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        try {
          const newAccessToken = await refreshTokenRequest(refreshToken);
          localStorage.setItem("accessToken", newAccessToken);
          headers["X-Auth-Token"] = newAccessToken;
          console.log("Retrying request with new token");
          return await fetch(url, { ...options, headers });
        } catch (e) {
          console.error("Refresh failed:", e);
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          window.location.href = "/";
          throw e;
        }
      } else {
        console.log("No refresh token, redirecting to login");
        localStorage.removeItem("accessToken");
        window.location.href = "/";
        throw new Error("Unauthorized");
      }
    }
    return res;
  } catch (err) {
    console.error("Fetch error:", err);
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
      storageLimit: totalStorageLimit,
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
      storageLimit: 5 * 1024 * 1024 * 1024,
      activeTariff: null,
      isActive: true,
      storageInfo: defaultStorage
    };
  }
};

export const getFiles = async (token, currentPath = "", page = 0, size = 20) => {
  console.log("=== GET FILES ===");

  if (!token) throw new Error("Требуется авторизация");

  const params = new URLSearchParams();
  params.append("includeDirectories", "true");
  params.append("page", page.toString());
  params.append("size", size.toString());

  if (currentPath && currentPath !== "") {
    params.append("parentId", currentPath);
  }

  const listUrl = `${BASE}/files/list?${params.toString()}`;
  console.log("Request URL:", listUrl);

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
    console.log("Raw server response:", data);

    let filesArray = [];

    if (data.page && Array.isArray(data.page.content)) {
      filesArray = data.page.content;
      console.log("Extracted from page.content");
    } else if (Array.isArray(data.content)) {
      filesArray = data.content;
      console.log("Extracted from content");
    } else if (Array.isArray(data.files)) {
      filesArray = data.files;
      console.log("Extracted from files");
    } else if (Array.isArray(data)) {
      filesArray = data;
      console.log("Data is already an array");
    } else {
      console.warn("Unexpected data structure:", data);
      filesArray = [];
    }

    console.log(`Found ${filesArray.length} items`);
    console.log("Raw items:", filesArray);

    const result = filesArray.map(item => ({
      name: item.name || "Без имени",
      type: item.isDirectory ? "folder" : "file",
      size: item.size || 0,
      id: item.id,
      parentId: item.parentId,
      tags: item.tags,
      _raw: item
    }));

    console.log("Processed result:", result);
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

export const getTrashFiles = async (token, page = 0, size = 1000) => {
  console.log("=== GET TRASH FILES ===");

  if (!token) throw new Error("Требуется авторизация");

  const params = new URLSearchParams();
  params.append("page", page.toString());
  params.append("size", size.toString());

  const url = `${BASE}/files/trash?${params.toString()}`;
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

    console.log("=== FULL SERVER RESPONSE ===");
    console.log(JSON.stringify(data, null, 2));

    let filesArray = [];
    if (data.page && data.page.content && Array.isArray(data.page.content)) {
      filesArray = data.page.content;
      console.log("Extracted from page.content");
    } else if (data.content && Array.isArray(data.content)) {
      filesArray = data.content;
      console.log("Extracted from content");
    }

    console.log(`Found ${filesArray.length} files in trash`);

    if (filesArray.length > 0) {
      console.log("=== FIRST FILE DETAILS ===");
      console.log("Raw file object:", filesArray[0]);
      console.log("All keys:", Object.keys(filesArray[0]));
      console.log("deletedAt:", filesArray[0].deletedAt);
      console.log("deleted_at:", filesArray[0].deleted_at);
      console.log("deletedAt type:", typeof filesArray[0].deletedAt);
    }

    return filesArray.map(item => {
      let deletedAt = null;

      if (item.deletedAt) {
        deletedAt = item.deletedAt;
        console.log(`Found deletedAt for ${item.name}:`, deletedAt);
      } else if (item.deleted_at) {
        deletedAt = item.deleted_at;
        console.log(`Found deleted_at for ${item.name}:`, deletedAt);
      } else {
        console.warn(`No deletedAt field for ${item.name}`);
      }

      return {
        name: item.name || "Без имени",
        id: item.id,
        type: item.isDirectory ? "folder" : "file",
        size: item.size || 0,
        deletedAt: item.updatedAt
      };
    });

  } catch (error) {
    console.error("Error getting trash files:", error);
    throw error;
  }
};


export const softDeleteFile = async (token, id) => {
  console.log("=== SOFT DELETE FILE (to trash) ===");
  console.log("File ID:", id);
  console.log("Token provided:", !!token);

  const userStr = localStorage.getItem('user');
  let userId = null;

  console.log("User from localStorage:", userStr);

  if (userStr) {
    try {
      const user = JSON.parse(userStr);
      userId = user.id;
      console.log("User ID for delete:", userId);
    } catch (e) {
      console.error("Failed to parse user:", e);
    }
  }

  if (!userId) {
    console.error("No user ID found, trying to get from getUserInfo...");
    try {
      const userInfo = await getUserInfo(token);
      userId = userInfo.id;
      console.log("Got user ID from getUserInfo:", userId);
      localStorage.setItem('user', JSON.stringify({
        id: userInfo.id,
        email: userInfo.email,
        username: userInfo.username
      }));
    } catch (error) {
      console.error("Failed to get user info:", error);
      throw new Error("User not found");
    }
  }

  const url = `${BASE}/files?id=${encodeURIComponent(id)}&permanent=false&userId=${encodeURIComponent(userId)}`;
  console.log("DELETE URL:", url);
  console.log("Full request details:", {
    method: "DELETE",
    url: url,
    headers: {
      "Accept": "application/json",
      "X-Auth-Token": token ? `${token.substring(0, 20)}...` : "missing"
    }
  });

  try {
    const res = await fetchWithTokenRefresh(url, {
      method: "DELETE",
      headers: {
        "Accept": "application/json"
      }
    }, token);

    console.log("Response status:", res.status);
    console.log("Response status text:", res.statusText);
    console.log("Response headers:", Object.fromEntries(res.headers.entries()));

    const responseText = await res.text();
    console.log("Response body:", responseText);

    if (!res.ok) {
      throw new Error(`Soft delete failed: ${res.status} ${responseText}`);
    }

    let responseData;
    try {
      responseData = responseText ? JSON.parse(responseText) : { success: true };
      console.log("Parsed response:", responseData);
    } catch (e) {
      console.log("Response is not JSON, treating as success");
      responseData = { success: true };
    }

    console.log("Soft delete completed successfully");
    return true;

  } catch (error) {
    console.error("Soft delete error:", error);
    throw error;
  }
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

    if (res.status === 404) {
      console.log("File already deleted (404), treating as success");
      return { success: true, id, alreadyDeleted: true };
    }

    if (!res.ok) {
      const txt = await res.text().catch(() => "(no body)");
      console.error("Permanent delete failed:", res.status, txt);
      throw new Error(`Permanent delete failed: ${res.status} ${txt}`);
    }

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

    console.log(`Found ${trashFiles.length} files to delete permanently`);

    const results = await Promise.allSettled(
      trashFiles.map(file => permanentDeleteFile(token, file.id))
    );

    const successful = results.filter(r => r.status === 'fulfilled').length;
    const failed = results.filter(r => r.status === 'rejected').length;

    console.log(`Empty trash result: ${successful} successful, ${failed} failed`);

    return {
      total: trashFiles.length,
      success: successful,
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
  } = options;

  const params = new URLSearchParams();
  params.append('fileId', fileId);
  params.append('shareType', shareType);
  if (expiresAt) params.append('expiresAt', expiresAt);
  if (maxDownloads) params.append('maxDownloads', maxDownloads);
  if (password) params.append('password', password);

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
  console.log("=== DOWNLOAD SHARED FILE ===");
  console.log("Share token:", shareToken);
  console.log("Password provided:", password ? "yes" : "no");

  const url = `${API_BASE}/shares/download?shareToken=${shareToken}`;
  console.log("URL:", url);

  const headers = {};

  if (password) {
    headers['X-Share-Password'] = password;
    console.log("Adding password to header: X-Share-Password");
  }

  const response = await fetch(url, {
    method: "GET",
    headers: headers
  });

  if (!response.ok) {
    console.error("Response status:", response.status);
    const errorText = await response.text();
    console.error("Error response:", errorText);
    const error = new Error('Download failed');
    error.status = response.status;
    throw error;
  }

  const blob = await response.blob();
  const contentDisposition = response.headers.get('Content-Disposition');
  let filename = 'file';
  if (contentDisposition && contentDisposition.includes('filename=')) {
    filename = decodeURIComponent(contentDisposition.split('filename=')[1].replace(/["']/g, ''));
  }

  console.log("Download successful, filename:", filename);
  return { blob, filename };
};

export const validateSharePassword = async (shareToken, password) => {
  console.log("=== VALIDATE SHARE PASSWORD ===");
  console.log("Share token:", shareToken);

  const url = `${API_BASE}/shares/validate`;
  console.log("Request URL:", url);

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      shareToken: shareToken,
      password: password
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error("Validation error response:", errorText);
    const error = new Error('Password validation failed');
    error.status = response.status;
    throw error;
  }

  const result = await response.json();
  console.log("Validation success:", result);
  return result;
};

export const deactivateShare = async (token, shareId) => {
  console.log("=== DEACTIVATE SHARE ===");
  console.log("Share ID:", shareId);

  const url = `${BASE}/shares?shareId=${shareId}`;
  console.log("Request URL:", url);

  try {
    const response = await fetch(url, {
      method: "DELETE",
      headers: {
        "X-Auth-Token": token,
        "Accept": "application/json"
      }
    });

    console.log("Response status:", response.status);
    const responseText = await response.text();
    console.log("Response body:", responseText);

    if (!response.ok) {
      throw new Error(`Failed: ${response.status}`);
    }

    return { success: true };

  } catch (error) {
    console.error("Error:", error);
    throw error;
  }
};

export const getUserShares = async (token) => {
  console.log("=== GET USER SHARES ===");

  const url = `${BASE}/shares/user`;
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
  console.log("Token provided:", token ? "yes" : "no");
  console.log("File ID:", fileId);

  const url = `${BASE}/shares/file?fileId=${fileId}`;
  console.log("Request URL:", url);

  const response = await fetchWithTokenRefresh(url, {
    method: "GET",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  if (!response.ok) {
    const errorText = await response.text();
    console.error("Error response:", errorText);
    throw new Error(`Failed to get file shares: ${response.status} ${errorText}`);
  }

  const data = await response.json();
  console.log("File shares response:", data);
  return data;
};

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

  if (parentId && parentId !== "") {
    url += `&parentId=${encodeURIComponent(parentId)}`;
  }

  if (tagsString && tagsString.trim() !== '') {
    url += `&tags=${encodeURIComponent(tagsString)}`;
  }

  console.log("Upload URL:", url);
  console.log("File size:", file.size);
  console.log("File type:", file.type);

  try {
    const res = await fetchWithTokenRefresh(url, {
      method: "POST",
      headers: {
        'Content-Type': 'application/octet-stream'
      },
      body: file
    }, token);

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

const uploadFileChunkedWithTags = async (token, file, parentId, onProgress, tagsString, existingSessionId = null) => {
  console.log("=== STARTING CHUNKED UPLOAD ===");
  console.log("File:", file.name, "size:", file.size);
  console.log("Existing session ID:", existingSessionId);

  const CHUNK_SIZE = 5 * 1024 * 1024;
  const totalParts = Math.ceil(file.size / CHUNK_SIZE);
  let sessionId = existingSessionId;

  if (!sessionId) {
    let url = `${BASE}/files/upload/chunked/start?name=${encodeURIComponent(file.name)}`;
    if (parentId && parentId !== "") {
      url += `&parentId=${encodeURIComponent(parentId)}`;
    }
    if (tagsString && tagsString.trim() !== '') {
      url += `&tags=${encodeURIComponent(tagsString)}`;
    }

    console.log("Start upload URL:", url);

    const startResponse = await fetchWithTokenRefresh(url, {
      method: "POST",
      headers: {
        "X-Total-Parts": totalParts.toString(),
        "X-File-Size": file.size.toString(),
        "Content-Type": "application/json"
      }
    }, token);

    if (!startResponse.ok) {
      const error = await startResponse.text();
      throw new Error(`Failed to start upload: ${error}`);
    }

    const startData = await startResponse.json();
    sessionId = startData.sessionId;

    saveUploadSession(file.name, parentId, sessionId, totalParts, file.size);
  } else {
    console.log("Using existing session:", sessionId);
  }

  let startPart = 1;
  try {
    const statusData = await getUploadStatus(token, sessionId);
    console.log("Upload status:", statusData);

    if (statusData && statusData.currentParts && statusData.currentParts > 0) {
      startPart = statusData.currentParts + 1;
      console.log(`✅ Resuming from part ${startPart} out of ${totalParts}`);
      console.log(`   Already uploaded: ${statusData.currentParts} parts`);

      if (onProgress) {
        const progress = Math.round(((startPart - 1) / totalParts) * 100);
        onProgress(progress);
      }
    }
  } catch (err) {
    console.log("Could not get upload status, starting from scratch:", err);
  }

  console.log(`Uploading parts from ${startPart} to ${totalParts}...`);

  for (let partNumber = startPart; partNumber <= totalParts; partNumber++) {
    const start = (partNumber - 1) * CHUNK_SIZE;
    const end = Math.min(start + CHUNK_SIZE, file.size);
    const chunk = file.slice(start, end);

    console.log(`📤 Uploading part ${partNumber}/${totalParts}, size: ${chunk.size}`);

    const partUrl = `${BASE}/files/upload/chunked/part?sessionId=${sessionId}&part=${partNumber}`;

    const partResponse = await fetchWithTokenRefresh(partUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/octet-stream"
      },
      body: chunk
    }, token);

    if (!partResponse.ok) {
      const error = await partResponse.text();
      throw new Error(`Failed to upload part ${partNumber}: ${error}`);
    }

    const progress = Math.round((partNumber / totalParts) * 100);
    if (onProgress) onProgress(progress);
  }

  console.log("Completing upload...");
  const completeResponse = await fetchWithTokenRefresh(`${BASE}/files/upload/chunked/complete?sessionId=${sessionId}`, {
    method: "POST"
  }, token);

  if (!completeResponse.ok) {
    const error = await completeResponse.text();
    throw new Error(`Failed to complete upload: ${error}`);
  }

  removeUploadSession(file.name, parentId);

  const result = await completeResponse.json();
  console.log("✅ Upload completed, fileId:", result.fileId);

  if (onProgress) onProgress(100);
  return result;
};

export const getUploadStatus = async (token, sessionId) => {
  console.log("=== GET UPLOAD STATUS ===");
  console.log("Session ID:", sessionId);

  const url = `${BASE}/files/upload/chunked/status?sessionId=${sessionId}`;

  const response = await fetchWithTokenRefresh(url, {
    method: "GET",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Failed to get upload status: ${error}`);
  }

  const data = await response.json();
  console.log("Raw status data:", data);

  return {
    sessionId: data.sessionId,
    status: data.status,
    currentSize: data.currentSize,
    currentParts: data.currentParts,
    totalParts: data.totalParts || 0,
    fileSize: data.fileSize || 0,
    missingPartsBitmask: data.missingPartsBitmask
  };
};

 export const abortChunkedUpload = async (token, sessionId) => {
   console.log("=== ABORT UPLOAD ===");

   const url = `${BASE}/files/upload/chunked/abort?sessionId=${sessionId}`;

   const response = await fetchWithTokenRefresh(url, {
     method: "POST"
   }, token);

   if (!response.ok) {
     const error = await response.text();
     throw new Error(`Failed to abort upload: ${error}`);
   }

   return { success: true };
 };

 export const saveUploadSession = (fileName, parentId, sessionId, totalParts, fileSize) => {
   const key = `${parentId || 'root'}_${fileName}`;
   const sessions = JSON.parse(localStorage.getItem('uploadSessions') || '{}');
   sessions[key] = {
     sessionId: sessionId,
     fileName: fileName,
     parentId: parentId,
     startedAt: Date.now(),
     totalParts: totalParts,
     fileSize: fileSize
   };
   localStorage.setItem('uploadSessions', JSON.stringify(sessions));
 };

 export const getSavedUploadSession = (fileName, parentId) => {
   const parentKey = parentId && parentId !== "" ? parentId : "root";
   const key = `${parentKey}_${fileName}`;
   console.log("🔍 Looking for session with key:", key);

   const sessions = JSON.parse(localStorage.getItem('uploadSessions') || '{}');
   console.log("📦 All sessions:", sessions);
   console.log("✅ Found:", sessions[key]);

   return sessions[key] || null;
 };

 export const removeUploadSession = (fileName, parentId) => {
   const parentKey = parentId && parentId !== "" ? parentId : "root";
   const key = `${parentKey}_${fileName}`;
   const sessions = JSON.parse(localStorage.getItem('uploadSessions') || '{}');
   delete sessions[key];
   localStorage.setItem('uploadSessions', JSON.stringify(sessions));
   console.log("🗑️ Removed session for key:", key);
 };

export const deleteSharePermanently = async (token, shareId) => {
  console.log("=== DELETE SHARE PERMANENTLY ===");
  console.log("Share ID:", shareId);

  const url = `${BASE}/shares/permanent?shareId=${shareId}`;
  console.log("Request URL:", url);

  try {
    const response = await fetch(url, {
      method: "DELETE",
      headers: {
        "X-Auth-Token": token,
        "Accept": "application/json"
      }
    });

    console.log("Response status:", response.status);
    const responseText = await response.text();
    console.log("Response body:", responseText);

    if (!response.ok) {
      throw new Error(`Failed to delete share: ${response.status}`);
    }

    return { success: true };

  } catch (error) {
    console.error("Error:", error);
    throw error;
  }
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
      return {
        activeTariff: null,
        totalStorageLimit: 5 * 1024 * 1024 * 1024,
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
    return {
      activeTariff: null,
      totalStorageLimit: 5 * 1024 * 1024 * 1024,
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
    "X-Payment-Token": paymentToken
  };

  if (paymentMethod) {
    headers["X-Payment-Method"] = paymentMethod;
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

export const getPaymentHistory = async (token) => {
  console.log("=== GET PAYMENT HISTORY ===");

  const url = `${BASE}/payments/history`;
  console.log("Request URL:", url);

  const response = await fetchWithTokenRefresh(url, {
    method: "GET",
    headers: {
      "Accept": "application/json"
    }
  }, token);

  console.log("Response status:", response.status);
  console.log("Response headers:", response.headers.get('content-type'));

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to get payment history: ${response.status} ${errorText}`);
  }

  const text = await response.text();
  console.log("Raw response text:", text);

  if (!text || text.trim() === '') {
    console.log("Empty response, returning empty history");
    return { transactions: [] };
  }

  try {
    const data = JSON.parse(text);
    console.log("Payment history response:", data);
    return data;
  } catch (e) {
    console.error("Failed to parse JSON:", e);
    return { transactions: [] };
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

export const getFilePreview = async (token, fileId) => {
  try {
    const url = `${BASE}/files/preview?fileId=${fileId}`;
    console.log("=== GET FILE PREVIEW ===");
    console.log("URL:", url);
    console.log("Token present:", !!token);

    const response = await fetchWithTokenRefresh(
      url,
      {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        }
      },
      token
    );

    console.log("Response status:", response.status);
    console.log("Response headers:", response.headers);

    const text = await response.text();
    console.log("Response body:", text);

    if (!response.ok) {
      throw new Error(`Failed to get preview URL: ${response.status} - ${text}`);
    }

    return JSON.parse(text);
  } catch (error) {
    console.error('Error getting file preview:', error);
    throw error;
  }
};