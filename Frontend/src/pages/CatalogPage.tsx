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

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Catálogo de Unidades</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Administración de habitaciones, cabañas, cupos, tarifas y disponibilidad en tiempo real.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem" }}>
          <button
            onClick={refreshUnits}
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
          {canEdit && (
            <button
              onClick={() => setShowCreateModal(true)}
              style={{
                background: "#059669",
                color: "white",
                border: "none",
                padding: "0.6rem 1.25rem",
                borderRadius: "6px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              + Nueva Unidad
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
            <h2 style={{ marginTop: 0 }}>Añadir Unidad al Catálogo</h2>
            <form onSubmit={handleCreateUnit}>
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                  Nombre de Unidad *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej: Cabaña Los Coihues #3"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                />
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1rem" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                    Tipo *
                  </label>
                  <select
                    value={newType}
                    onChange={(e) => setNewType(e.target.value as "Habitación" | "Cabaña" | "Lodge")}
                    style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                  >
                    <option value="Habitación">Habitación</option>
                    <option value="Cabaña">Cabaña</option>
                    <option value="Lodge">Lodge</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                    Capacidad (Personas) *
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={20}
                    required
                    value={newCapacity}
                    onChange={(e) => setNewCapacity(Number(e.target.value))}
                    style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                  />
                </div>
              </div>
              <div style={{ marginBottom: "1.5rem" }}>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.25rem" }}>
                  Tarifa por Noche ($CLP) *
                </label>
                <input
                  type="number"
                  min={10000}
                  step={1000}
                  required
                  value={newPrice}
                  onChange={(e) => setNewPrice(Number(e.target.value))}
                  style={{ width: "100%", padding: "0.5rem", borderRadius: "4px", border: "1px solid #ccc" }}
                />
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
                  style={{ padding: "0.5rem 1rem", background: "#059669", color: "white", border: "none", borderRadius: "4px" }}
                >
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
          style={{
            background: "#f8fafc",
            border: "1px solid #cbd5e1",
            padding: "1rem",
            borderRadius: "8px",
            marginBottom: "1.5rem",
          }}
        >
          <h3 style={{ marginTop: 0 }}>Modificar Tarifa: {editingUnit.name}</h3>
          <form onSubmit={handleUpdatePrice} style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
            <label style={{ fontSize: "0.9rem" }}>
              Tarifa por noche ($CLP):
              <input
                type="number"
                value={editingPrice}
                onChange={(e) => setEditingPrice(Number(e.target.value))}
                style={{ marginLeft: "0.5rem", padding: "0.35rem", borderRadius: "4px", border: "1px solid #ccc" }}
              />
            </label>
            <button
              type="submit"
              style={{ background: "#16a34a", color: "white", border: "none", padding: "0.4rem 0.8rem", borderRadius: "4px", cursor: "pointer" }}
            >
              Guardar Tarifa
            </button>
            <button
              type="button"
              onClick={() => setEditingUnit(null)}
              style={{ background: "#94a3b8", color: "white", border: "none", padding: "0.4rem 0.8rem", borderRadius: "4px", cursor: "pointer" }}
            >
              Cancelar
            </button>
          </form>
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: "center", padding: "3rem", color: "#64748b" }}>
          Cargando catálogo de unidades desde el backend...
        </div>
      ) : units.length === 0 ? (
        <div style={{ textAlign: "center", padding: "3rem", color: "#94a3b8" }}>
          No hay unidades registradas en el catálogo.
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
                style={{
                  background: "white",
                  border: "1px solid #e2e8f0",
                  borderRadius: "8px",
                  padding: "1.25rem",
                  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "space-between",
                }}
              >
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "0.5rem" }}>
                    <h3 style={{ margin: 0, fontSize: "1.1rem", color: "#1e293b" }}>{unit.name}</h3>
                    <span
                      style={{
                        background: unit.available ? "#dcfce7" : "#fee2e2",
                        color: unit.available ? "#15803d" : "#991b1b",
                        fontSize: "0.75rem",
                        fontWeight: 700,
                        padding: "0.2rem 0.5rem",
                        borderRadius: "12px",
                      }}
                    >
                      {unit.available ? "Disponible" : "Ocupada / No disp."}
                    </span>
                  </div>

                  <div style={{ fontSize: "0.85rem", color: "#64748b", marginBottom: "0.75rem" }}>
                    Tipo: <strong>{unit.type}</strong> &bull; Capacidad: <strong>{unit.capacity} personas</strong>
                  </div>

                  <div style={{ fontSize: "1.25rem", fontWeight: 800, color: "#0f172a", marginBottom: "0.75rem" }}>
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
                      onClick={() => handleStartEditPrice(unit)}
                      style={{
                        flex: 1,
                        padding: "0.4rem",
                        fontSize: "0.8rem",
                        background: "#f1f5f9",
                        border: "1px solid #cbd5e1",
                        borderRadius: "4px",
                        cursor: "pointer",
                      }}
                    >
                      Editar Tarifa
                    </button>
                    <button
                      onClick={() => handleToggleAvailability(unit)}
                      style={{
                        flex: 1,
                        padding: "0.4rem",
                        fontSize: "0.8rem",
                        background: unit.available ? "#fef2f2" : "#f0fdf4",
                        color: unit.available ? "#dc2626" : "#16a34a",
                        border: `1px solid ${unit.available ? "#fecaca" : "#bbf7d0"}`,
                        borderRadius: "4px",
                        cursor: "pointer",
                      }}
                    >
                      {unit.available ? "Marcar No Disp." : "Habilitar"}
                    </button>
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
