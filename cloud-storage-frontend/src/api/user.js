import api from "./client";

export const userApi = {
  info() {
    return api.get("/users/info");
  },

  update({ newUsername, oldPassword, newPassword }) {
    const headers = {};
    if (newUsername) headers["X-New-Username"] = newUsername;
    if (oldPassword) headers["X-Old-Password"] = oldPassword;
    if (newPassword) headers["X-New-Password"] = newPassword;

    return api.post("/users/update", null, { headers });
  }
};
