import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { InteractionStatus } from "@azure/msal-browser";
import { Navigate } from "react-router-dom";
import { loginRequest } from "../authConfig";

export function LoginPage() {
  const { instance, inProgress } = useMsal();
  const isAuthenticated = useIsAuthenticated();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleLogin = () => {
    if (inProgress === InteractionStatus.None) {
      instance.loginRedirect(loginRequest).catch(console.error);
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "linear-gradient(135deg, #0f172a 0%, #1e293b 100%)",
        padding: "1.5rem",
        color: "#f8fafc",
        fontFamily: "system-ui, -apple-system, sans-serif",
      }}
    >
      <div
        style={{
          background: "#ffffff",
          color: "#0f172a",
          padding: "2.5rem",
          borderRadius: "12px",
          maxWidth: "460px",
          width: "100%",
          boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.3)",
          textAlign: "center",
        }}
      >
        <div style={{ fontSize: "3rem", marginBottom: "0.5rem" }}>🏔️</div>
        <h1 style={{ fontSize: "1.75rem", margin: "0 0 0.5rem 0", fontWeight: 800 }}>
          AndesStay
        </h1>
        <p style={{ color: "#64748b", margin: "0 0 2rem 0", fontSize: "0.95rem" }}>
          Plataforma Unificada de Reservas para Hostales y Cabañas
        </p>

        <div
          style={{
            background: "#f1f5f9",
            borderRadius: "8px",
            padding: "1rem",
            marginBottom: "2rem",
            textAlign: "left",
            fontSize: "0.85rem",
            color: "#334155",
          }}
        >
          <div style={{ fontWeight: 700, marginBottom: "0.25rem" }}>
            🔒 Acceso Corporativo Seguro (IDaaS)
          </div>
          <div>
            Autenticación federada mediante <strong>Microsoft Azure Entra ID</strong> con tokens JWT protegidos.
          </div>
        </div>

        <button
          onClick={handleLogin}
          disabled={inProgress !== InteractionStatus.None}
          style={{
            width: "100%",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: "0.75rem",
            background: "#0078d4",
            color: "#ffffff",
            border: "none",
            padding: "0.85rem 1.25rem",
            borderRadius: "6px",
            fontSize: "1rem",
            fontWeight: 600,
            cursor: inProgress !== InteractionStatus.None ? "not-allowed" : "pointer",
            boxShadow: "0 4px 6px -1px rgba(0, 120, 212, 0.4)",
            transition: "background 0.2s",
          }}
        >
          <svg width="20" height="20" viewBox="0 0 21 21" fill="none">
            <path d="M1 1h9v9H1z" fill="#f25022" />
            <path d="M11 1h9v9H11z" fill="#7fba00" />
            <path d="M1 11h9v9H1z" fill="#00a4ef" />
            <path d="M11 11h9v9H11z" fill="#ffb900" />
          </svg>
          {inProgress !== InteractionStatus.None ? "Cargando..." : "Iniciar sesión con Microsoft"}
        </button>

        <div style={{ marginTop: "1.5rem", fontSize: "0.75rem", color: "#94a3b8" }}>
          Red AndesStay &bull; Acceso para Administradores, Recepcionistas, Huéspedes y Auditores
        </div>
      </div>
    </div>
  );
}
