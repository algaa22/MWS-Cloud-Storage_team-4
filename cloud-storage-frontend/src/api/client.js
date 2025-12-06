import axios from "axios";

const api = axios.create({
  baseURL: "https://localhost:8443/api",
});

// автоматически добавляем X-Auth-Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("authToken");
  if (token) {
    config.headers["X-Auth-Token"] = token;
  }
  return config;
});

export default api;
