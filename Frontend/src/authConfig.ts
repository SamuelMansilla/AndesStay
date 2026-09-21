import { PublicClientApplication, type Configuration, LogLevel } from "@azure/msal-browser";

export const msalConfig: Configuration = {
  auth: {
    clientId: import.meta.env.VITE_AZURE_CLIENT_ID || "TU_AZURE_CLIENT_ID",
    authority: `https://login.microsoftonline.com/${import.meta.env.VITE_AZURE_TENANT_ID || "TU_TENANT_ID"}`,
    redirectUri: import.meta.env.VITE_AZURE_REDIRECT_URI || window.location.origin,
  },
  cache: {
    cacheLocation: "localStorage",
  },
  system: {
    loggerOptions: {
      loggerCallback: (level, message, containsPii) => {
        if (containsPii) return;
        if (level === LogLevel.Error) console.error(message);
      },
      logLevel: LogLevel.Error,
    },
  },
};

// Scopes específicos para invocar el API Gateway de AWS y el BFF
export const apiTokenRequest = {
  scopes: [
    import.meta.env.VITE_AZURE_API_SCOPE ||
      `api://${import.meta.env.VITE_AZURE_API_CLIENT_ID || "TU_API_CLIENT_ID"}/access_as_user`,
  ],
};

// Scopes combinados para inicio de sesión en Azure AD (incluye scopes de usuario y del API)
export const loginRequest = {
  scopes: ["User.Read", ...apiTokenRequest.scopes],
};

export const msalInstance = new PublicClientApplication(msalConfig);