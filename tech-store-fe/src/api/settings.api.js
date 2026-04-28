import http from "./http";

export const settingsApi = {
  getDefaultCurrency() {
    return http.get("/api/settings/currency/default");
  },
  setDefaultCurrency(currencyCode) {
    return http.put("/api/settings/currency/default", { currencyCode });
  },
};
