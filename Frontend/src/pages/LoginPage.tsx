import { useState } from "react";
import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { InteractionStatus } from "@azure/msal-browser";
import { Navigate } from "react-router-dom";
import { loginRequest } from "../authConfig";

export function LoginPage() {
  const { instance, inProgress } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const isAuthenticating =
    inProgress === InteractionStatus.HandleRedirect ||
    inProgress === InteractionStatus.Startup;

  const handleLogin = async () => {
    setErrorMessage(null);
    if (inProgress === InteractionStatus.None) {
      try {
        // Redirección completa recomendada por Microsoft para SPAs (evita bucles y bloqueos de popups)
        await instance.loginRedirect(loginRequest);
      } catch (redirectErr: unknown) {
        console.error("Error en loginRedirect:", redirectErr);
        if (redirectErr instanceof Error) {
          setErrorMessage(redirectErr.message);
        } else {
          setErrorMessage("Ocurrió un error inesperado al iniciar sesión con Azure AD.");
        }
      }
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        position: "relative",
        overflow: "hidden",
        background: "linear-gradient(165deg, #0b1526 0%, #10314d 45%, #0f3d50 100%)",
        padding: "1.5rem",
        color: "#f8fafc",
        fontFamily: "system-ui, -apple-system, sans-serif",
      }}
    >
      {/* Orbes de luz decorativos */}
      <div
        style={{
          position: "absolute",
          width: "420px",
          height: "420px",
          borderRadius: "50%",
          top: "-140px",
          right: "-100px",
          background: "radial-gradient(circle, rgba(56, 189, 248, 0.22), transparent 70%)",
          pointerEvents: "none",
        }}
      />
      <div
        style={{
          position: "absolute",
          width: "480px",
          height: "480px",
          borderRadius: "50%",
          bottom: "-180px",
          left: "-140px",
          background: "radial-gradient(circle, rgba(13, 148, 136, 0.22), transparent 70%)",
          pointerEvents: "none",
        }}
      />

      {/* Silueta de montañas de los Andes */}
      <svg
        viewBox="0 0 1440 320"
        preserveAspectRatio="none"
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          width: "100%",
          height: "38vh",
          pointerEvents: "none",
        }}
      >
        <path
          d="M0 260 L180 140 L300 230 L430 90 L560 210 L700 60 L830 190 L980 100 L1120 220 L1250 110 L1440 240 L1440 320 L0 320 Z"
          fill="#0a1826"
          opacity="0.9"
        />
        <path
          d="M0 300 L200 210 L340 290 L480 180 L640 280 L800 150 L950 270 L1100 190 L1280 290 L1440 200 L1440 320 L0 320 Z"
          fill="#081320"
          opacity="0.95"
        />
      </svg>

      <div
        style={{
          position: "relative",
          zIndex: 1,
          background: "rgba(255, 255, 255, 0.97)",
          color: "#0f172a",
          padding: "2.5rem 2.5rem 2rem 2.5rem",
          borderRadius: "20px",
          maxWidth: "440px",
          width: "100%",
          boxShadow: "0 30px 60px -12px rgba(2, 6, 23, 0.55), 0 0 0 1px rgba(148, 163, 184, 0.2)",
          textAlign: "center",
          animation: "as-fade-up 0.5s ease-out both",
          overflow: "hidden",
        }}
      >
        {/* Barra de acento superior */}
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            height: "5px",
            background: "linear-gradient(90deg, #0284c7, #38bdf8, #0d9488)",
          }}
        />

        <div
          style={{
            width: "64px",
            height: "64px",
            margin: "0 auto 0.75rem auto",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "2rem",
            borderRadius: "18px",
            background: "linear-gradient(135deg, #0284c7, #0d9488)",
            boxShadow: "0 10px 22px -6px rgba(2, 132, 199, 0.55)",
          }}
        >
          🏔️
        </div>

        <h1 style={{ fontSize: "1.8rem", margin: "0 0 0.35rem 0", fontWeight: 800, letterSpacing: "-0.5px" }}>
          AndesStay
        </h1>
        <p style={{ color: "#64748b", margin: "0 0 1.75rem 0", fontSize: "0.92rem" }}>
          Plataforma Unificada de Reservas
          <br />
          para Hostales y Cabañas
        </p>

        <div
          style={{
            background: "linear-gradient(135deg, #f0f9ff, #f8fafc)",
            border: "1px solid #e0f2fe",
            borderRadius: "12px",
            padding: "1rem 1.1rem",
            marginBottom: "1.75rem",
            textAlign: "left",
            fontSize: "0.83rem",
            color: "#334155",
          }}
        >
          <div style={{ fontWeight: 700, marginBottom: "0.3rem", display: "flex", alignItems: "center", gap: "0.4rem" }}>
            <span
              style={{
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                width: "20px",
                height: "20px",
                borderRadius: "50%",
                background: "#dcfce7",
                fontSize: "0.65rem",
              }}
            >
              🔒
            </span>
            Acceso Corporativo Seguro (IDaaS)
          </div>
          <div style={{ color: "#64748b" }}>
            Autenticación federada mediante <strong>Microsoft Azure Entra ID</strong> con tokens JWT
            protegidos.
          </div>
        </div>

        {errorMessage && (
          <div
            style={{
              background: "#fef2f2",
              border: "1px solid #fecaca",
              color: "#991b1b",
              borderRadius: "10px",
              padding: "0.85rem 1rem",
              marginBottom: "1.5rem",
              fontSize: "0.83rem",
              textAlign: "left",
              lineHeight: 1.45,
            }}
          >
            <strong>⚠️ Error de autenticación</strong>
            <p style={{ margin: "0.3rem 0 0 0" }}>{errorMessage}</p>
          </div>
        )}

        {isAuthenticating ? (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: "1rem",
              padding: "1.25rem 0",
            }}
          >
            <div className="spinner" />
            <p style={{ color: "#475569", fontSize: "0.92rem", fontWeight: 500, margin: 0 }}>
              Autenticando con Microsoft Entra ID...
            </p>
          </div>
        ) : (
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
              padding: "0.9rem 1.25rem",
              borderRadius: "10px",
              fontSize: "1rem",
              fontWeight: 600,
              cursor: inProgress !== InteractionStatus.None ? "not-allowed" : "pointer",
              boxShadow: "0 6px 16px -4px rgba(0, 120, 212, 0.55)",
              transition: "background 0.2s, transform 0.12s, box-shadow 0.2s",
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = "#106ebe";
              e.currentTarget.style.transform = "translateY(-1px)";
              e.currentTarget.style.boxShadow = "0 8px 20px -4px rgba(0, 120, 212, 0.6)";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = "#0078d4";
              e.currentTarget.style.transform = "translateY(0)";
              e.currentTarget.style.boxShadow = "0 6px 16px -4px rgba(0, 120, 212, 0.55)";
            }}
          >
            <svg width="20" height="20" viewBox="0 0 21 21" fill="none">
              <path d="M1 1h9v9H1z" fill="#f25022" />
              <path d="M11 1h9v9H11z" fill="#7fba00" />
              <path d="M1 11h9v9H1z" fill="#00a4ef" />
              <path d="M11 11h9v9H11z" fill="#ffb900" />
            </svg>
            Iniciar sesión con Microsoft
          </button>
        )}

        <div
          style={{
            marginTop: "1.6rem",
            paddingTop: "1.25rem",
            borderTop: "1px solid #f1f5f9",
            fontSize: "0.73rem",
            color: "#94a3b8",
            lineHeight: 1.6,
          }}
        >
          Red AndesStay &bull; Acceso para Administradores, Recepcionistas, Huéspedes y Auditores
          <br />
          <span style={{ color: "#cbd5e1" }}>El rol es asignado por Azure Entra ID en el momento del login</span>
        </div>
      </div>
    </div>
  );
}
