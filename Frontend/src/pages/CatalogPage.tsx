import { useState } from "react";
import { useUserRole } from "../hooks/useUserRole";
import type { Unit } from "../types";

const INITIAL_UNITS: Unit[] = [
  {
    id: "U-1",
    name: "Cabaña Los Alerces #1",
    type: "Cabaña",
    capacity: 6,
    pricePerNight: 80000,
    available: true,
    amenities: ["Tinaja caliente", "Parrilla", "Wi-Fi Fibra", "Cocina equipada"],
  },
  {
    id: "U-2",
    name: "Habitación Vista Lago 201",
    type: "Habitación",
    capacity: 2,
    pricePerNight: 45000,
    available: false,
    amenities: ["Cama King", "Baño en suite", "Desayuno incluido"],
  },
  {
    id: "U-3",
    name: "Lodge Refugio Andino",
    type: "Lodge",
    capacity: 4,
    pricePerNight: 95000,
    available: true,
    amenities: ["Chimenea", "Vista a volcanes", "Estacionamiento"],
  },
  {
    id: "U-4",
    name: "Cabaña Los Coihues #2",
    type: "Cabaña",
    capacity: 5,
    pricePerNight: 75000,
    available: true,
    amenities: ["Parrilla", "Calefacción a leña", "Wi-Fi"],
  },
];

export function CatalogPage() {
  const { role } = useUserRole();
  const [units, setUnits] = useState<Unit[]>(INITIAL_UNITS);
  const [editingUnit, setEditingUnit] = useState<Unit | null>(null);

  const canEdit = role === "Admin";

  const handleToggleAvailability = (unitId: string) => {
    if (!canEdit) return;
    setUnits((prev) =>
      prev.map((u) => (u.id === unitId ? { ...u, available: !u.available } : u))
    );
  };

  const handleUpdatePrice = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUnit) return;

    setUnits((prev) =>
      prev.map((u) => (u.id === editingUnit.id ? editingUnit : u))
    );
    setEditingUnit(null);
  };

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Catálogo de Unidades</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Administración de habitaciones, cabañas, cupos, tarifas y disponibilidad.
          </p>
        </div>
      </div>

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
                value={editingUnit.pricePerNight}
                onChange={(e) =>
                  setEditingUnit({ ...editingUnit, pricePerNight: Number(e.target.value) })
                }
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

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
          gap: "1.5rem",
        }}
      >
        {units.map((unit) => (
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
                ${unit.pricePerNight.toLocaleString("es-CL")}{" "}
                <span style={{ fontSize: "0.8rem", fontWeight: 400, color: "#64748b" }}>/ noche</span>
              </div>

              <div style={{ fontSize: "0.8rem", color: "#475569", marginBottom: "1rem" }}>
                <strong>Comodidades:</strong>
                <ul style={{ margin: "0.25rem 0 0 0", paddingLeft: "1.2rem" }}>
                  {unit.amenities.map((a) => (
                    <li key={a}>{a}</li>
                  ))}
                </ul>
              </div>
            </div>

            {canEdit && (
              <div style={{ display: "flex", gap: "0.5rem", paddingTop: "0.75rem", borderTop: "1px solid #f1f5f9" }}>
                <button
                  onClick={() => setEditingUnit(unit)}
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
                  onClick={() => handleToggleAvailability(unit.id)}
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
        ))}
      </div>
    </div>
  );
}
