import { ref, computed } from "vue";
import { defineStore, getActivePinia } from "pinia";

export const AUTH_TOKEN_KEY = "access_token";
export const AUTH_USER_KEY = "auth_user";
export const LAST_RESPONSE_KEY = "last_auth_response";

const S = {
  get(key) {
    return sessionStorage.getItem(key);
  },
  set(key, val) {
    sessionStorage.setItem(key, val);
  },
  remove(key) {
    sessionStorage.removeItem(key);
  },
};

function safeJsonParse(raw) {
  try {
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function decodeJwtPayload(jwt) {
  try {
    if (!jwt) return null;
    const parts = jwt.split(".");
    if (parts.length < 2) return null;
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(
      base64.length + ((4 - (base64.length % 4)) % 4),
      "="
    );
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

export const useAuthStore = defineStore("auth", () => {
  const token = ref(S.get(AUTH_TOKEN_KEY) || "");
  const user = ref(safeJsonParse(S.get(AUTH_USER_KEY)));
  const lastAuthResponse = ref(safeJsonParse(S.get(LAST_RESPONSE_KEY)));

  const role = computed(() =>
    ((user.value?.role || user.value?.data?.role || "") + "").toUpperCase()
  );
  const isAuthed = computed(() => !!token.value);
  const isCustomer = computed(
    () => isAuthed.value && role.value === "CUSTOMER"
  );
  const displayName = computed(
    () =>
      user.value?.username ||
      user.value?.fullName ||
      user.value?.name ||
      user.value?.email ||
      "User"
  );

  function setSession({ token: t, user: u }) {
    if (t) {
      token.value = t;
      S.set(AUTH_TOKEN_KEY, t);
    } else {
      token.value = "";
      S.remove(AUTH_TOKEN_KEY);
    }

    if (u) {
      user.value = u;
      S.set(AUTH_USER_KEY, JSON.stringify(u));
    } else {
      user.value = null;
      S.remove(AUTH_USER_KEY);
    }
  }

  function clearSession() {
    token.value = "";
    user.value = null;
    S.remove(AUTH_TOKEN_KEY);
    S.remove(AUTH_USER_KEY);
  }

  function setLastAuthResponse(resData) {
    try {
      lastAuthResponse.value = resData ?? null;
      S.set(LAST_RESPONSE_KEY, JSON.stringify(resData ?? null));
    } catch {}
  }

  function clearLastAuthResponse() {
    lastAuthResponse.value = null;
    S.remove(LAST_RESPONSE_KEY);
  }

  return {
    token,
    user,
    lastAuthResponse,
    role,
    isAuthed,
    isCustomer,
    displayName,
    setSession,
    clearSession,
    setLastAuthResponse,
    clearLastAuthResponse,
  };
});

// ===== Backward-compatible helpers (used by api/http + legacy pages) =====
function getStoreOrFallback() {
  try {
    if (getActivePinia()) return useAuthStore();
  } catch {}
  return null;
}

export function getToken() {
  const st = getStoreOrFallback();
  if (st) return st.token || "";
  return S.get(AUTH_TOKEN_KEY) || "";
}

export function getUser() {
  const st = getStoreOrFallback();
  if (st) return st.user || null;
  return safeJsonParse(S.get(AUTH_USER_KEY));
}

export function getRole() {
  const st = getStoreOrFallback();
  if (st) return (st.role || "").toString();
  const u = safeJsonParse(S.get(AUTH_USER_KEY));
  return (u?.role || u?.data?.role || "").toString();
}

export function setSession(payload) {
  const st = getStoreOrFallback();
  if (st) return st.setSession(payload);
  const { token, user } = payload || {};
  if (token) S.set(AUTH_TOKEN_KEY, token);
  else S.remove(AUTH_TOKEN_KEY);
  if (user) S.set(AUTH_USER_KEY, JSON.stringify(user));
  else S.remove(AUTH_USER_KEY);
}

export function clearSession() {
  const st = getStoreOrFallback();
  if (st) return st.clearSession();
  S.remove(AUTH_TOKEN_KEY);
  S.remove(AUTH_USER_KEY);
}

export function setLastAuthResponse(resData) {
  const st = getStoreOrFallback();
  if (st) return st.setLastAuthResponse(resData);
  try {
    S.set(LAST_RESPONSE_KEY, JSON.stringify(resData ?? null));
  } catch {}
}

export function getLastAuthResponse() {
  const st = getStoreOrFallback();
  if (st) return st.lastAuthResponse ?? null;
  return safeJsonParse(S.get(LAST_RESPONSE_KEY));
}

export function clearLastAuthResponse() {
  const st = getStoreOrFallback();
  if (st) return st.clearLastAuthResponse();
  S.remove(LAST_RESPONSE_KEY);
}
