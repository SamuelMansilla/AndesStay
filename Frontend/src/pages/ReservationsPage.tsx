import { useState, useEffect } from "react";
import { useUserRole } from "../hooks/useUserRole";
import type { Reservation, ReservationStatus, Unit } from "../types";
import { reservationsService } from "../api/reservationsService";
import { catalogService } from "../api/catalogService";

const VALID_STATUS_TRANSITIONS: Record<ReservationStatus, ReservationStatus[]> = {
  CREADA: ["CONFIRMADA", "CANCELADA"],
  CONFIRMADA: ["CHECKIN_PENDIENTE", "CANCELADA"],
  CHECKIN_PENDIENTE: ["EN_ESTADIA", "CANCELADA"],
  EN_ESTADIA: ["CHECKOUT"],
  EN_ESTADÍA: ["CHECKOUT"],
  CHECKOUT: [],
  CANCELADA: [],
};

export function ReservationsPage() {
  const { role, user } = useUserRole();
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("TODOS");

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form state
  const [guestName, setGuestName] = useState(role === "Huésped" ? user.name : "");
  const [guestEmail, setGuestEmail] = useState(role === "Huésped" ? user.username : "");
  const [selectedUnitId, setSelectedUnitId] = useState<string | number>("");
  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");

  const canChangeStatus = role === "Admin" || role === "Recepcionista";
  const canCreate = role === "Admin" || role === "Recepcionista" || role === "Huésped";

  const refreshReservations = async () => {
    setLoading(true);
    setError(null);
    try {
      const [resList, unitList] = await Promise.all([
        reservationsService.getReservations(),
        catalogService.getUnits().catch(() => [] as Unit[]),
      ]);

      setUnits(unitList);
      if (unitList.length > 0 && !selectedUnitId) {
        setSelectedUnitId(unitList[0].id);
      }

      if (role === "Huésped") {
        const myEmail = user.username?.toLowerCase();
        setReservations(resList.filter((r) => r.guestEmail?.toLowerCase() === myEmail));
      } else {
        setReservations(resList);
      }
    } catch (err: unknown) {
      console.error("Error al cargar reservas:", err);
      setError("No se pudieron cargar las reservas desde la API. Mostrando datos disponibles.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      reservationsService.getReservations(),
      catalogService.getUnits().catch(() => [] as Unit[]),
    ])
      .then(([resList, unitList]) => {
        if (isMounted) {
          setUnits(unitList);
          if (unitList.length > 0 && !selectedUnitId) {
            setSelectedUnitId(unitList[0].id);
          }
          if (role === "Huésped") {
            const myEmail = user.username?.toLowerCase();
            setReservations(resList.filter((r) => r.guestEmail?.toLowerCase() === myEmail));
          } else {
            setReservations(resList);
          }
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          console.error("Error al cargar reservas:", err);
          setError("No se pudieron cargar las reservas desde la API. Mostrando datos disponibles.");
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [role, user.username, selectedUnitId]);

  const handleStatusChange = async (
    reservationId: string | number,
    currentStatus: ReservationStatus,
    newStatus: ReservationStatus
  ) => {
    // Regla clave del caso: No se puede hacer check-in (EN_ESTADIA) sin haber estado CONFIRMADA o CHECKIN_PENDIENTE
    if ((newStatus === "EN_ESTADIA" || newStatus === "EN_ESTADÍA") && currentStatus === "CREADA") {
      alert("Violación de regla: No se puede hacer check-in sin CONFIRMAR la reserva previamente.");
      return;
    }

    try {
      await reservationsService.updateReservationStatus(reservationId, newStatus);
      setReservations((prev) =>
        prev.map((r) => (r.id === reservationId ? { ...r, status: newStatus } : r))
      );
    } catch (err: unknown) {
      console.error("Error al actualizar estado en API:", err);
      alert("No se pudo actualizar el estado en el backend. Revisa la consola o permisos.");
    }
  };

  const handleCreateReservation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!guestEmail || !checkIn || !checkOut || !selectedUnitId) {
      alert("Por favor completa todos los campos obligatorios.");
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        guestEmail,
        unitId: Number(selectedUnitId),
        checkInDate: checkIn,
        checkOutDate: checkOut,
      };

      const created = await reservationsService.createReservation(payload);
      setReservations((prev) => [created, ...prev]);
      setShowCreateModal(false);
      setCheckIn("");
      setCheckOut("");
      alert("¡Reserva creada exitosamente!");
    } catch (err: unknown) {
      console.error("Error al crear reserva en API:", err);
      alert("Error al registrar la reserva en el servidor. Verifica las fechas y la unidad.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredReservations = reservations.filter((r) => {
    if (statusFilter !== "TODOS") {
      if (statusFilter === "EN_ESTADIA" || statusFilter === "EN_ESTADÍA") {
        return r.status === "EN_ESTADIA" || r.status === "EN_ESTADÍA";
      }
      return r.status === statusFilter;
    }
    return true;
  });

  const getUnitName = (unitId: string | number) => {
    const found = units.find((u) => String(u.id) === String(unitId));
    return found ? found.name : `Unidad #${unitId}`;
  };

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Gestión de Reservas</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Módulo de reservas web sincronizado en tiempo real con microservicios.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem" }}>
          <button
            onClick={refreshReservations}
            disabled={loading}
            style={{
              background: "#f1f5f9",
              color: "#334155",
              border: "1px solid #cbd5e1",
              padding: "0.6rem 1rem",
              borderRadius: "6px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            {loading ? "Cargando..." : "↻ Refrescar"}
          </button>
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
      </div>

      {error && (
        <div
          style={{
            padding: "0.75rem 1rem",
            marginBottom: "1rem",
            background: "#fffbeb",
            color: "#b45309",
            borderRadius: "6px",
            border: "1px solid #fde68a",
            fontSize: "0.875rem",
          }}
        >
          ⚠️ {error}
        </div>
      )}

      {/* FILTROS */}
      <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem", alignItems: "center" }}>
        <label style={{ fontSize: "0.85rem", color: "#475569", fontWeight: 600 }}>Filtrar por Estado:</label>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          style={{ padding: "0.4rem 0.75rem", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "0.85rem" }}
        >
          <option value="TODOS">Todos los estados</option>
          <option value="CREADA">CREADA</option>
          <option value="CONFIRMADA">CONFIRMADA</option>
          <option value="CHECKIN_PENDIENTE">CHECKIN_PENDIENTE</option>
          <option value="EN_ESTADÍA">EN_ESTADÍA</option>
          <option value="CHECKOUT">CHECKOUT</option>
          <option value="CANCELADA">CANCELADA</option>
        </select>
        <span style={{ fontSize: "0.85rem", color: "#64748b" }}>
          Mostrando {filteredReservations.length} reservas
        </span>
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
                  value={guestName}
                  onChange={(e) => setGuestName(e.target.value)}
                  placeholder="Nombre completo"
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                />
              </div>
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                  Correo Electrónico (Huésped) *
                </label>
                <input
                  type="email"
                  required
                  value={guestEmail}
                  onChange={(e) => setGuestEmail(e.target.value)}
                  placeholder="correo@ejemplo.com"
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                />
              </div>
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                  Unidad de Hospedaje *
                </label>
                <select
                  value={selectedUnitId}
                  onChange={(e) => setSelectedUnitId(e.target.value)}
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                  required
                >
                  {units.length === 0 ? (
                    <option value="">Cargando unidades del catálogo...</option>
                  ) : (
                    units.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.name} ({u.type}) - ${Number(u.price || u.pricePerNight || 0).toLocaleString()} / noche
                      </option>
                    ))
                  )}
                </select>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1.5rem" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                    Check-in *
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
                    Check-out *
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
                  disabled={isSubmitting}
                  style={{
                    padding: "0.5rem 1rem",
                    background: "#2563eb",
                    color: "white",
                    border: "none",
                    borderRadius: "4px",
                    cursor: isSubmitting ? "not-allowed" : "pointer",
                  }}
                >
                  {isSubmitting ? "Guardando..." : "Confirmar y Guardar"}
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
              <th style={{ padding: "0.75rem 1rem" }}>ID</th>
              <th style={{ padding: "0.75rem 1rem" }}>Huésped</th>
              <th style={{ padding: "0.75rem 1rem" }}>Unidad</th>
              <th style={{ padding: "0.75rem 1rem" }}>Fechas</th>
              <th style={{ padding: "0.75rem 1rem" }}>Estado</th>
              <th style={{ padding: "0.75rem 1rem" }}>Acción de Estado</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} style={{ padding: "2rem", textAlign: "center", color: "#64748b" }}>
                  Cargando reservas desde la API...
                </td>
              </tr>
            ) : filteredReservations.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: "2rem", textAlign: "center", color: "#94a3b8" }}>
                  No se encontraron reservas para el criterio seleccionado.
                </td>
              </tr>
            ) : (
              filteredReservations.map((res) => (
                <tr key={res.id} style={{ borderBottom: "1px solid #f1f5f9" }}>
                  <td style={{ padding: "0.75rem 1rem", fontWeight: 700 }}>#{res.id}</td>
                  <td style={{ padding: "0.75rem 1rem" }}>
                    <div>{res.guestName || "Huésped"}</div>
                    <div style={{ fontSize: "0.75rem", color: "#64748b" }}>{res.guestEmail}</div>
                  </td>
                  <td style={{ padding: "0.75rem 1rem" }}>{res.unitName || getUnitName(res.unitId)}</td>
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
                        onChange={(e) =>
                          handleStatusChange(res.id, res.status, e.target.value as ReservationStatus)
                        }
                        style={{
                          padding: "0.25rem 0.5rem",
                          borderRadius: "4px",
                          fontSize: "0.8rem",
                          border: "1px solid #cbd5e1",
                        }}
                      >
                        <option value={res.status} disabled>
                          Actual: {res.status}
                        </option>
                        {VALID_STATUS_TRANSITIONS[res.status]?.map((targetStatus) => (
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
              ))
            )}
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
    EN_ESTADIA: { bg: "#dcfce7", color: "#166534" },
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