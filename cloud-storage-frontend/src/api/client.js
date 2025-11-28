export const API_BASE = "http://localhost:8082";


export async function apiRequest(path, method = 'GET', headers = {}, body = null) {
  const response = await fetch(API_BASE + path, {
    method,
    headers,
    body,
  });


  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || 'Request failed');
  }


  return response;
}