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
    <div className="page">
      <div className="page-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
        <div>
          <h1 className="page-title">📊 Reportería y Analítica (KPIs)</h1>
          <p className="page-subtitle">
            Panel de indicadores en tiempo real procesados vía Kafka / Microservicio ms-andesstay-report.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
          <button className="btn btn-secondary" onClick={() => refreshReportData(range)} disabled={loading}>
            {loading ? "Cargando..." : "↻ Refrescar"}
          </button>
          <select
            value={range}
            onChange={(e) => setRange(e.target.value as "last24h" | "last7d" | "last30d")}
            style={{ fontWeight: 600 }}
          >
            <option value="last24h">Últimas 24 horas</option>
            <option value="last7d">Últimos 7 días</option>
            <option value="last30d">Últimos 30 días</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="alert alert--info" style={{ marginBottom: "1.5rem" }}>
          ℹ️ {error}
        </div>
      )}

      {/* TARJETAS DE KPIS PRINCIPALES */}
      <section className="section-gap">
        <h2 className="section-title">📈 Indicadores Clave</h2>
        <div className="kpi-grid" style={{ marginBottom: "2.25rem" }}>
          <div className="card card--hover accent-top accent-top--blue">
            <div className="card-title">Tasa de Ocupación Activa</div>
            <div className="kpi-value" style={{ color: "#0284c7" }}>
              {loading ? "..." : `${kpis?.activeOccupancyRate?.toFixed(1) ?? "0.0"}%`}
            </div>
            <div className="kpi-hint" style={{ color: "#16a34a" }}>KPI en tiempo real</div>
          </div>
          <div className="card card--hover accent-top accent-top--amber">
            <div className="card-title">Tiempo de Ciclo Promedio</div>
            <div className="kpi-value" style={{ color: "#d97706" }}>
              {loading ? "..." : `${kpis?.averageCycleHours?.toFixed(1) ?? "0.0"} hrs`}
            </div>
            <div className="kpi-hint">Creación a Check-out</div>
          </div>
          <div className="card card--hover accent-top accent-top--green">
            <div className="card-title">Reservas Hoy</div>
            <div className="kpi-value" style={{ color: "#16a34a" }}>
              {loading ? "..." : (kpis?.reservationsToday ?? 0)}
            </div>
            <div className="kpi-hint">Ingresadas al sistema</div>
          </div>
          <div className="card card--hover accent-top accent-top--violet">
            <div className="card-title">Total Reservas (Rango)</div>
            <div className="kpi-value" style={{ color: "#7c3aed" }}>
              {loading ? "..." : (kpis?.totalReservations ?? 0)}
            </div>
            <div className="kpi-hint">Filtro: {range}</div>
          </div>
        </div>
      </section>

      {loading ? (
        <div style={{ display: "flex", justifyContent: "center", padding: "3rem 0" }}>
          <div className="spinner" />
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))", gap: "1.5rem" }}>
          {/* GRAFICO / BARRAS DE RESERVAS POR HORA */}
          <div className="card accent-top accent-top--blue">
            <h3 className="card-title" style={{ fontSize: "0.95rem", color: "#1e293b" }}>
              Distribución Estimada de Reservas por Hora (Rango: {range})
            </h3>
            <div style={{ display: "flex", alignItems: "flex-end", height: "180px", gap: "1rem", paddingTop: "2rem" }}>
              {hourlyData.map((d) => (
                <div key={d.hour} style={{ flex: 1, textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center" }}>
                  <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "#0284c7", marginBottom: "0.25rem" }}>
                    {d.count}
                  </span>
                  <div
                    style={{
                      width: "100%",
                      background: "linear-gradient(180deg, #38bdf8, #0284c7)",
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
          <div>
            <h3 className="card-title" style={{ fontSize: "0.95rem", color: "#1e293b", marginBottom: "1rem" }}>
              Unidades Más Demandadas (Top Units - Rango: {range})
            </h3>
            {topUnits.length === 0 ? (
              <div className="card">
                <div className="empty-state">
                  <span className="empty-icon">📊</span>
                  <span>Sin datos de demanda registrados para este rango temporal.</span>
                </div>
              </div>
            ) : (
              <div className="table-card card">
                <table>
                  <thead>
                    <tr>
                      <th>Unidad</th>
                      <th>Reservas</th>
                      <th>Ingresos Estimados</th>
                    </tr>
                  </thead>
                  <tbody>
                    {topUnits.map((u, idx) => (
                      <tr key={u.unitId ?? idx}>
                        <td style={{ fontWeight: 600 }}>{u.name}</td>
                        <td>{u.bookings}</td>
                        <td style={{ color: "#16a34a", fontWeight: 600 }}>
                          {typeof u.revenue === "number" ? `$${u.revenue.toLocaleString("es-CL")}` : (u.revenue || "N/A")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
