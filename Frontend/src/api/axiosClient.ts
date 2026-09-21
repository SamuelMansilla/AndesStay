import axios from "axios";
import { InteractionRequiredAuthError } from "@azure/msal-browser";
import { msalInstance, apiTokenRequest } from "../authConfig";

const baseURL =
  import.meta.env.VITE_API_GATEWAY_URL ||
  import.meta.env.VITE_API_URL ||
  "https://pn62ivjtk2.execute-api.us-east-1.amazonaws.com";

const axiosClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

axiosClient.interceptors.request.use(
  async (config) => {
    // Obtener la cuenta activa o la primera cuenta disponible en sesión
    const account =
      msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];

    if (account) {
      try {
        let tokenResponse;
        try {
          // Intento de adquisición silenciosa
          tokenResponse = await msalInstance.acquireTokenSilent({
            ...apiTokenRequest,
            account,
          });
        } catch (error) {
          // Si la sesión expiró o requiere interacción del usuario
          if (error instanceof InteractionRequiredAuthError) {
            tokenResponse = await msalInstance.acquireTokenPopup({
              ...apiTokenRequest,
              account,
            });
          } else {
            throw error;
          }
        }

        if (tokenResponse?.accessToken) {
          config.headers.Authorization = `Bearer ${tokenResponse.accessToken}`;

          // Diagnóstico de claims del token (decodificación segura de Base64URL)
          try {
            const base64Url = tokenResponse.accessToken.split(".")[1];
            const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
            const jsonPayload = decodeURIComponent(
              atob(base64)
                .split("")
                .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
                .join("")
            );
            const decoded = JSON.parse(jsonPayload);

            console.log("🔍 [DEBUG JWT] Token adjunto para API Gateway:", {
              iss: decoded.iss,
              aud: decoded.aud,
              roles: decoded.roles || decoded.roles_access || "sin roles",
              scp: decoded.scp || "sin scopes",
            });
          } catch {
            // Ignorar errores del log de depuración
          }
        }
      } catch (tokenErr) {
        console.error("❌ [DEBUG JWT] Error al adquirir token con MSAL:", tokenErr);
        return Promise.reject(tokenErr);
      }
    } else {
      console.warn("⚠️ [DEBUG JWT] Petición enviada sin autenticación (no hay sesión activa).");
    }

    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      console.error("❌ [DEBUG API Gateway Response]:", {
        status: error.response.status,
        url: error.config?.url,
        data: error.response.data,
        headers: error.response.headers,
      });
    } else if (error.request) {
      console.error("❌ [DEBUG Conexión]: No hubo respuesta de API Gateway (posible error CORS o red).", error.message);
    }
    return Promise.reject(error);
  }
);

export default axiosClient;