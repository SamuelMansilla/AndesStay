import axios from "axios";
import { msalInstance, loginRequest } from "../authConfig";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL || "https://api-gateway-url.aws.com/v2", // URL de AWS API Gateway
});

axiosClient.interceptors.request.use(
  async (config) => {
    const account = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
    if (account) {
      const response = await msalInstance.acquireTokenSilent({
        ...loginRequest,
        account: account,
      });
      config.headers.Authorization = `Bearer ${response.accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default axiosClient;
