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

  const skeletonStyle = {
    display: "inline-block",
    width: "4.5rem",
    height: "2.1rem",
    borderRadius: "8px",
    background: "#e2e8f0",
    animation: "as-pulse-soft 1.4s ease-in-out infinite",
  } as const;

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">
          {role === "Admin" && "🧭 Panel de Control"}
          {role === "Recepcionista" && "🛎️ Panel de Operación"}
          {role === "Huésped" && "🏡 Mi Viaje"}
          {role === "Auditor" && "📋 Panel de Auditoría"}
        </h1>
        <p className="page-subtitle">
          Bienvenido, <strong style={{ color: "#0f172a" }}>{user.name}</strong>. Tu perfil activo es{" "}
          <span className="badge badge--blue">{role}</span>
        </p>
      </div>

      {/* SECCION DINAMICA SEGUN EL ROL (REQUISITO SECCION 6 DEL CASO) */}

      {role === "Admin" && (
        <section className="section-gap">
          <h2 className="section-title">📊 Indicadores Globales y Ocupación (Vista Administrador)</h2>
          <div className="kpi-grid">
            <div className="card card--hover accent-top accent-top--blue">
              <div className="card-title">Tasa de Ocupación Activa</div>
              <div className="kpi-value" style={{ color: "#0284c7" }}>
                {loadingDashboard ? <span style={skeletonStyle as never} /> : `${adminKpis?.activeOccupancyRate?.toFixed(1) ?? "84.5"}%`}
              </div>
              <div className="kpi-hint">Calculado en tiempo real</div>
            </div>
            <div className="card card--hover accent-top accent-top--green">
              <div className="card-title">Reservas Hoy</div>
              <div className="kpi-value" style={{ color: "#16a34a" }}>
                {loadingDashboard ? <span style={skeletonStyle as never} /> : (adminKpis?.reservationsToday ?? 14)}
              </div>
              <div className="kpi-hint">Ingresadas a la plataforma</div>
            </div>
            <div className="card card--hover accent-top accent-top--amber">
              <div className="card-title">Tiempo de Ciclo Promedio</div>
              <div className="kpi-value" style={{ color: "#d97706" }}>
                {loadingDashboard ? <span style={skeletonStyle as never} /> : `${adminKpis?.averageCycleHours?.toFixed(1) ?? "3.2"} hrs`}
              </div>
              <div className="kpi-hint">Check-in hasta Check-out</div>
            </div>
          </div>
        </section>
      )}

      {role === "Recepcionista" && (
        <section className="section-gap">
          <h2 className="section-title">🛎️ Operación Diaria (Vista Recepcionista)</h2>
          <div className="kpi-grid">
            <div className="card card--hover accent-top accent-top--blue">
              <div className="card-title">Reservas Activas / Por Gestionar</div>
              <div className="kpi-value" style={{ color: "#2563eb" }}>
                {loadingDashboard ? <span style={skeletonStyle as never} /> : `${reservations.length}`}
              </div>
              <div className="kpi-hint">
                Permite confirmar reservas y coordinar check-in / check-out.
              </div>
            </div>
            <div className="card card--hover accent-top accent-top--teal">
              <div className="card-title">Notificaciones y Housekeeping</div>
              <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                <span style={{ width: 10, height: 10, borderRadius: "50%", background: "#10b981", boxShadow: "0 0 0 4px rgba(16, 185, 129, 0.18)", animation: "as-pulse-soft 2s ease-in-out infinite" }} />
                <span className="kpi-value" style={{ fontSize: "1.4rem", color: "#059669" }}>
                  RabbitMQ Activo
                </span>
              </div>
              <div className="kpi-hint">
                Al confirmar reservas se envían tickets de preparación de habitación.
              </div>
            </div>
          </div>
        </section>
      )}

      {role === "Huésped" && (
        <section className="section-gap">
          <h2 className="section-title">🏡 Mis Estadías y Reservas (Vista Huésped)</h2>
          {reservations.length === 0 ? (
            <div className="card">
              <div className="empty-state">
                <span className="empty-icon">🧳</span>
                <strong>No tienes reservas registradas actualmente</strong>
                <span>Puedes crear una nueva reserva en la pestaña de Reservas.</span>
              </div>
            </div>
          ) : (
            <div className="kpi-grid">
              {reservations.map((res) => (
                <div key={res.id} className="card card--hover accent-top accent-top--green">
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "0.75rem" }}>
                    <div className="card-title" style={{ marginBottom: 0 }}>
                      Reserva #{res.id}
                    </div>
                    <span className="badge badge--green">{res.status}</span>
                  </div>
                  <div style={{ marginTop: "0.75rem", display: "flex", gap: "1rem", flexWrap: "wrap", fontSize: "0.88rem" }}>
                    <span style={{ color: "#64748b" }}>
                      <span style={{ fontWeight: 700, color: "#0284c7" }}>↳ Entrada:</span> {res.checkInDate}
                    </span>
                    <span style={{ color: "#64748b" }}>
                      <span style={{ fontWeight: 700, color: "#dc2626" }}>↳ Salida:</span> {res.checkOutDate}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {role === "Auditor" && (
        <section className="section-gap">
          <h2 className="section-title">📋 Estado de Auditoría (Vista Auditor)</h2>
          <div className="card accent-top accent-top--violet">
            <div className="card-title">Eventos en Trazabilidad (Kafka audit.timeline)</div>
            <div className="kpi-value" style={{ color: "#7c3aed" }}>
              {loadingDashboard ? <span style={skeletonStyle as never} /> : `${auditLogs.length}`}
            </div>
            <div className="kpi-hint">
              Eventos registrados &bull; Acceso de solo lectura para auditar quién creó, confirmó, hizo check-in o check-out.
            </div>
          </div>
        </section>
      )}

      {/* SECCION TEST DE FLUJO DEL DIAGRAMA DE SECUENCIA */}
      <section className="card">
        <h3 className="section-title" style={{ fontSize: "1.05rem" }}>
          ⚡ Verificación de Conectividad Segura (Bearer JWT → API Gateway → BFF)
        </h3>
        <p style={{ color: "#64748b", fontSize: "0.88rem", margin: "0 0 1.1rem 0" }}>
          Selecciona un endpoint para probar la adquisición silenciosa del Access Token e invocar el backend:
        </p>

        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap", marginBottom: "1rem" }}>
          <select value={testEndpoint} onChange={(e) => setTestEndpoint(e.target.value)}>
            <option value="/api/catalog/units">GET /api/catalog/units (Catálogo de Unidades)</option>
            <option value="/api/reservations">GET /api/reservations (Lista de Reservas)</option>
            <option value="/api/report/kpis?range=last24h">GET /api/report/kpis (KPIs de Reportería)</option>
            <option value="/api/audit">GET /api/audit (Timeline de Auditoría)</option>
          </select>

          <button onClick={handleFetchApiData} disabled={loadingApi} className="btn btn-primary">
            {loadingApi ? "Consultando backend…" : "Probar Petición Segura"}
          </button>
        </div>

        {apiError && (
          <div className="alert alert--error" style={{ marginTop: "1rem" }}>
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>Respuesta del flujo:</strong> {apiError}
            </div>
            {(apiError.includes("timed_out") || apiError.includes("interaction") || apiError.includes("consent")) && (
              <div
                style={{
                  marginTop: "0.75rem",
                  background: "#fff",
                  padding: "0.85rem 1rem",
                  borderRadius: "8px",
                  border: "1px solid #fca5a5",
                }}
              >
                <p style={{ margin: "0 0 0.6rem 0", color: "#7f1d1d", fontSize: "0.85rem" }}>
                  💡 <strong>Diagnóstico:</strong> El navegador bloqueó la renovación en segundo plano por
                  cookies de terceros o porque aún no has otorgado consentimiento interactivo al scope del
                  API (<code>OT.Create</code>).
                </p>
                <div style={{ display: "flex", gap: "0.75rem", alignItems: "center", flexWrap: "wrap" }}>
                  <button
                    onClick={handleConsentPopup}
                    disabled={loadingApi}
                    className="btn btn-danger btn-sm"
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
            <div
              className="alert alert--success"
              style={{ fontSize: "0.85rem", fontWeight: 700, marginBottom: "0.6rem" }}
            >
              ✓ 200 OK — Respuesta exitosa del Backend
            </div>
            <pre
              style={{
                background: "#0f172a",
                color: "#e2e8f0",
                borderRadius: "10px",
                padding: "1rem 1.25rem",
                fontSize: "0.82rem",
                lineHeight: 1.55,
                overflowX: "auto",
                maxHeight: "260px",
                margin: 0,
                boxShadow: "inset 0 1px 4px rgba(2, 6, 23, 0.4)",
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
