const API_BASE = "http://localhost:8081";

export async function login(email, password) {
  const res = await fetch(`${API_BASE}/api/users/auth/login`, {
    method: "POST",
    headers: {
      "X-Auth-Email": email,
      "X-Auth-Password": password
    }
  });

  if (!res.ok) return null;

  const token = res.headers.get("X-Auth-Token");
  return token;
}

export async function registerUser(email, password, username) {
  const res = await fetch(`${API_BASE}/api/users/auth/register`, {
    method: "POST",
    headers: {
      "X-Auth-Email": email,
      "X-Auth-Password": password,
      "X-Auth-Username": username
    }
  });

  if (!res.ok) return null;

  const token = res.headers.get("X-Auth-Token");
  return token;
}

export async function getUserInfo(token) {
  const res = await fetch(`${API_BASE}/api/users/info`, {
    method: "GET",
    headers: { "X-Auth-Token": token }
  });

  if (!res.ok) return null;

  return res.json();
}

export async function listDirectorys(token, directory = "") {
  const res = await fetch(
      `${API_BASE}/api/files?directory=${encodeURIComponent(directory)}`,
      {
        method: "GET",
        headers: { "X-Auth-Token": token }
      }
  );

  if (!res.ok) return [];

  const data = await res.json();

  // backend отдаёт список объектов — фильтруем только папки
  return data.filter((item) => item.isDirectory);
}
