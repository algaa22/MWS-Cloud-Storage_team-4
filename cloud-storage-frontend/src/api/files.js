import { apiRequest } from "./client";


export const FilesAPI = {
  list: () => apiRequest("/files"),


  info: (path) => apiRequest(`/files/info?path=${encodeURIComponent(path)}`),


  download: (path) => apiRequest(`/files?path=${encodeURIComponent(path)}`),


  delete: (path) =>
      apiRequest(`/files?path=${encodeURIComponent(path)}`, {
        method: "DELETE"
      }),


  rename: (path, newPath) =>
      apiRequest(`/files?path=${encodeURIComponent(path)}`, {
        method: "PUT",
        headers: {
          "X-File-New-Path": newPath
        }
      }),


  upload: (path, file, onProgress) => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", `/api/files/upload?path=${encodeURIComponent(path)}`);
      xhr.setRequestHeader("X-Auth-Token", localStorage.getItem("token"));


      xhr.upload.onprogress = (e) => {
        if (onProgress) onProgress(e.loaded / e.total);
      };


      xhr.onload = () => resolve(xhr.responseText);
      xhr.onerror = reject;


      const formData = new FormData();
      formData.append("file", file);
      xhr.send(formData);
    });
  }
};

export const getFileList = async () => {
  const data = await FilesAPI.list();
  return data;
};
