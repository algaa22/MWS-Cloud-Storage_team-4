import api from "./client";

export const directoriesApi = {
  create(path) {
    return api.put("/directories", null, {
      params: { path },
    });
  },

  rename(from, to) {
    return api.post("/directories", null, {
      params: { from, to },
    });
  },

  delete(path) {
    return api.delete("/directories", {
      params: { path },
    });
  },
};
