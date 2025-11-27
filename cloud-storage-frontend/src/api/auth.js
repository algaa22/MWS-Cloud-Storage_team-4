import { apiRequest } from './client';


export async function login(email, password) {
  const res = await apiRequest('/users/login', 'GET', {
    'X-Auth-Email': email,
    'X-Auth-Password': password,
  });
  return res.json();
}


export async function register(email, username, password) {
  const res = await apiRequest('/users/register', 'GET', {
    'X-Auth-Email': email,
    'X-Auth-Password': password,
    'X-Auth-Username': username,
  });
  return res.json();
}