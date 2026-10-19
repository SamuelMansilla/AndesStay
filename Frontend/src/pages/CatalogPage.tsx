import { useState, useEffect } from "react";
import { useUserRole } from "../hooks/useUserRole";
import type { Unit } from "../types";
import { catalogService } from "../api/catalogService";

export function CatalogPage() {
  const { role } = useUserRole();
  const [units, setUnits] = useState<Unit[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [editingUnit, setEditingUnit] = useState<Unit | null>(null);
  const [editingPrice, setEditingPrice] = useState<number>(0);

  // Modal para crear nueva unidad
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newName, setNewName] = useState("");
  const [newType, setNewType] = useState<"Habitación" | "Cabaña" | "Lodge">("Cabaña");
  const [newCapacity, setNewCapacity] = useState<number>(4);
  const [newPrice, setNewPrice] = useState<number>(75000);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const canEdit = role === "Admin";

  const refreshUnits = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await catalogService.getUnits();
      setUnits(data);
    } catch (err: unknown) {
      console.error("Error al cargar unidades del catálogo:", err);
      setError("No se pudieron cargar las unidades desde el servidor. Comprueba la conexión con el BFF.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    catalogService
      .getUnits()
      .then((data) => {
        if (isMounted) {
          setUnits(data);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          console.error("Error al cargar unidades del catálogo:", err);
          setError("No se pudieron cargar las unidades desde el servidor. Comprueba la conexión con el BFF.");
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const handleToggleAvailability = async (unit: Unit) => {
    if (!canEdit) return;
    const newAvailable = !unit.available;

    try {
      await catalogService.updateUnit(unit.id, {
        ...unit,
        available: newAvailable,
      });

      setUnits((prev) =>
        prev.map((u) => (u.id === unit.id ? { ...u, available: newAvailable } : u))
      );
    } catch (err: unknown) {
      console.error("Error al actualizar disponibilidad:", err);
      alert("Error al actualizar disponibilidad en la API.");
    }
  };

  const handleStartEditPrice = (unit: Unit) => {
    setEditingUnit(unit);
    setEditingPrice(unit.price ?? unit.pricePerNight ?? 0);
  };

  const handleUpdatePrice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUnit) return;

    try {
      await catalogService.updateUnit(editingUnit.id, {
        ...editingUnit,
        price: editingPrice,
        pricePerNight: editingPrice,
      });

      setUnits((prev) =>
        prev.map((u) =>
          u.id === editingUnit.id
            ? { ...u, price: editingPrice, pricePerNight: editingPrice }
            : u
        )
      );
      setEditingUnit(null);
    } catch (err: unknown) {
      console.error("Error al actualizar tarifa:", err);
      alert("Error al actualizar tarifa en la API.");
    }
  };

  const handleCreateUnit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName || newPrice <= 0 || newCapacity <= 0) return;

    setIsSubmitting(true);
    try {
      const created = await catalogService.createUnit({
        name: newName,
        type: newType,
        capacity: newCapacity,
        price: newPrice,
        pricePerNight: newPrice,
        available: true,
        amenities: ["Wi-Fi", "Calefacción"],
      });

      setUnits((prev) => [...prev, created]);
      setShowCreateModal(false);
      setNewName("");
      setNewCapacity(4);
      setNewPrice(75000);
      alert("Unidad registrada en el catálogo con éxito.");
    } catch (err: unknown) {
      console.error("Error al crear unidad:", err);
      alert("Error al registrar unidad en la API.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const accentByType: Record<string, string> = {
    "Habitación": "accent-top--blue",
    "Cabaña": "accent-top--teal",
    "Lodge": "accent-top--green",
  };

  return (
    <div className="page">
      <div className="page-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
        <div>
          <h1 className="page-title">Catálogo de Unidades</h1>
          <p className="page-subtitle">
            Administración de habitaciones, cabañas, cupos, tarifas y disponibilidad en tiempo real.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem" }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={refreshUnits}
            disabled={loading}
          >
            {loading ? "Cargando..." : "↻ Refrescar"}
          </button>
          {canEdit && (
            <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
              + Nueva Unidad
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="alert alert--error" style={{ marginBottom: "1.5rem" }}>
          ⚠️ {error}
        </div>
      )}

      {/* MODAL CREAR */}
      {showCreateModal && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(15,23,42,0.55)",
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
              position: "relative",
              overflow: "hidden",
              boxShadow: "0 20px 45px -10px rgba(15,23,42,0.35)",
            }}
          >
            <div style={{ position: "absolute", inset: "0 0 auto 0", height: "4px", background: "linear-gradient(90deg, #0284c7, #0d9488)" }} />
            <h2 className="section-title" style={{ marginTop: "0.5rem", marginBottom: "1.25rem" }}>Añadir Unidad al Catálogo</h2>
            <form onSubmit={handleCreateUnit}>
              <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem", marginBottom: "1rem" }}>
                <label htmlFor="new-unit-name">Nombre de Unidad *</label>
                <input
                  id="new-unit-name"
                  type="text"
                  required
                  placeholder="Ej: Cabaña Los Coihues #3"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                />
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1rem" }}>
                <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem" }}>
                  <label htmlFor="new-unit-type">Tipo *</label>
                  <select
                    id="new-unit-type"
                    value={newType}
                    onChange={(e) => setNewType(e.target.value as "Habitación" | "Cabaña" | "Lodge")}
                  >
                    <option value="Habitación">Habitación</option>
                    <option value="Cabaña">Cabaña</option>
                    <option value="Lodge">Lodge</option>
                  </select>
                </div>
                <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem" }}>
                  <label htmlFor="new-unit-capacity">Capacidad (Personas) *</label>
                  <input
                    id="new-unit-capacity"
                    type="number"
                    min={1}
                    max={20}
                    required
                    value={newCapacity}
                    onChange={(e) => setNewCapacity(Number(e.target.value))}
                  />
                </div>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem", marginBottom: "1.5rem" }}>
                <label htmlFor="new-unit-price">Tarifa por Noche ($CLP) *</label>
                <input
                  id="new-unit-price"
                  type="number"
                  min={10000}
                  step={1000}
                  required
                  value={newPrice}
                  onChange={(e) => setNewPrice(Number(e.target.value))}
                />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem" }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                  {isSubmitting ? "Guardando..." : "Crear Unidad"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* FORMULARIO EDITAR TARIFA */}
      {editingUnit && (
        <div
          className="card accent-top accent-top--amber"
          style={{ marginBottom: "1.5rem" }}
        >
          <h3 className="card-title" style={{ fontSize: "0.95rem", marginBottom: "0.9rem" }}>
            Modificar Tarifa: {editingUnit.name}
          </h3>
          <form onSubmit={handleUpdatePrice} style={{ display: "flex", gap: "0.75rem", alignItems: "center", flexWrap: "wrap" }}>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem" }}>
              <label htmlFor="edit-price">Tarifa por noche ($CLP)</label>
              <input
                id="edit-price"
                type="number"
                value={editingPrice}
                onChange={(e) => setEditingPrice(Number(e.target.value))}
                style={{ maxWidth: "180px" }}
              />
            </div>
            <div style={{ display: "flex", gap: "0.5rem", alignSelf: "flex-end", paddingBottom: "2px" }}>
              <button type="submit" className="btn btn-primary btn-sm">
                Guardar Tarifa
              </button>
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditingUnit(null)}>
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}

      {loading ? (
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "1rem", padding: "3rem 1rem", color: "#64748b", fontSize: "0.9rem" }}>
          <div className="spinner" />
          Cargando catálogo de unidades desde el backend...
        </div>
      ) : units.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <div className="empty-icon">🏕️</div>
            <div style={{ fontWeight: 600, fontSize: "1rem", color: "#475569" }}>No hay unidades registradas en el catálogo</div>
            <div style={{ fontSize: "0.85rem" }}>Crea tu primera unidad para empezar a gestionar el catálogo.</div>
          </div>
        </div>
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
            gap: "1.5rem",
          }}
        >
          {units.map((unit) => {
            const price = unit.price ?? unit.pricePerNight ?? 0;
            return (
              <div
                key={unit.id}
                className={`card card--hover accent-top ${accentByType[unit.type] ?? "accent-top--blue"}`}
                style={{
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "space-between",
                }}
              >
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "0.5rem", marginBottom: "0.5rem", paddingTop: "2px" }}>
                    <h3 style={{ margin: 0, fontSize: "1.1rem", color: "#1e293b", letterSpacing: "-0.2px" }}>{unit.name}</h3>
                    {unit.available ? (
                      <span className="badge badge--green">Disponible</span>
                    ) : (
                      <span className="badge badge--red">No disponible</span>
                    )}
                  </div>

                  <div style={{ fontSize: "0.85rem", color: "#64748b", marginBottom: "0.75rem" }}>
                    Tipo: <strong>{unit.type}</strong> &bull; Capacidad: <strong>{unit.capacity} personas</strong>
                  </div>

                  <div className="kpi-value" style={{ fontSize: "1.35rem", marginBottom: "0.75rem" }}>
                    ${Number(price).toLocaleString("es-CL")}{" "}
                    <span style={{ fontSize: "0.8rem", fontWeight: 400, color: "#64748b" }}>/ noche</span>
                  </div>

                  {unit.amenities && unit.amenities.length > 0 && (
                    <div style={{ fontSize: "0.8rem", color: "#475569", marginBottom: "1rem" }}>
                      <strong>Comodidades:</strong>
                      <ul style={{ margin: "0.25rem 0 0 0", paddingLeft: "1.2rem" }}>
                        {unit.amenities.map((a) => (
                          <li key={a}>{a}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>

                {canEdit && (
                  <div style={{ display: "flex", gap: "0.5rem", paddingTop: "0.75rem", borderTop: "1px solid #f1f5f9" }}>
                    <button
                      className="btn btn-secondary btn-sm"
                      style={{ flex: 1 }}
                      onClick={() => handleStartEditPrice(unit)}
                    >
                      Editar Tarifa
                    </button>
                    {unit.available ? (
                      <button
                        className="btn btn-danger btn-sm"
                        style={{ flex: 1 }}
                        onClick={() => handleToggleAvailability(unit)}
                      >
                        Marcar No Disp.
                      </button>
                    ) : (
                      <button
                        className="btn btn-primary btn-sm"
                        style={{ flex: 1 }}
                        onClick={() => handleToggleAvailability(unit)}
                      >
                        Habilitar
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
