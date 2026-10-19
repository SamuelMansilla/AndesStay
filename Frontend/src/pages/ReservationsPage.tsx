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
    <div className="page">
      <div className="page-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
        <div>
          <h1 className="page-title">Gestión de Reservas</h1>
          <p className="page-subtitle">
            Módulo de reservas web sincronizado en tiempo real con microservicios.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem" }}>
          <button
            onClick={refreshReservations}
            disabled={loading}
            className="btn btn-secondary"
          >
            {loading ? "Cargando..." : "↻ Refrescar"}
          </button>
          {canCreate && (
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn btn-primary"
            >
              + Nueva Reserva
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="alert alert--error" style={{ marginBottom: "1.25rem" }}>
          ⚠️ {error}
        </div>
      )}

      {/* FILTROS */}
      <div style={{ display: "flex", gap: "1rem", marginBottom: "1.25rem", alignItems: "center", flexWrap: "wrap" }}>
        <label>Filtrar por Estado:</label>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
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
            background: "rgba(15, 23, 42, 0.55)",
            backdropFilter: "blur(4px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 100,
            padding: "1rem",
          }}
        >
          <div
            className="card"
            style={{
              borderRadius: "16px",
              maxWidth: "520px",
              width: "100%",
              padding: 0,
              overflow: "hidden",
              maxHeight: "90vh",
              overflowY: "auto",
              boxShadow: "var(--as-shadow-lg)",
            }}
          >
            <div style={{ height: "4px", background: "linear-gradient(90deg, #0284c7, #0d9488)" }} />
            <div style={{ padding: "1.75rem" }}>
              <h2 className="section-title">Crear Reserva</h2>
              <form onSubmit={handleCreateReservation}>
                <div style={{ marginBottom: "1rem" }}>
                  <label style={{ display: "block", marginBottom: "0.3rem" }}>
                    Nombre Huésped
                  </label>
                  <input
                    type="text"
                    value={guestName}
                    onChange={(e) => setGuestName(e.target.value)}
                    placeholder="Nombre completo"
                    style={{ width: "100%" }}
                  />
                </div>
                <div style={{ marginBottom: "1rem" }}>
                  <label style={{ display: "block", marginBottom: "0.3rem" }}>
                    Correo Electrónico (Huésped) *
                  </label>
                  <input
                    type="email"
                    required
                    value={guestEmail}
                    onChange={(e) => setGuestEmail(e.target.value)}
                    placeholder="correo@ejemplo.com"
                    style={{ width: "100%" }}
                  />
                </div>
                <div style={{ marginBottom: "1rem" }}>
                  <label style={{ display: "block", marginBottom: "0.3rem" }}>
                    Unidad de Hospedaje *
                  </label>
                  <select
                    value={selectedUnitId}
                    onChange={(e) => setSelectedUnitId(e.target.value)}
                    style={{ width: "100%" }}
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
                    <label style={{ display: "block", marginBottom: "0.3rem" }}>
                      Check-in *
                    </label>
                    <input
                      type="date"
                      required
                      value={checkIn}
                      onChange={(e) => setCheckIn(e.target.value)}
                      style={{ width: "100%" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", marginBottom: "0.3rem" }}>
                      Check-out *
                    </label>
                    <input
                      type="date"
                      required
                      value={checkOut}
                      onChange={(e) => setCheckOut(e.target.value)}
                      style={{ width: "100%" }}
                    />
                  </div>
                </div>
                <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem" }}>
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="btn btn-secondary"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="btn btn-primary"
                  >
                    {isSubmitting ? "Guardando..." : "Confirmar y Guardar"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* TABLA DE RESERVAS */}
      {loading ? (
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            gap: "0.85rem",
            padding: "3.5rem 0",
          }}
        >
          <div className="spinner" />
          <span style={{ fontSize: "0.9rem", color: "#64748b" }}>
            Cargando reservas desde la API...
          </span>
        </div>
      ) : filteredReservations.length === 0 ? (
        <div className="table-card card">
          <div className="empty-state">
            <div className="empty-icon">📅</div>
            <div>No se encontraron reservas para el criterio seleccionado.</div>
          </div>
        </div>
      ) : (
        <div className="table-card card" style={{ overflowX: "auto" }}>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Huésped</th>
                <th>Unidad</th>
                <th>Fechas</th>
                <th>Estado</th>
                <th>Acción de Estado</th>
              </tr>
            </thead>
            <tbody>
              {filteredReservations.map((res) => (
                <tr key={res.id}>
                  <td style={{ fontWeight: 700 }}>#{res.id}</td>
                  <td>
                    <div>{res.guestName || "Huésped"}</div>
                    <div style={{ fontSize: "0.75rem", color: "#64748b" }}>{res.guestEmail}</div>
                  </td>
                  <td>{res.unitName || getUnitName(res.unitId)}</td>
                  <td style={{ fontSize: "0.85rem" }}>
                    {res.checkInDate} &rarr; {res.checkOutDate}
                  </td>
                  <td>
                    <span className={statusBadgeClass(res.status)}>{res.status}</span>
                  </td>
                  <td>
                    {canChangeStatus ? (
                      <select
                        value={res.status}
                        onChange={(e) =>
                          handleStatusChange(res.id, res.status, e.target.value as ReservationStatus)
                        }
                        style={{ fontSize: "0.8rem", padding: "0.3rem 0.5rem" }}
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
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function statusBadgeClass(status: ReservationStatus): string {
  const map: Record<ReservationStatus, string> = {
    CREADA: "badge badge--amber",
    CONFIRMADA: "badge badge--green",
    CHECKIN_PENDIENTE: "badge badge--amber",
    EN_ESTADIA: "badge badge--blue",
    EN_ESTADÍA: "badge badge--blue",
    CHECKOUT: "badge badge--green",
    CANCELADA: "badge badge--red",
  };
  return map[status] || "badge badge--slate";
}
