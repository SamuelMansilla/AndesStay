import { useState } from "react";
import type { AuditEvent } from "../types";

const INITIAL_AUDIT_EVENTS: AuditEvent[] = [
  {
    id: "EVT-9001",
    reservationId: "RES-101",
    action: "CAMBIO_ESTADO",
    performedBy: "admin@andesstay.cl (Admin)",
    timestamp: "2026-09-08 14:25:10",
    details: "Estado actualizado de CREADA a CONFIRMADA tras comprobante de pago.",
    eventType: "CONFIRMACION",
  },
  {
    id: "EVT-9002",
    reservationId: "RES-101",
    action: "CREACION_RESERVA",
    performedBy: "carlos.mendoza@email.com (Huésped)",
    timestamp: "2026-09-08 14:20:00",
    details: "Reserva web generada para Cabaña Los Alerces #1 por 3 noches.",
    eventType: "CREACION",
  },
  {
    id: "EVT-9003",
    reservationId: "RES-102",
    action: "NOTIFICACION_HOUSEKEEPING",
    performedBy: "operador@andesstay.cl (Recepcionista)",
    timestamp: "2026-09-09 10:20:00",
    details: "Enviado ticket de preparación a RabbitMQ (q.cmd.housekeeping).",
    eventType: "CONFIRMACION",
  },
  {
    id: "EVT-9004",
    reservationId: "RES-098",
    action: "CHECK_IN_REGISTRADO",
    performedBy: "operador@andesstay.cl (Recepcionista)",
    timestamp: "2026-09-07 15:30:12",
    details: "Huésped ingresó a Habitación 105. Estado pasó a EN_ESTADÍA.",
    eventType: "CHECK_IN",
  },
  {
    id: "EVT-9005",
    reservationId: "RES-095",
    action: "CHECK_OUT_FINALIZADO",
    performedBy: "operador@andesstay.cl (Recepcionista)",
    timestamp: "2026-09-06 11:00:45",
    details: "Entrega de llaves y cierre de cuenta. Estado CHECKOUT.",
    eventType: "CHECK_OUT",
  },
];

export function AuditPage() {
  const [events] = useState<AuditEvent[]>(INITIAL_AUDIT_EVENTS);
  const [filterUser, setFilterUser] = useState("");
  const [filterType, setFilterType] = useState<string>("TODOS");

  const filteredEvents = events.filter((ev) => {
    const matchesUser = filterUser
      ? ev.performedBy.toLowerCase().includes(filterUser.toLowerCase()) ||
        ev.reservationId.toLowerCase().includes(filterUser.toLowerCase())
      : true;
    const matchesType = filterType === "TODOS" ? true : ev.eventType === filterType;
    return matchesUser && matchesType;
  });

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ marginBottom: "1.5rem" }}>
        <h1 style={{ margin: 0, color: "#0f172a" }}>Auditoría y Timeline de Eventos</h1>
        <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
          Trazabilidad inmutable de eventos de reserva (Solo lectura &bull; Fuente: Tópico Kafka <code>audit.timeline</code>).
        </p>
      </div>

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
            Buscar por Usuario o ID de Reserva:
          </label>
          <input
            type="text"
            placeholder="Ej: operador, RES-101..."
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
            <option value="CREACION">CREACION</option>
            <option value="CONFIRMACION">CONFIRMACION</option>
            <option value="CHECK_IN">CHECK_IN</option>
            <option value="CHECK_OUT">CHECK_OUT</option>
            <option value="CANCELACION">CANCELACION</option>
          </select>
        </div>

        <div style={{ alignSelf: "flex-end", color: "#64748b", fontSize: "0.85rem" }}>
          Mostrando {filteredEvents.length} eventos
        </div>
      </div>

      {/* TIMELINE DE EVENTOS */}
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
            {/* Punto del timeline */}
            <div
              style={{
                position: "absolute",
                left: "-1.9rem",
                top: "1.2rem",
                width: "12px",
                height: "12px",
                borderRadius: "50%",
                background: "#0284c7",
                border: "2px solid white",
              }}
            />

            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.35rem" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                <span style={{ fontWeight: 700, color: "#0f172a" }}>{evt.action}</span>
                <span
                  style={{
                    background: "#e0f2fe",
                    color: "#0369a1",
                    fontSize: "0.75rem",
                    fontWeight: 700,
                    padding: "0.15rem 0.45rem",
                    borderRadius: "4px",
                  }}
                >
                  {evt.reservationId}
                </span>
              </div>
              <span style={{ fontSize: "0.8rem", color: "#64748b" }}>{evt.timestamp}</span>
            </div>

            <p style={{ margin: "0 0 0.5rem 0", fontSize: "0.9rem", color: "#334155" }}>
              {evt.details}
            </p>

            <div style={{ fontSize: "0.8rem", color: "#64748b" }}>
              Responsable: <strong>{evt.performedBy}</strong> &bull; Event ID: <code>{evt.id}</code>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
