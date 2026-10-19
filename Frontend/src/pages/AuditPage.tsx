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

  const labelStyle = { display: "block", marginBottom: "0.25rem" } as const;

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">📜 Auditoría y Timeline de Eventos</h1>
        <p className="page-subtitle">
          Trazabilidad inmutable de eventos de reserva (Solo lectura &bull; Fuente: Tópico Kafka{" "}
          <code>audit.timeline</code>).
        </p>
      </div>

      {error && (
        <div className="alert alert--error" style={{ marginBottom: "1.5rem" }}>
          ⚠️ {error}
        </div>
      )}

      {/* FILTROS (REQUISITO DEL CASO: usuario, fechas, tipo de evento) */}
      <div className="card" style={{ padding: "1rem 1.25rem", marginBottom: "1.75rem" }}>
        <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", alignItems: "flex-end" }}>
          <div style={{ flex: 1, minWidth: "220px" }}>
            <label style={labelStyle}>Buscar por Actor / Usuario / Detalle:</label>
            <input
              type="text"
              placeholder="Ej: operador, huésped, confirmación..."
              value={filterUser}
              onChange={(e) => setFilterUser(e.target.value)}
              style={{ width: "100%" }}
            />
          </div>

          <div>
            <label style={labelStyle}>Tipo de Evento:</label>
            <select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
              <option value="TODOS">Todos los eventos</option>
              <option value="CREACION">CREACIÓN / RESERVA_CREADA</option>
              <option value="CONFIRMACION">CONFIRMACIÓN / RESERVA_CONFIRMADA</option>
              <option value="CHECK_IN">CHECK-IN</option>
              <option value="CHECK_OUT">CHECK-OUT</option>
              <option value="CANCELACION">CANCELACIÓN</option>
            </select>
          </div>

          <div style={{ paddingBottom: "0.4rem", color: "#64748b", fontSize: "0.85rem" }}>
            Mostrando <strong style={{ color: "#7c3aed" }}>{filteredEvents.length}</strong> eventos
          </div>

          <div>
            <button className="btn btn-secondary btn-sm" onClick={refreshAuditLogs} disabled={loading}>
              {loading ? "Cargando..." : "↻ Refrescar Timeline"}
            </button>
          </div>
        </div>
      </div>

      {/* TIMELINE DE EVENTOS */}
      {loading ? (
        <div style={{ display: "flex", justifyContent: "center", padding: "3rem 0" }}>
          <div className="spinner" />
        </div>
      ) : filteredEvents.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <span className="empty-icon">📜</span>
            <span>No se encontraron eventos en la línea de tiempo.</span>
          </div>
        </div>
      ) : (
        <div className="card table-card">
          <table>
            <thead>
              <tr>
                <th>Tipo de Evento</th>
                <th>Acción</th>
                <th>Detalle</th>
                <th>Actor</th>
                <th>Fecha / Hora</th>
              </tr>
            </thead>
            <tbody>
              {filteredEvents.map((evt) => (
                <tr key={evt.id}>
                  <td>
                    <span className={`badge ${getEventBadgeClass(evt.eventType)}`}>{evt.eventType}</span>
                  </td>
                  <td style={{ fontWeight: 700, color: "#1e293b" }}>{evt.action || evt.eventType}</td>
                  <td>{evt.details || evt.payload || "Sin detalles adicionales"}</td>
                  <td>
                    <strong style={{ color: "#0d9488" }}>{evt.actor || evt.performedBy || "Sistema"}</strong>
                  </td>
                  <td style={{ fontSize: "0.8rem", color: "#64748b", whiteSpace: "nowrap" }}>{evt.timestamp}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function getEventBadgeClass(type: string): string {
  if (!type) return "badge--slate";
  const t = type.toUpperCase();
  if (t.includes("CREA")) return "badge--blue";
  if (t.includes("CONFIRM")) return "badge--green";
  if (t.includes("CHECK_IN") || t.includes("CHECKIN")) return "badge--amber";
  if (t.includes("CHECK_OUT") || t.includes("CHECKOUT")) return "badge--slate";
  if (t.includes("CANCEL")) return "badge--red";
  return "badge--violet";
}
