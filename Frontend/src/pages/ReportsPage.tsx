import { useState, useEffect } from "react";
import { reportService } from "../api/reportService";
import type { KpiSummary, TopUnitItem } from "../types";

export function ReportsPage() {
  const [range, setRange] = useState<"last24h" | "last7d" | "last30d">("last24h");
  const [kpis, setKpis] = useState<KpiSummary | null>(null);
  const [topUnits, setTopUnits] = useState<TopUnitItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const refreshReportData = async (selectedRange: string) => {
    setLoading(true);
    setError(null);
    try {
      const [kpiData, unitsData] = await Promise.all([
        reportService.getKpis(selectedRange),
        reportService.getTopUnits(selectedRange).catch(() => [] as TopUnitItem[]),
      ]);
      setKpis(kpiData);
      setTopUnits(unitsData);
    } catch (err: unknown) {
      console.error("Error al cargar reportes desde la API:", err);
      setError("No se pudieron obtener métricas desde el microservicio de reportería. Mostrando valores por defecto.");
      setKpis({
        reservationsToday: 0,
        activeOccupancyRate: 0,
        averageCycleHours: 0,
        totalReservations: 0,
      });
      setTopUnits([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      reportService.getKpis(range),
      reportService.getTopUnits(range).catch(() => [] as TopUnitItem[]),
    ])
      .then(([kpiData, unitsData]) => {
        if (isMounted) {
          setKpis(kpiData);
          setTopUnits(unitsData);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          console.error("Error al cargar reportes desde la API:", err);
          setError("No se pudieron obtener métricas desde el microservicio de reportería. Mostrando valores por defecto.");
          setKpis({
            reservationsToday: 0,
            activeOccupancyRate: 0,
            averageCycleHours: 0,
            totalReservations: 0,
          });
          setTopUnits([]);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [range]);

  const hourlyData = [
    { hour: "08:00", count: Math.max(1, Math.round((kpis?.reservationsToday || 4) * 0.15)) },
    { hour: "10:00", count: Math.max(2, Math.round((kpis?.reservationsToday || 4) * 0.25)) },
    { hour: "12:00", count: Math.max(3, Math.round((kpis?.reservationsToday || 4) * 0.40)) },
    { hour: "14:00", count: Math.max(2, Math.round((kpis?.reservationsToday || 4) * 0.30)) },
    { hour: "16:00", count: Math.max(4, Math.round((kpis?.reservationsToday || 4) * 0.50)) },
    { hour: "18:00", count: Math.max(3, Math.round((kpis?.reservationsToday || 4) * 0.35)) },
    { hour: "20:00", count: Math.max(1, Math.round((kpis?.reservationsToday || 4) * 0.20)) },
  ];

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Reportería y Analítica (KPIs)</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Panel de indicadores en tiempo real procesados vía Kafka / Microservicio ms-andesstay-report.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
          <button
            onClick={() => refreshReportData(range)}
            disabled={loading}
            style={{
              background: "#f1f5f9",
              color: "#334155",
              border: "1px solid #cbd5e1",
              padding: "0.4rem 0.8rem",
              borderRadius: "6px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            {loading ? "Cargando..." : "↻ Refrescar"}
          </button>
          <select
            value={range}
            onChange={(e) => setRange(e.target.value as "last24h" | "last7d" | "last30d")}
            style={{ padding: "0.4rem 0.8rem", borderRadius: "6px", border: "1px solid #cbd5e1", fontWeight: 600 }}
          >
            <option value="last24h">Últimas 24 horas</option>
            <option value="last7d">Últimos 7 días</option>
            <option value="last30d">Últimos 30 días</option>
          </select>
        </div>
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

      {/* TARJETAS DE KPIS PRINCIPALES */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
          gap: "1rem",
          marginBottom: "2rem",
        }}
      >
        <div style={kpiBoxStyle}>
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Tasa de Ocupación Activa</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#0284c7" }}>
            {loading ? "..." : `${kpis?.activeOccupancyRate?.toFixed(1) ?? "0.0"}%`}
          </div>
          <div style={{ fontSize: "0.75rem", color: "#16a34a" }}>KPI en tiempo real</div>
        </div>
        <div style={kpiBoxStyle}>
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Tiempo de Ciclo Promedio</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#d97706" }}>
            {loading ? "..." : `${kpis?.averageCycleHours?.toFixed(1) ?? "0.0"} hrs`}
          </div>
          <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Creación a Check-out</div>
        </div>
        <div style={kpiBoxStyle}>
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Reservas Hoy</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#16a34a" }}>
            {loading ? "..." : (kpis?.reservationsToday ?? 0)}
          </div>
          <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Ingresadas al sistema</div>
        </div>
        <div style={kpiBoxStyle}>
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Total Reservas (Rango)</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#8b5cf6" }}>
            {loading ? "..." : (kpis?.totalReservations ?? 0)}
          </div>
          <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Filtro: {range}</div>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))", gap: "1.5rem" }}>
        {/* GRAFICO / BARRAS DE RESERVAS POR HORA */}
        <div style={{ background: "white", border: "1px solid #e2e8f0", borderRadius: "8px", padding: "1.25rem" }}>
          <h3 style={{ marginTop: 0, fontSize: "1rem", color: "#1e293b" }}>
            Distribución Estimada de Reservas por Hora (Rango: {range})
          </h3>
          <div style={{ display: "flex", alignItems: "flex-end", height: "180px", gap: "1rem", paddingTop: "2rem" }}>
            {hourlyData.map((d) => (
              <div key={d.hour} style={{ flex: 1, textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center" }}>
                <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "#2563eb", marginBottom: "0.25rem" }}>
                  {d.count}
                </span>
                <div
                  style={{
                    width: "100%",
                    background: "#3b82f6",
                    borderRadius: "4px 4px 0 0",
                    height: `${d.count * 15}px`,
                    maxHeight: "140px",
                  }}
                />
                <span style={{ fontSize: "0.75rem", color: "#64748b", marginTop: "0.5rem" }}>{d.hour}</span>
              </div>
            ))}
          </div>
        </div>

        {/* UNIDADES MAS DEMANDADAS */}
        <div style={{ background: "white", border: "1px solid #e2e8f0", borderRadius: "8px", padding: "1.25rem" }}>
          <h3 style={{ marginTop: 0, fontSize: "1rem", color: "#1e293b" }}>
            Unidades Más Demandadas (Top Units - Rango: {range})
          </h3>
          {topUnits.length === 0 ? (
            <div style={{ padding: "2rem", textAlign: "center", color: "#94a3b8", fontSize: "0.9rem" }}>
              Sin datos de demanda registrados para este rango temporal.
            </div>
          ) : (
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.85rem" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid #e2e8f0", color: "#64748b", textAlign: "left" }}>
                  <th style={{ padding: "0.5rem" }}>Unidad</th>
                  <th style={{ padding: "0.5rem" }}>Reservas</th>
                  <th style={{ padding: "0.5rem" }}>Ingresos Estimados</th>
                </tr>
              </thead>
              <tbody>
                {topUnits.map((u, idx) => (
                  <tr key={u.unitId ?? idx} style={{ borderBottom: "1px solid #f1f5f9" }}>
                    <td style={{ padding: "0.5rem", fontWeight: 600 }}>{u.name}</td>
                    <td style={{ padding: "0.5rem" }}>{u.bookings}</td>
                    <td style={{ padding: "0.5rem", color: "#16a34a", fontWeight: 600 }}>
                      {typeof u.revenue === "number" ? `$${u.revenue.toLocaleString("es-CL")}` : (u.revenue || "N/A")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

const kpiBoxStyle = {
  background: "white",
  border: "1px solid #e2e8f0",
  borderRadius: "8px",
  padding: "1rem",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
};
