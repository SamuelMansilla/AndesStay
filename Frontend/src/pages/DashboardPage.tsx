import { useState } from "react";
import { useUserRole } from "../hooks/useUserRole";
import axiosClient from "../api/axiosClient";
import axios, { type AxiosError } from "axios";

export function DashboardPage() {
  const { role, user } = useUserRole();

  // Estado para la prueba de API (Pasos 4 a 12 del diagrama de secuencia)
  const [apiData, setApiData] = useState<Record<string, unknown> | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [loadingApi, setLoadingApi] = useState(false);

  const handleFetchApiData = async () => {
    setLoadingApi(true);
    setApiError(null);
    setApiData(null);
    try {
      // Paso 7 del flujo: GET /v2/datos con inyección automática de Bearer JWT
      const res = await axiosClient.get<Record<string, unknown>>("/datos");
      setApiData(res.data);
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const axiosErr = err as AxiosError<{ message?: string }>;
        if (axiosErr.response?.status === 401) {
          setApiError("401 Unauthorized: El token es inválido o ausente ante el API Gateway.");
        } else {
          setApiError(`HTTP ${axiosErr.response?.status || "Error"}: ${axiosErr.message}`);
        }
      } else if (err instanceof Error) {
        setApiError(err.message);
      } else {
        setApiError("Error inesperado al consultar el API Gateway.");
      }
    } finally {
      setLoadingApi(false);
    }
  };

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ marginBottom: "2rem" }}>
        <h1 style={{ margin: "0 0 0.5rem 0", color: "#0f172a" }}>
          Panel de Control - AndesStay
        </h1>
        <p style={{ color: "#64748b", margin: 0 }}>
          Bienvenido, <strong>{user.name}</strong>. Tu perfil activo en la plataforma es{" "}
          <span
            style={{
              fontWeight: 700,
              padding: "0.2rem 0.5rem",
              background: "#e2e8f0",
              borderRadius: "4px",
            }}
          >
            {role}
          </span>
        </p>
      </div>

      {/* SECCION DINAMICA SEGUN EL ROL (REQUISITO SECCION 6 DEL CASO) */}

      {role === "Admin" && (
        <section style={{ marginBottom: "2.5rem" }}>
          <h2 style={{ fontSize: "1.25rem", color: "#1e293b", marginBottom: "1rem" }}>
            📊 Indicadores Globales y Ocupación (Vista Administrador)
          </h2>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
              gap: "1.25rem",
            }}
          >
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Ocupación Activa</div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "#0284c7" }}>82%</div>
              <div style={{ fontSize: "0.85rem", color: "#64748b" }}>16 de 20 cabañas ocupadas</div>
            </div>
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Reservas Hoy</div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "#16a34a" }}>14</div>
              <div style={{ fontSize: "0.85rem", color: "#64748b" }}>+4 frente a ayer</div>
            </div>
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Tiempo de Ciclo Promedio</div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "#d97706" }}>3.2 días</div>
              <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Check-in hasta Check-out</div>
            </div>
          </div>
        </section>
      )}

      {role === "Recepcionista" && (
        <section style={{ marginBottom: "2.5rem" }}>
          <h2 style={{ fontSize: "1.25rem", color: "#1e293b", marginBottom: "1rem" }}>
            🛎️ Operación Diaria (Vista Recepcionista)
          </h2>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
              gap: "1.25rem",
            }}
          >
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Llegadas de Hoy (Check-in)</div>
              <div style={{ fontSize: "1.75rem", fontWeight: 700, color: "#2563eb" }}>6 Huéspedes</div>
              <ul style={{ margin: "0.5rem 0 0 0", paddingLeft: "1.2rem", fontSize: "0.85rem", color: "#475569" }}>
                <li>Cabaña Los Alerces (Juan Pérez) - Confirmada</li>
                <li>Hab. 102 (María González) - Check-in Pendiente</li>
              </ul>
            </div>
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Salidas de Hoy (Check-out)</div>
              <div style={{ fontSize: "1.75rem", fontWeight: 700, color: "#dc2626" }}>4 Habitaciones</div>
              <div style={{ fontSize: "0.85rem", color: "#64748b", marginTop: "0.5rem" }}>
                Requiere notificación a housekeeping para preparación.
              </div>
            </div>
          </div>
        </section>
      )}

      {role === "Huésped" && (
        <section style={{ marginBottom: "2.5rem" }}>
          <h2 style={{ fontSize: "1.25rem", color: "#1e293b", marginBottom: "1rem" }}>
            🏡 Mis Estadías y Reservas (Vista Huésped)
          </h2>
          <div style={cardStyle}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: "1.1rem" }}>Cabaña Volcán Osorno #4</div>
                <div style={{ color: "#64748b", fontSize: "0.9rem" }}>Check-in: 15 Sep 2026 - Check-out: 18 Sep 2026</div>
              </div>
              <span
                style={{
                  background: "#dcfce7",
                  color: "#15803d",
                  fontWeight: 700,
                  fontSize: "0.8rem",
                  padding: "0.35rem 0.75rem",
                  borderRadius: "12px",
                }}
              >
                CONFIRMADA
              </span>
            </div>
            <p style={{ marginTop: "1rem", fontSize: "0.85rem", color: "#475569" }}>
              Tu reserva está lista. Recibirás tu ticket y voucher vía correo/push antes de tu llegada.
            </p>
          </div>
        </section>
      )}

      {role === "Auditor" && (
        <section style={{ marginBottom: "2.5rem" }}>
          <h2 style={{ fontSize: "1.25rem", color: "#1e293b", marginBottom: "1rem" }}>
            📋 Estado de Auditoría (Vista Auditor)
          </h2>
          <div style={cardStyle}>
            <div style={cardTitleStyle}>Trazabilidad Activa</div>
            <p style={{ fontSize: "0.9rem", color: "#475569" }}>
              Se cuenta con acceso de solo lectura al timeline de eventos de reserva emitidos por los microservicios.
            </p>
          </div>
        </section>
      )}

      {/* SECCION TEST DE FLUJO DEL DIAGRAMA DE SECUENCIA */}
      <section
        style={{
          background: "#ffffff",
          border: "1px solid #e2e8f0",
          borderRadius: "8px",
          padding: "1.5rem",
          boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
        }}
      >
        <h3 style={{ margin: "0 0 0.5rem 0", color: "#0f172a" }}>
          ⚡ Verificación del Flujo de Trabajo (Diagrama de Secuencia)
        </h3>
        <p style={{ color: "#64748b", fontSize: "0.9rem", margin: "0 0 1rem 0" }}>
          Paso 4: Haz clic en el botón para solicitar silenciosamente el Access Token JWT e invocar{" "}
          <code>GET /v2/datos</code> en AWS API Gateway mediante <code>axiosClient</code>.
        </p>

        <button
          onClick={handleFetchApiData}
          disabled={loadingApi}
          style={{
            background: "#2563eb",
            color: "white",
            border: "none",
            padding: "0.6rem 1.25rem",
            borderRadius: "6px",
            fontWeight: 600,
            fontSize: "0.9rem",
            cursor: loadingApi ? "wait" : "pointer",
          }}
        >
          {loadingApi ? "Consultando AWS API Gateway..." : "Obtener Datos vía API"}
        </button>

        {apiError && (
          <div
            style={{
              marginTop: "1rem",
              padding: "0.75rem",
              background: "#fee2e2",
              color: "#991b1b",
              borderRadius: "6px",
              fontSize: "0.9rem",
            }}
          >
            <strong>Respuesta del flujo:</strong> {apiError}
          </div>
        )}

        {apiData && (
          <div style={{ marginTop: "1rem" }}>
            <div style={{ fontSize: "0.85rem", fontWeight: 700, color: "#16a34a", marginBottom: "0.5rem" }}>
              200 OK - Datos recibidos desde el Backend:
            </div>
            <pre
              style={{
                background: "#f8fafc",
                border: "1px solid #cbd5e1",
                borderRadius: "6px",
                padding: "1rem",
                fontSize: "0.85rem",
                overflowX: "auto",
              }}
            >
              {JSON.stringify(apiData, null, 2)}
            </pre>
          </div>
        )}
      </section>
    </div>
  );
}

const cardStyle = {
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: "8px",
  padding: "1.25rem",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
};

const cardTitleStyle = {
  fontSize: "0.9rem",
  fontWeight: 600,
  color: "#64748b",
  marginBottom: "0.5rem",
};
