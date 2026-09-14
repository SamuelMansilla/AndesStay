import axios from "axios";
import { msalInstance, apiTokenRequest } from "../authConfig";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL || "https://api-gateway-url.aws.com/v2", // URL de AWS API Gateway
});

axiosClient.interceptors.request.use(
  async (config) => {
    const account = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
    if (account) {
      try {
        const response = await msalInstance.acquireTokenSilent({
          ...apiTokenRequest,
          account: account,
        });
        config.headers.Authorization = `Bearer ${response.accessToken}`;

        // Diagnóstico de claims del token en la consola
        try {
          const payloadBase64 = response.accessToken.split(".")[1];
          const decoded = JSON.parse(atob(payloadBase64));
          console.log("🔍 [DEBUG JWT] Token adquirido para API Gateway:", {
            iss: decoded.iss,
            aud: decoded.aud,
            scp: decoded.scp,
            roles: decoded.roles,
          });
        } catch {
          // ignore
        }
      } catch (tokenErr) {
        console.error("❌ [DEBUG JWT] Falló acquireTokenSilent:", tokenErr);
        // Si falló de forma silenciosa, intentar con popup/redirect si es requerido
        throw tokenErr;
      }
    } else {
      console.warn("⚠️ [DEBUG JWT] No se encontró cuenta activa en MSAL.");
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
        data: error.response.data,
        wwwAuthenticate: error.response.headers?.["www-authenticate"],
      });
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
