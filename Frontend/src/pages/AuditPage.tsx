import { useState, useEffect } from "react";
import { auditService } from "../api/auditService";
import type { AuditEvent } from "../types";

export function AuditPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [filterUser, setFilterUser] = useState("");
  const [filterType, setFilterType] = useState<string>("TODOS");

  const refreshAuditLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await auditService.getAuditLogs();
      setEvents(data);
    } catch (err: unknown) {
      console.error("Error al cargar logs de auditoría:", err);
      setError("No se pudieron cargar los registros de auditoría desde el microservicio ms-andesstay-audit.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    auditService
      .getAuditLogs()
      .then((data) => {
        if (isMounted) {
          setEvents(data);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          console.error("Error al cargar logs de auditoría:", err);
          setError("No se pudieron cargar los registros de auditoría desde el microservicio ms-andesstay-audit.");
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const filteredEvents = events.filter((ev) => {
    const actorOrUser = ev.actor || ev.performedBy || "";
    const resId = ev.reservationId ? String(ev.reservationId) : "";
    const payloadText = ev.payload || ev.details || "";

    const matchesUser = filterUser
      ? actorOrUser.toLowerCase().includes(filterUser.toLowerCase()) ||
        resId.toLowerCase().includes(filterUser.toLowerCase()) ||
        payloadText.toLowerCase().includes(filterUser.toLowerCase())
      : true;

    const matchesType =
      filterType === "TODOS"
        ? true
        : ev.eventType?.toUpperCase().includes(filterType.toUpperCase());

    return matchesUser && matchesType;
  });

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Auditoría y Timeline de Eventos</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Trazabilidad inmutable de eventos de reserva (Solo lectura &bull; Fuente: Tópico Kafka <code>audit.timeline</code>).
          </p>
        </div>
        <button
          onClick={refreshAuditLogs}
          disabled={loading}
          style={{
            background: "#f1f5f9",
            color: "#334155",
            border: "1px solid #cbd5e1",
            padding: "0.5rem 1rem",
            borderRadius: "6px",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          {loading ? "Cargando..." : "↻ Refrescar Timeline"}
        </button>
      </div>

      {error && (
        <div
          style={{
            padding: "0.75rem 1rem",
            marginBottom: "1.5rem",
            background: "#fffbeb",
            color: "#b45309",
            borderRadius: "6px",
            border: "1px solid #fde68a",
            fontSize: "0.875rem",
          }}
        >
          ℹ️ {error}
        </div>
      )}

      {/* FILTROS (REQUISITO DEL CASO: usuario, fechas, tipo de evento) */}
      <div
        style={{
          background: "white",
          border: "1px solid #e2e8f0",
          borderRadius: "8px",
          padding: "1rem",
          display: "flex",
          gap: "1rem",
          flexWrap: "wrap",
          alignItems: "center",
          marginBottom: "1.5rem",
        }}
      >
        <div style={{ flex: 1, minWidth: "220px" }}>
          <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "#475569", marginBottom: "0.25rem" }}>
            Buscar por Actor / Usuario / Detalle:
          </label>
          <input
            type="text"
            placeholder="Ej: operador, huésped, confirmación..."
            value={filterUser}
            onChange={(e) => setFilterUser(e.target.value)}
            style={{ width: "100%", padding: "0.4rem 0.6rem", borderRadius: "4px", border: "1px solid #cbd5e1" }}
          />
        </div>

        <div>
          <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "#475569", marginBottom: "0.25rem" }}>
            Tipo de Evento:
          </label>
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            style={{ padding: "0.4rem 0.6rem", borderRadius: "4px", border: "1px solid #cbd5e1" }}
          >
            <option value="TODOS">Todos los eventos</option>
            <option value="CREACION">CREACIÓN / RESERVA_CREADA</option>
            <option value="CONFIRMACION">CONFIRMACIÓN / RESERVA_CONFIRMADA</option>
            <option value="CHECK_IN">CHECK-IN</option>
            <option value="CHECK_OUT">CHECK-OUT</option>
            <option value="CANCELACION">CANCELACIÓN</option>
          </select>
        </div>

        <div style={{ alignSelf: "flex-end", color: "#64748b", fontSize: "0.85rem" }}>
          Mostrando {filteredEvents.length} eventos
        </div>
      </div>

      {/* TIMELINE DE EVENTOS */}
      {loading ? (
        <div style={{ padding: "3rem", textAlign: "center", color: "#64748b" }}>
          Cargando registros de auditoría desde el backend...
        </div>
      ) : filteredEvents.length === 0 ? (
        <div style={{ padding: "3rem", textAlign: "center", color: "#94a3b8", background: "white", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
          No se encontraron eventos en la línea de tiempo.
        </div>
      ) : (
        <div style={{ position: "relative", paddingLeft: "1.5rem", borderLeft: "3px solid #cbd5e1" }}>
          {filteredEvents.map((evt) => (
            <div
              key={evt.id}
              style={{
                position: "relative",
                marginBottom: "1.5rem",
                background: "white",
                border: "1px solid #e2e8f0",
                borderRadius: "8px",
                padding: "1rem 1.25rem",
                boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
              }}
            >
              {/* Indicador visual en el punto del timeline */}
              <div
                style={{
                  position: "absolute",
                  left: "-1.9rem",
                  top: "1.2rem",
                  width: "12px",
                  height: "12px",
                  borderRadius: "50%",
                  background: getEventColor(evt.eventType),
                  border: "2px solid white",
                  boxShadow: "0 0 0 2px #cbd5e1",
                }}
              />

              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "0.5rem" }}>
                <div>
                  <span
                    style={{
                      background: getEventBg(evt.eventType),
                      color: getEventColor(evt.eventType),
                      fontSize: "0.75rem",
                      fontWeight: 700,
                      padding: "0.2rem 0.5rem",
                      borderRadius: "12px",
                      marginRight: "0.75rem",
                    }}
                  >
                    {evt.eventType}
                  </span>
                  <span style={{ fontSize: "0.9rem", fontWeight: 700, color: "#1e293b" }}>
                    {evt.action || evt.eventType}
                  </span>
                </div>
                <span style={{ fontSize: "0.8rem", color: "#64748b" }}>{evt.timestamp}</span>
              </div>

              <p style={{ margin: "0.5rem 0", fontSize: "0.9rem", color: "#334155" }}>
                {evt.details || evt.payload || "Sin detalles adicionales"}
              </p>

              <div style={{ fontSize: "0.75rem", color: "#64748b" }}>
                Realizado por: <strong>{evt.actor || evt.performedBy || "Sistema"}</strong>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function getEventColor(type: string): string {
  if (!type) return "#64748b";
  const t = type.toUpperCase();
  if (t.includes("CREA")) return "#2563eb";
  if (t.includes("CONFIRM")) return "#16a34a";
  if (t.includes("CHECK_IN") || t.includes("CHECKIN")) return "#0891b2";
  if (t.includes("CHECK_OUT") || t.includes("CHECKOUT")) return "#475569";
  if (t.includes("CANCEL")) return "#dc2626";
  return "#8b5cf6";
}

function getEventBg(type: string): string {
  if (!type) return "#f1f5f9";
  const t = type.toUpperCase();
  if (t.includes("CREA")) return "#dbeafe";
  if (t.includes("CONFIRM")) return "#dcfce7";
  if (t.includes("CHECK_IN") || t.includes("CHECKIN")) return "#cffafe";
  if (t.includes("CHECK_OUT") || t.includes("CHECKOUT")) return "#f1f5f9";
  if (t.includes("CANCEL")) return "#fee2e2";
  return "#ede9fe";
}
