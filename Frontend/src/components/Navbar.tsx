import { NavLink, useNavigate } from "react-router-dom";
import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { useUserRole } from "../hooks/useUserRole";
import type { UserRole } from "../types";

const roleBadgeClass: Record<UserRole, string> = {
  Admin: "badge badge--violet",
  Recepcionista: "badge badge--blue",
  Huésped: "badge badge--green",
  Auditor: "badge badge--amber",
};

const linkStyle = ({ isActive }: { isActive: boolean }) => ({
  color: isActive ? "#ffffff" : "#cbd5e1",
  textDecoration: "none",
  fontWeight: isActive ? 700 : 500,
  fontSize: "0.88rem",
  padding: "0.4rem 0.85rem",
  borderRadius: "999px",
  background: isActive
    ? "linear-gradient(135deg, rgba(2, 132, 199, 0.85), rgba(7, 89, 133, 0.85))"
    : "transparent",
  boxShadow: isActive ? "0 2px 8px rgba(2, 132, 199, 0.4)" : "none",
  transition: "color 0.15s ease, background 0.15s ease",
  whiteSpace: "nowrap" as const,
});

export function Navbar() {
  const { instance } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const { role, user } = useUserRole();
  const navigate = useNavigate();

  const handleLogout = () => {
    instance.logoutRedirect({ postLogoutRedirectUri: "/login" }).catch(console.error);
  };

  if (!isAuthenticated) return null;

  const initials = (user.name || "U")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

  return (
    <header
      style={{
        position: "sticky",
        top: 0,
        zIndex: 50,
        background: "linear-gradient(90deg, #0f172a 0%, #1e293b 55%, #164e63 100%)",
        color: "#ffffff",
        padding: "0.65rem 1.75rem",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        flexWrap: "wrap",
        gap: "0.75rem",
        boxShadow: "0 4px 16px rgba(2, 6, 23, 0.35)",
        borderBottom: "1px solid rgba(148, 163, 184, 0.12)",
        backdropFilter: "blur(8px)",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "1.75rem", flexWrap: "wrap" }}>
        <div
          onClick={() => navigate("/dashboard")}
          style={{
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            gap: "0.55rem",
          }}
        >
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              width: "34px",
              height: "34px",
              borderRadius: "10px",
              fontSize: "1.05rem",
              background: "linear-gradient(135deg, #0284c7, #0d9488)",
              boxShadow: "0 2px 8px rgba(13, 148, 136, 0.45)",
            }}
          >
            🏔️
          </span>
          <span style={{ lineHeight: 1.1 }}>
            <span
              style={{
                display: "block",
                fontWeight: 800,
                fontSize: "1.15rem",
                letterSpacing: "-0.4px",
              }}
            >
              AndesStay
            </span>
            <span
              style={{
                display: "block",
                fontSize: "0.62rem",
                color: "#7dd3fc",
                letterSpacing: "1.6px",
                textTransform: "uppercase",
                fontWeight: 600,
              }}
            >
              Mountain Stays
            </span>
          </span>
        </div>

        <nav style={{ display: "flex", gap: "0.3rem", flexWrap: "wrap" }}>
          <NavLink to="/dashboard" style={linkStyle}>
            Dashboard
          </NavLink>

          {(role === "Admin" || role === "Recepcionista" || role === "Huésped") && (
            <NavLink to="/reservations" style={linkStyle}>
              Reservas
            </NavLink>
          )}

          {(role === "Admin" || role === "Recepcionista") && (
            <NavLink to="/catalog" style={linkStyle}>
              Catálogo
            </NavLink>
          )}

          {role === "Admin" && (
            <NavLink to="/reports" style={linkStyle}>
              Reportería
            </NavLink>
          )}

          {(role === "Admin" || role === "Auditor") && (
            <NavLink to="/audit" style={linkStyle}>
              Auditoría
            </NavLink>
          )}

          {role === "Admin" && (
            <NavLink to="/messaging" style={linkStyle}>
              Mensajería
            </NavLink>
          )}
        </nav>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: "0.9rem", flexWrap: "wrap" }}>
        {/* Bloque de usuario (solo lectura): el rol lo asigna Azure Entra ID */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "0.6rem",
            background: "rgba(148, 163, 184, 0.12)",
            padding: "0.3rem 0.75rem 0.3rem 0.35rem",
            borderRadius: "999px",
            border: "1px solid rgba(148, 163, 184, 0.18)",
          }}
        >
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              width: "30px",
              height: "30px",
              borderRadius: "50%",
              background: "linear-gradient(135deg, #38bdf8, #0d9488)",
              color: "#0f172a",
              fontSize: "0.75rem",
              fontWeight: 800,
            }}
          >
            {initials}
          </span>
          <span style={{ display: "flex", flexDirection: "column", lineHeight: 1.25 }}>
            <span style={{ fontSize: "0.82rem", fontWeight: 600, color: "#f1f5f9" }}>
              {user.name}
            </span>
            <span className={roleBadgeClass[role]} style={{ fontSize: "0.58rem", padding: "0.05rem 0.45rem" }}>
              {role}
            </span>
          </span>
        </div>

        <button
          onClick={handleLogout}
          className="btn btn-sm"
          style={{
            background: "rgba(239, 68, 68, 0.14)",
            border: "1px solid rgba(239, 68, 68, 0.35)",
            color: "#fca5a5",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = "rgba(239, 68, 68, 0.32)";
            e.currentTarget.style.color = "#ffffff";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = "rgba(239, 68, 68, 0.14)";
            e.currentTarget.style.color = "#fca5a5";
          }}
        >
          Cerrar Sesión
        </button>
      </div>
    </header>
  );
}
