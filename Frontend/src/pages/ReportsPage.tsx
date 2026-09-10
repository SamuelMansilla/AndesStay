import { useState } from "react";

export function ReportsPage() {
  const [range, setRange] = useState<"last24h" | "last7d" | "last30d">("last24h");

  const hourlyData = [
    { hour: "08:00", count: 2 },
    { hour: "10:00", count: 5 },
    { hour: "12:00", count: 9 },
    { hour: "14:00", count: 6 },
    { hour: "16:00", count: 12 },
    { hour: "18:00", count: 8 },
    { hour: "20:00", count: 4 },
  ];

  const topUnits = [
    { name: "Cabaña Los Alerces #1", bookings: 28, revenue: "$2.240.000" },
    { name: "Lodge Refugio Andino", bookings: 22, revenue: "$2.090.000" },
    { name: "Cabaña Los Coihues #2", bookings: 19, revenue: "$1.425.000" },
    { name: "Habitación Vista Lago 201", bookings: 14, revenue: "$630.000" },
  ];

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Reportería y Analítica (KPIs)</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Panel de indicadores en tiempo real procesados vía Kafka / Microservicio Report.
          </p>
        </div>
        <div>
          <select
            value={range}
            onChange={(e) => setRange(e.target.value as "last24h" | "last7d" | "last30d")}
            style={{ padding: "0.4rem 0.8rem", borderRadius: "6px", border: "1px solid #cbd5e1" }}
          >
            <option value="last24h">Últimas 24 horas</option>
            <option value="last7d">Últimos 7 días</option>
            <option value="last30d">Últimos 30 días</option>
          </select>
        </div>
      </div>

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
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Tasa de Ocupación</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#0284c7" }}>84.5%</div>
          <div style={{ fontSize: "0.75rem", color: "#16a34a" }}>↑ +5.2% vs periodo anterior</div>
        </div>
        <div style={kpiBoxStyle}>
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Tiempo de Ciclo Promedio</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#d97706" }}>3.4 días</div>
          <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Check-in a Check-out</div>
        </div>
        <div style={kpiBoxStyle}>
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Total Reservas Procesadas</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#16a34a" }}>83</div>
          <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Ingresadas al sistema</div>
        </div>
        <div style={kpiBoxStyle}>
          <div style={{ fontSize: "0.85rem", color: "#64748b" }}>Eventos de Streaming</div>
          <div style={{ fontSize: "1.8rem", fontWeight: 800, color: "#8b5cf6" }}>1,420 msgs</div>
          <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Tópico: reservations.events</div>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))", gap: "1.5rem" }}>
        {/* GRAFICO / BARRAS DE RESERVAS POR HORA */}
        <div style={{ background: "white", border: "1px solid #e2e8f0", borderRadius: "8px", padding: "1.25rem" }}>
          <h3 style={{ marginTop: 0, fontSize: "1rem", color: "#1e293b" }}>
            Distribución de Reservas por Hora (Rango: {range})
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
                    height: `${d.count * 12}px`,
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
            Unidades Más Demandadas (Top Cabañas/Habitaciones)
          </h3>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.85rem" }}>
            <thead>
              <tr style={{ borderBottom: "1px solid #e2e8f0", color: "#64748b", textAlign: "left" }}>
                <th style={{ padding: "0.5rem" }}>Unidad</th>
                <th style={{ padding: "0.5rem" }}>Reservas</th>
                <th style={{ padding: "0.5rem" }}>Ingresos Estimados</th>
              </tr>
            </thead>
            <tbody>
              {topUnits.map((u) => (
                <tr key={u.name} style={{ borderBottom: "1px solid #f1f5f9" }}>
                  <td style={{ padding: "0.5rem", fontWeight: 600 }}>{u.name}</td>
                  <td style={{ padding: "0.5rem" }}>{u.bookings}</td>
                  <td style={{ padding: "0.5rem", color: "#16a34a", fontWeight: 600 }}>{u.revenue}</td>
                </tr>
              ))}
            </tbody>
          </table>
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
