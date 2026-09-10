import { Navigate } from "react-router-dom";
import { useIsAuthenticated } from "@azure/msal-react";
import { useUserRole } from "../hooks/useUserRole";
import type { UserRole } from "../types";
import type { ReactNode } from "react";

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles?: UserRole[];
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const isAuthenticated = useIsAuthenticated();
  const { role } = useUserRole();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return (
      <div style={{ padding: "3rem", textAlign: "center", maxWidth: "600px", margin: "0 auto" }}>
        <h2 style={{ color: "#d32f2f" }}>⛔ Acceso Denegado (403)</h2>
        <p>
          Tu rol actual es <strong>{role}</strong>, pero esta sección requiere uno de los siguientes roles:
        </p>
        <div style={{ margin: "1rem 0" }}>
          {allowedRoles.map((r) => (
            <span
              key={r}
              style={{
                display: "inline-block",
                padding: "0.25rem 0.75rem",
                margin: "0 0.25rem",
                background: "#e0e0e0",
                borderRadius: "16px",
                fontSize: "0.85rem",
                fontWeight: 600,
              }}
            >
              {r}
            </span>
          ))}
        </div>
        <p style={{ color: "#666", fontSize: "0.9rem" }}>
          Puedes cambiar temporalmente de rol en el selector de la barra de navegación para evaluar esta vista.
        </p>
      </div>
    );
  }

  return <>{children}</>;
}
