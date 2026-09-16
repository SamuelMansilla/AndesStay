import React from "react";
import ReactDOM from "react-dom/client";
import { EventType } from "@azure/msal-browser";
import type { EventMessage, AuthenticationResult } from "@azure/msal-browser";
import { MsalProvider } from "@azure/msal-react";
import { msalInstance } from "./authConfig";
import App from "./App";

async function main() {
    // Inicialización obligatoria en MSAL v3+
    await msalInstance.initialize();

    // 1. Registrar callback de eventos ANTES de resolver redirección
    msalInstance.addEventCallback((event: EventMessage) => {
        if (
            (event.eventType === EventType.LOGIN_SUCCESS ||
             event.eventType === EventType.ACQUIRE_TOKEN_SUCCESS) &&
            event.payload
        ) {
            const payload = event.payload as AuthenticationResult;
            if (payload.account) {
                msalInstance.setActiveAccount(payload.account);
            }
        }
    });

    // 2. Procesar respuesta de redirección si viene de Microsoft
    try {
        const redirectResult = await msalInstance.handleRedirectPromise();
        if (redirectResult?.account) {
            msalInstance.setActiveAccount(redirectResult.account);
        }
    } catch (error) {
        console.error("Error al procesar respuesta de Azure AD en handleRedirectPromise:", error);
    }

    // 3. Activar la cuenta si existe una sesión previa en caché
    if (
        !msalInstance.getActiveAccount() &&
        msalInstance.getAllAccounts().length > 0
    ) {
        msalInstance.setActiveAccount(msalInstance.getAllAccounts()[0]);
    }

    ReactDOM.createRoot(document.getElementById("root")!).render(
        <React.StrictMode>
        <MsalProvider instance={msalInstance}>
            <App />
        </MsalProvider>
        </React.StrictMode>,
    );
}

main();