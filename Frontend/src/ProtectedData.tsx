import { useState } from "react";
import {
    AuthenticatedTemplate,
    UnauthenticatedTemplate,
} from "@azure/msal-react";
import axios, { type AxiosError } from "axios";
import axiosClient from "./api/axiosClient";

export function ProtectedData() {
    const [data, setData] = useState<Record<string, unknown> | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const handleFetchData = async () => {
        setLoading(true);
        setError(null);
        setData(null);
        try {
            // Pasos 4 a 11 del flujo: GET /v2/datos (con Header Authorization: Bearer <JWT>)
            const response = await axiosClient.get<Record<string, unknown>>("/datos");
            setData(response.data);
        } catch (err) {
            console.error("Error al consultar API Gateway:", err);
            if (axios.isAxiosError(err)) {
                const axiosErr = err as AxiosError<{ message?: string }>;
                if (axiosErr.response?.status === 401) {
                    setError("401 Unauthorized: Token inválido o ausente ante el API Gateway.");
                } else {
                    setError(`Error HTTP ${axiosErr.response?.status ?? ""}: ${axiosErr.message}`);
                }
            } else if (err instanceof Error) {
                setError(err.message);
            } else {
                setError("Error desconocido al consultar el API Gateway.");
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <AuthenticatedTemplate>
                <button onClick={handleFetchData} disabled={loading}>
                    {loading ? "Consultando..." : "Obtener Datos vía API"}
                </button>
                {error && (
                    <div style={{ color: "#d32f2f", marginTop: "1rem", fontWeight: "bold" }}>
                        ⚠️ {error}
                    </div>
                )}
                {data && (
                    <pre style={{ marginTop: "1rem", background: "#f5f5f5", color: "#333", padding: "1rem", borderRadius: "4px" }}>
                        {JSON.stringify(data, null, 2)}
                    </pre>
                )}
            </AuthenticatedTemplate>
            <UnauthenticatedTemplate>
                <p>
                    ⚠️ Acceso denegado. Debes iniciar sesión para consultar este recurso.
                </p>
            </UnauthenticatedTemplate>
        </div>
    );
}