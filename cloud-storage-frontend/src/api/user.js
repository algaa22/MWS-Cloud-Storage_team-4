import { apiRequest } from "./client";


export const UserAPI = {
  info: () => apiRequest("/users/info"),


  update: (newUsername, oldPassword, newPassword) =>
      apiRequest("/users/update", {
        method: "POST",
        headers: {
          ...(newUsername ? { "X-New-Username": newUsername } : {}),
          ...(oldPassword ? { "X-Old-Password": oldPassword } : {}),
          ...(newPassword ? { "X-New-Password": newPassword } : {})
        }
      })
};