import { useState } from "react";
import { useUserRole } from "../hooks/useUserRole";
import type { Reservation, ReservationStatus } from "../types";

const INITIAL_RESERVATIONS: Reservation[] = [
  {
    id: "RES-101",
    guestName: "Carlos Mendoza",
    guestEmail: "carlos.mendoza@email.com",
    unitId: "U-1",
    unitName: "Cabaña Los Alerces #1",
    checkInDate: "2026-09-12",
    checkOutDate: "2026-09-15",
    status: "CONFIRMADA",
    totalPrice: 240000,
    createdAt: "2026-09-08 14:20",
  },
  {
    id: "RES-102",
    guestName: "Andrea Morales",
    guestEmail: "andrea.m@email.com",
    unitId: "U-2",
    unitName: "Habitación Vista Lago 201",
    checkInDate: "2026-09-10",
    checkOutDate: "2026-09-13",
    status: "CHECKIN_PENDIENTE",
    totalPrice: 180000,
    createdAt: "2026-09-09 10:15",
  },
  {
    id: "RES-103",
    guestName: "Roberto Silva",
    guestEmail: "rsilva@email.com",
    unitId: "U-3",
    unitName: "Lodge Refugio Andino",
    checkInDate: "2026-09-14",
    checkOutDate: "2026-09-18",
    status: "CREADA",
    totalPrice: 320000,
    createdAt: "2026-09-10 09:00",
  },
];

const VALID_STATUS_TRANSITIONS: Record<ReservationStatus, ReservationStatus[]> = {
  CREADA: ["CONFIRMADA", "CANCELADA"],
  CONFIRMADA: ["CHECKIN_PENDIENTE", "CANCELADA"],
  CHECKIN_PENDIENTE: ["EN_ESTADÍA", "CANCELADA"],
  EN_ESTADÍA: ["CHECKOUT"],
  CHECKOUT: [],
  CANCELADA: [],
};

export function ReservationsPage() {
  const { role, user } = useUserRole();
  const [reservations, setReservations] = useState<Reservation[]>(INITIAL_RESERVATIONS);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Form state
  const [guestName, setGuestName] = useState(role === "Huésped" ? user.name : "");
  const [guestEmail, setGuestEmail] = useState(role === "Huésped" ? user.username : "");
  const [unitName, setUnitName] = useState("Cabaña Los Alerces #1");
  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");

  const canChangeStatus = role === "Admin" || role === "Recepcionista";
  const canCreate = role === "Admin" || role === "Recepcionista" || role === "Huésped";

  const handleStatusChange = (reservationId: string, currentStatus: ReservationStatus, newStatus: ReservationStatus) => {
    // Regla clave del caso: No se puede hacer check-in (EN_ESTADÍA) sin haber estado CONFIRMAR / CHECKIN_PENDIENTE
    if (newStatus === "EN_ESTADÍA" && currentStatus === "CREADA") {
      alert("Violación de regla: No se puede hacer check-in sin CONFIRMAR la reserva previamente.");
      return;
    }

    setReservations((prev) =>
      prev.map((r) => (r.id === reservationId ? { ...r, status: newStatus } : r))
    );
  };

  const handleCreateReservation = (e: React.FormEvent) => {
    e.preventDefault();
    if (!guestName || !checkIn || !checkOut) return;

    const newRes: Reservation = {
      id: `RES-${Math.floor(100 + Math.random() * 900)}`,
      guestName,
      guestEmail: guestEmail || "cliente@andesstay.cl",
      unitId: `U-${Math.floor(1 + Math.random() * 5)}`,
      unitName,
      checkInDate: checkIn,
      checkOutDate: checkOut,
      status: "CREADA",
      totalPrice: 150000,
      createdAt: new Date().toISOString().substring(0, 16).replace("T", " "),
    };

    setReservations([newRes, ...reservations]);
    setShowCreateModal(false);
    setCheckIn("");
    setCheckOut("");
  };

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Gestión de Reservas</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Módulo de reservas web y control de flujo de estadía.
          </p>
        </div>
        {canCreate && (
          <button
            onClick={() => setShowCreateModal(true)}
            style={{
              background: "#2563eb",
              color: "white",
              border: "none",
              padding: "0.6rem 1.25rem",
              borderRadius: "6px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            + Nueva Reserva
          </button>
        )}
      </div>

      {/* MODAL CREAR */}
      {showCreateModal && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 100,
          }}
        >
          <div
            style={{
              background: "white",
              padding: "2rem",
              borderRadius: "8px",
              maxWidth: "480px",
              width: "100%",
            }}
          >
            <h2 style={{ marginTop: 0 }}>Crear Reserva</h2>
            <form onSubmit={handleCreateReservation}>
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                  Nombre Huésped
                </label>
                <input
                  type="text"
                  required
                  value={guestName}
                  onChange={(e) => setGuestName(e.target.value)}
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                />
              </div>
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                  Correo Electrónico
                </label>
                <input
                  type="email"
                  required
                  value={guestEmail}
                  onChange={(e) => setGuestEmail(e.target.value)}
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                />
              </div>
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                  Unidad
                </label>
                <select
                  value={unitName}
                  onChange={(e) => setUnitName(e.target.value)}
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                >
                  <option value="Cabaña Los Alerces #1">Cabaña Los Alerces #1</option>
                  <option value="Habitación Vista Lago 201">Habitación Vista Lago 201</option>
                  <option value="Lodge Refugio Andino">Lodge Refugio Andino</option>
                </select>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1.5rem" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                    Check-in
                  </label>
                  <input
                    type="date"
                    required
                    value={checkIn}
                    onChange={(e) => setCheckIn(e.target.value)}
                    style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                    Check-out
                  </label>
                  <input
                    type="date"
                    required
                    value={checkOut}
                    onChange={(e) => setCheckOut(e.target.value)}
                    style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                  />
                </div>
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem" }}>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  style={{ padding: "0.5rem 1rem", border: "1px solid #ccc", background: "#f8fafc", borderRadius: "4px" }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  style={{ padding: "0.5rem 1rem", background: "#2563eb", color: "white", border: "none", borderRadius: "4px" }}
                >
                  Confirmar y Guardar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* TABLA DE RESERVAS */}
      <div style={{ background: "white", borderRadius: "8px", border: "1px solid #e2e8f0", overflowX: "auto" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left", fontSize: "0.9rem" }}>
          <thead>
            <tr style={{ background: "#f8fafc", borderBottom: "1px solid #e2e8f0", color: "#475569" }}>
              <th style={{ padding: "0.75rem 1rem" }}>Código</th>
              <th style={{ padding: "0.75rem 1rem" }}>Huésped</th>
              <th style={{ padding: "0.75rem 1rem" }}>Unidad</th>
              <th style={{ padding: "0.75rem 1rem" }}>Fechas</th>
              <th style={{ padding: "0.75rem 1rem" }}>Estado</th>
              <th style={{ padding: "0.75rem 1rem" }}>Acción de Estado</th>
            </tr>
          </thead>
          <tbody>
            {reservations.map((res) => (
              <tr key={res.id} style={{ borderBottom: "1px solid #f1f5f9" }}>
                <td style={{ padding: "0.75rem 1rem", fontWeight: 700 }}>{res.id}</td>
                <td style={{ padding: "0.75rem 1rem" }}>
                  <div>{res.guestName}</div>
                  <div style={{ fontSize: "0.75rem", color: "#64748b" }}>{res.guestEmail}</div>
                </td>
                <td style={{ padding: "0.75rem 1rem" }}>{res.unitName}</td>
                <td style={{ padding: "0.75rem 1rem", fontSize: "0.85rem" }}>
                  {res.checkInDate} &rarr; {res.checkOutDate}
                </td>
                <td style={{ padding: "0.75rem 1rem" }}>
                  <span style={statusBadgeStyle(res.status)}>{res.status}</span>
                </td>
                <td style={{ padding: "0.75rem 1rem" }}>
                  {canChangeStatus ? (
                    <select
                      value={res.status}
                      onChange={(e) => handleStatusChange(res.id, res.status, e.target.value as ReservationStatus)}
                      style={{ padding: "0.25rem 0.5rem", borderRadius: "4px", fontSize: "0.8rem", border: "1px solid #cbd5e1" }}
                    >
                      <option value={res.status} disabled>
                        Actual: {res.status}
                      </option>
                      {VALID_STATUS_TRANSITIONS[res.status].map((targetStatus) => (
                        <option key={targetStatus} value={targetStatus}>
                          &rarr; {targetStatus}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span style={{ fontSize: "0.8rem", color: "#94a3b8" }}>Solo lectura</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function statusBadgeStyle(status: ReservationStatus): React.CSSProperties {
  const map: Record<ReservationStatus, { bg: string; color: string }> = {
    CREADA: { bg: "#fef3c7", color: "#92400e" },
    CONFIRMADA: { bg: "#dbeafe", color: "#1e40af" },
    CHECKIN_PENDIENTE: { bg: "#e0e7ff", color: "#3730a3" },
    EN_ESTADÍA: { bg: "#dcfce7", color: "#166534" },
    CHECKOUT: { bg: "#f3f4f6", color: "#374151" },
    CANCELADA: { bg: "#fee2e2", color: "#991b1b" },
  };
  const c = map[status] || { bg: "#eee", color: "#333" };
  return {
    background: c.bg,
    color: c.color,
    padding: "0.2rem 0.5rem",
    borderRadius: "12px",
    fontWeight: 700,
    fontSize: "0.75rem",
  };
}
