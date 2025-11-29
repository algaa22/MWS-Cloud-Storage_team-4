import { apiRequest } from "./client";


export const DirectorysAPI = {
  create: (folder) =>
      apiRequest(`/folders?folder=${encodeURIComponent(folder)}`, {
        method: "POST"
      }),


  delete: (folder) =>
      apiRequest(`/folders?folder=${encodeURIComponent(folder)}`, {
        method: "DELETE"
      })
};