const BASE = "http://localhost:8081/api";

export const getFiles = async (token, directory = '') => {
  const response = await fetch(`${BASE}/files?directory=${encodeURIComponent(directory)}`, {
    headers: {
      'X-Auth-Token': token
    }
  });

  if (!response.ok) throw new Error('Failed to fetch files');
  return response.json();
};

export const uploadFile = async (token, file, path, onProgress) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${BASE}/files/upload?path=${encodeURIComponent(path)}`, {
    method: 'POST',
    headers: {
      'X-Auth-Token': token,
      'X-File-Tags': 'user_upload'
    },
    body: formData
  });

  if (!response.ok) throw new Error('Upload failed');
  return response.json();
};

export const downloadFile = async (token, path, filename) => {
  const response = await fetch(`${BASE}/files?path=${encodeURIComponent(path)}`, {
    headers: {
      'X-Auth-Token': token
    }
  });

  if (!response.ok) throw new Error('Download failed');

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
};

export const deleteFile = async (token, path) => {
  const response = await fetch(`${BASE}/files?path=${encodeURIComponent(path)}`, {
    method: 'DELETE',
    headers: {
      'X-Auth-Token': token
    }
  });

  if (!response.ok) throw new Error('Delete failed');
  return true;
};

export const renameFile = async (token, oldPath, newPath) => {
  const response = await fetch(`${BASE}/files?path=${encodeURIComponent(oldPath)}`, {
    method: 'PUT',
    headers: {
      'X-Auth-Token': token,
      'X-File-New-Path': newPath
    }
  });

  if (!response.ok) throw new Error('Rename failed');
  return true;
};

export const getFileInfo = async (token, path) => {
  const response = await fetch(`${BASE}/files/info?path=${encodeURIComponent(path)}`, {
    headers: {
      'X-Auth-Token': token
    }
  });

  if (!response.ok) throw new Error('Failed to get file info');
  return response.json();
};