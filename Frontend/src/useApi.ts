import { useMsal } from "@azure/msal-react";
import { InteractionRequiredAuthError } from "@azure/msal-browser";
import { loginRequest } from "./authConfig";

export function useApi() {
    const { instance, accounts } = useMsal();

    const fetchWithToken = async (url: string) => {
        const account = accounts[0] || instance.getActiveAccount();
        if (!account) throw new Error("No hay una cuenta activa");

        let response;

        try {
            // Solicitar token silenciosamente
            response = await instance.acquireTokenSilent({
                ...loginRequest,
                account,
            });
        } catch (error) {
            // Si falla por interacción requerida (MFA, sesión caducada), forzar re-autenticación
            if (error instanceof InteractionRequiredAuthError) {
                await instance.acquireTokenRedirect(loginRequest);
                throw new Error("Redirigiendo para autenticación", { cause: error });
            }
            throw error;
        }

        // Adjuntar token en el header HTTP
        return fetch(url, {
            headers: {
                Authorization: `Bearer ${response.accessToken}`,
            },
        });
    };

    return { fetchWithToken };
}