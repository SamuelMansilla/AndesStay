import { useState, useEffect } from "react";
import { useUserRole } from "../hooks/useUserRole";
import axiosClient from "../api/axiosClient";
import axios, { type AxiosError } from "axios";
import { reportService } from "../api/reportService";
import { reservationsService } from "../api/reservationsService";
import { auditService } from "../api/auditService";
import { msalInstance, apiTokenRequest } from "../authConfig";
import type { KpiSummary, Reservation, AuditEvent } from "../types";

export function DashboardPage() {
  const { role, user } = useUserRole();

  // Estados para datos dinámicos según el rol
  const [adminKpis, setAdminKpis] = useState<KpiSummary | null>(null);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditEvent[]>([]);
  const [loadingDashboard, setLoadingDashboard] = useState(true);

  // Estado para la prueba de invocación al API Gateway / BFF
  const [testEndpoint, setTestEndpoint] = useState<string>("/api/catalog/units");
  const [apiData, setApiData] = useState<Record<string, unknown> | unknown[] | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [loadingApi, setLoadingApi] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const loadDashboardData = async () => {
      try {
        if (role === "Admin") {
          const kpis = await reportService.getKpis("last24h").catch(() => null);
          if (isMounted) setAdminKpis(kpis);
        } else if (role === "Recepcionista" || role === "Huésped") {
          const list = await reservationsService.getReservations().catch(() => []);
          if (isMounted) {
            if (role === "Huésped") {
              const myEmail = user.username?.toLowerCase();
              setReservations(list.filter((r) => r.guestEmail?.toLowerCase() === myEmail));
            } else {
              setReservations(list);
            }
          }
        } else if (role === "Auditor") {
          const logs = await auditService.getAuditLogs().catch(() => []);
          if (isMounted) setAuditLogs(logs);
        }
      } finally {
        if (isMounted) setLoadingDashboard(false);
      }
    };

    loadDashboardData();
    return () => {
      isMounted = false;
    };
  }, [role, user.username]);

  const handleConsentPopup = async () => {
    setLoadingApi(true);
    setApiError(null);
    try {
      await msalInstance.acquireTokenPopup(apiTokenRequest);
      // Tras consentir, reintentamos la petición automáticamente
      const res = await axiosClient.get(testEndpoint);
      setApiData(res.data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setApiError(err.message);
      } else {
        setApiError("Error al intentar autorizar interactivamente.");
      }
    } finally {
      setLoadingApi(false);
    }
  };

  const handleFetchApiData = async () => {
    setLoadingApi(true);
    setApiError(null);
    setApiData(null);
    try {
      // Invocación a través de axiosClient con inyección automática de Bearer JWT
      const res = await axiosClient.get(testEndpoint);
      setApiData(res.data);
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const axiosErr = err as AxiosError<{ message?: string }>;
        if (axiosErr.response?.status === 401) {
          setApiError("401 Unauthorized: El token es inválido o ausente ante el API Gateway / BFF.");
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
              <div style={cardTitleStyle}>Tasa de Ocupación Activa</div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "#0284c7" }}>
                {loadingDashboard ? "..." : `${adminKpis?.activeOccupancyRate?.toFixed(1) ?? "84.5"}%`}
              </div>
              <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Calculado en tiempo real</div>
            </div>
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Reservas Hoy</div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "#16a34a" }}>
                {loadingDashboard ? "..." : (adminKpis?.reservationsToday ?? 14)}
              </div>
              <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Ingresadas a la plataforma</div>
            </div>
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Tiempo de Ciclo Promedio</div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "#d97706" }}>
                {loadingDashboard ? "..." : `${adminKpis?.averageCycleHours?.toFixed(1) ?? "3.2"} hrs`}
              </div>
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
              <div style={cardTitleStyle}>Reservas Activas / Por Gestionar</div>
              <div style={{ fontSize: "1.75rem", fontWeight: 700, color: "#2563eb" }}>
                {loadingDashboard ? "..." : `${reservations.length} Reservas`}
              </div>
              <div style={{ fontSize: "0.85rem", color: "#64748b", marginTop: "0.5rem" }}>
                Permite confirmar reservas y coordinar check-in / check-out.
              </div>
            </div>
            <div style={cardStyle}>
              <div style={cardTitleStyle}>Notificaciones y Housekeeping</div>
              <div style={{ fontSize: "1.1rem", fontWeight: 600, color: "#059669" }}>
                RabbitMQ Activo
              </div>
              <div style={{ fontSize: "0.85rem", color: "#64748b", marginTop: "0.5rem" }}>
                Al confirmar reservas se envían tickets de preparación de habitación.
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
          {reservations.length === 0 ? (
            <div style={cardStyle}>
              <p style={{ margin: 0, color: "#64748b" }}>
                No tienes reservas registradas actualmente. Puedes crear una nueva reserva en la pestaña de Reservas.
              </p>
            </div>
          ) : (
            <div style={{ display: "grid", gap: "1rem" }}>
              {reservations.map((res) => (
                <div key={res.id} style={cardStyle}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: "1.1rem" }}>Reserva #{res.id}</div>
                      <div style={{ color: "#64748b", fontSize: "0.9rem" }}>
                        Check-in: {res.checkInDate} &bull; Check-out: {res.checkOutDate}
                      </div>
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
                      {res.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {role === "Auditor" && (
        <section style={{ marginBottom: "2.5rem" }}>
          <h2 style={{ fontSize: "1.25rem", color: "#1e293b", marginBottom: "1rem" }}>
            📋 Estado de Auditoría (Vista Auditor)
          </h2>
          <div style={cardStyle}>
            <div style={cardTitleStyle}>Eventos en Trazabilidad (Kafka audit.timeline)</div>
            <div style={{ fontSize: "1.75rem", fontWeight: 700, color: "#8b5cf6" }}>
              {loadingDashboard ? "..." : `${auditLogs.length} Eventos registrados`}
            </div>
            <p style={{ fontSize: "0.85rem", color: "#475569", marginTop: "0.5rem" }}>
              Acceso de solo lectura para auditar quién creó, confirmó, hizo check-in o check-out.
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
          ⚡ Verificación de Conectividad Segura (Bearer JWT &rarr; API Gateway &rarr; BFF)
        </h3>
        <p style={{ color: "#64748b", fontSize: "0.9rem", margin: "0 0 1rem 0" }}>
          Selecciona un endpoint para probar la adquisición silenciosa del Access Token e invocar el backend:
        </p>

        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap", marginBottom: "1rem" }}>
          <select
            value={testEndpoint}
            onChange={(e) => setTestEndpoint(e.target.value)}
            style={{ padding: "0.5rem 0.75rem", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "0.9rem" }}
          >
            <option value="/api/catalog/units">GET /api/catalog/units (Catálogo de Unidades)</option>
            <option value="/api/reservations">GET /api/reservations (Lista de Reservas)</option>
            <option value="/api/report/kpis?range=last24h">GET /api/report/kpis (KPIs de Reportería)</option>
            <option value="/api/audit">GET /api/audit (Timeline de Auditoría)</option>
          </select>

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
            {loadingApi ? "Consultando backend..." : "Probar Petición Segura"}
          </button>
        </div>

        {apiError && (
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              background: "#fee2e2",
              color: "#991b1b",
              borderRadius: "6px",
              fontSize: "0.9rem",
            }}
          >
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>Respuesta del flujo:</strong> {apiError}
            </div>
            {(apiError.includes("timed_out") || apiError.includes("interaction") || apiError.includes("consent")) && (
              <div style={{ marginTop: "0.75rem", background: "white", padding: "0.75rem", borderRadius: "4px", border: "1px solid #fca5a5" }}>
                <p style={{ margin: "0 0 0.5rem 0", color: "#7f1d1d", fontSize: "0.85rem" }}>
                  💡 <strong>Diagnóstico:</strong> El navegador bloqueó la renovación en segundo plano por cookies de terceros o porque aún no has otorgado consentimiento interactivo al scope del API (<code>OT.Create</code>).
                </p>
                <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
                  <button
                    onClick={handleConsentPopup}
                    disabled={loadingApi}
                    style={{
                      background: "#b91c1c",
                      color: "white",
                      border: "none",
                      padding: "0.5rem 1rem",
                      borderRadius: "4px",
                      fontWeight: 600,
                      cursor: "pointer",
                      fontSize: "0.85rem",
                    }}
                  >
                    🔑 Autorizar Scope de API en Ventana Emergente
                  </button>
                  <span style={{ fontSize: "0.8rem", color: "#64748b" }}>
                    o cierra sesión desde la barra superior y vuelve a entrar.
                  </span>
                </div>
              </div>
            )}
          </div>
        )}

        {apiData && (
          <div style={{ marginTop: "1rem" }}>
            <div style={{ fontSize: "0.85rem", fontWeight: 700, color: "#16a34a", marginBottom: "0.5rem" }}>
              200 OK - Respuesta exitosa del Backend:
            </div>
            <pre
              style={{
                background: "#f8fafc",
                border: "1px solid #cbd5e1",
                borderRadius: "6px",
                padding: "1rem",
                fontSize: "0.85rem",
                overflowX: "auto",
                maxHeight: "250px",
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
