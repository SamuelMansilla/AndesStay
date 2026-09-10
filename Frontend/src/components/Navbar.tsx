import { NavLink, useNavigate } from "react-router-dom";
import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { useUserRole } from "../hooks/useUserRole";
import type { UserRole } from "../types";

export function Navbar() {
  const { instance } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const { role, user, setRoleOverride, isRoleOverridden } = useUserRole();
  const navigate = useNavigate();

  const handleLogout = () => {
    instance.logoutRedirect({ postLogoutRedirectUri: "/login" }).catch(console.error);
  };

  if (!isAuthenticated) return null;

  const roleColors: Record<UserRole, string> = {
    Admin: "#d32f2f",
    Recepcionista: "#1976d2",
    Huésped: "#2e7d32",
    Auditor: "#ed6c02",
  };

  return (
    <header
      style={{
        background: "#1e293b",
        color: "#ffffff",
        padding: "0.75rem 2rem",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        flexWrap: "wrap",
        gap: "1rem",
        boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "1.5rem" }}>
        <div
          onClick={() => navigate("/dashboard")}
          style={{
            cursor: "pointer",
            fontWeight: 800,
            fontSize: "1.25rem",
            letterSpacing: "-0.5px",
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
          }}
        >
          <span>🏔️</span>
          <span>AndesStay</span>
        </div>

        <nav style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
          <NavLink
            to="/dashboard"
            style={({ isActive }) => ({
              color: isActive ? "#38bdf8" : "#cbd5e1",
              textDecoration: "none",
              fontWeight: isActive ? 700 : 500,
              fontSize: "0.9rem",
              padding: "0.35rem 0.6rem",
              borderRadius: "4px",
              background: isActive ? "rgba(56, 189, 248, 0.1)" : "transparent",
            })}
          >
            Dashboard
          </NavLink>

          {(role === "Admin" || role === "Recepcionista" || role === "Huésped") && (
            <NavLink
              to="/reservations"
              style={({ isActive }) => ({
                color: isActive ? "#38bdf8" : "#cbd5e1",
                textDecoration: "none",
                fontWeight: isActive ? 700 : 500,
                fontSize: "0.9rem",
                padding: "0.35rem 0.6rem",
                borderRadius: "4px",
                background: isActive ? "rgba(56, 189, 248, 0.1)" : "transparent",
              })}
            >
              Reservas
            </NavLink>
          )}

          {(role === "Admin" || role === "Recepcionista") && (
            <NavLink
              to="/catalog"
              style={({ isActive }) => ({
                color: isActive ? "#38bdf8" : "#cbd5e1",
                textDecoration: "none",
                fontWeight: isActive ? 700 : 500,
                fontSize: "0.9rem",
                padding: "0.35rem 0.6rem",
                borderRadius: "4px",
                background: isActive ? "rgba(56, 189, 248, 0.1)" : "transparent",
              })}
            >
              Catálogo
            </NavLink>
          )}

          {role === "Admin" && (
            <NavLink
              to="/reports"
              style={({ isActive }) => ({
                color: isActive ? "#38bdf8" : "#cbd5e1",
                textDecoration: "none",
                fontWeight: isActive ? 700 : 500,
                fontSize: "0.9rem",
                padding: "0.35rem 0.6rem",
                borderRadius: "4px",
                background: isActive ? "rgba(56, 189, 248, 0.1)" : "transparent",
              })}
            >
              Reportería
            </NavLink>
          )}

          {(role === "Admin" || role === "Auditor") && (
            <NavLink
              to="/audit"
              style={({ isActive }) => ({
                color: isActive ? "#38bdf8" : "#cbd5e1",
                textDecoration: "none",
                fontWeight: isActive ? 700 : 500,
                fontSize: "0.9rem",
                padding: "0.35rem 0.6rem",
                borderRadius: "4px",
                background: isActive ? "rgba(56, 189, 248, 0.1)" : "transparent",
              })}
            >
              Auditoría
            </NavLink>
          )}
        </nav>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
        {/* Selector de modo rol para testing rápido */}
        <div style={{ display: "flex", alignItems: "center", gap: "0.35rem", fontSize: "0.8rem" }}>
          <span style={{ color: "#94a3b8" }}>Rol:</span>
          <select
            value={role}
            onChange={(e) => setRoleOverride(e.target.value as UserRole)}
            style={{
              background: "#334155",
              color: "#f8fafc",
              border: "1px solid #475569",
              padding: "0.25rem 0.5rem",
              borderRadius: "4px",
              fontSize: "0.8rem",
              cursor: "pointer",
            }}
          >
            <option value="Admin">Admin</option>
            <option value="Recepcionista">Recepcionista</option>
            <option value="Huésped">Huésped</option>
            <option value="Auditor">Auditor</option>
          </select>
          {isRoleOverridden && (
            <button
              onClick={() => setRoleOverride(null)}
              title="Restaurar a rol nativo de Azure"
              style={{
                background: "transparent",
                border: "none",
                color: "#f59e0b",
                cursor: "pointer",
                fontSize: "0.75rem",
                textDecoration: "underline",
              }}
            >
              (reset)
            </button>
          )}
        </div>

        {/* Badge de Rol */}
        <span
          style={{
            background: roleColors[role],
            color: "#fff",
            fontSize: "0.75rem",
            fontWeight: 700,
            padding: "0.25rem 0.6rem",
            borderRadius: "12px",
            textTransform: "uppercase",
            letterSpacing: "0.5px",
          }}
        >
          {role}
        </span>

        {/* Info de usuario y Logout */}
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
          <span style={{ fontSize: "0.85rem", color: "#e2e8f0" }}>{user.name}</span>
          <button
            onClick={handleLogout}
            style={{
              background: "#ef4444",
              color: "white",
              border: "none",
              padding: "0.35rem 0.75rem",
              borderRadius: "4px",
              fontSize: "0.8rem",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            Cerrar Sesión
          </button>
        </div>
      </div>
    </header>
  );
}
