import { useMsal } from "@azure/msal-react";
import { useState, useEffect } from "react";
import type { UserRole } from "../types";

const ROLE_STORAGE_KEY = "andesstay_active_role_override";

export function useUserRole() {
  const { accounts } = useMsal();
  const currentAccount = accounts[0];

  // 1. Extraer los roles nativos desde el ID Token de Azure Entra ID
  const tokenClaims = currentAccount?.idTokenClaims as { roles?: string[]; name?: string } | undefined;
  const azureRoles: string[] = tokenClaims?.roles || [];

  // Mapeo seguro a UserRole admitido por el caso
  const detectedRole: UserRole | null = ((): UserRole | null => {
    if (azureRoles.includes("Admin")) return "Admin";
    if (azureRoles.includes("Recepcionista") || azureRoles.includes("Operador")) return "Recepcionista";
    if (azureRoles.includes("Auditor")) return "Auditor";
    if (azureRoles.includes("Huésped") || azureRoles.includes("Cliente")) return "Huésped";
    return null;
  })();

  // 2. Estado de simulación/override para pruebas locales antes de configurar Azure Portal
  const [roleOverride, setRoleOverride] = useState<UserRole | null>(() => {
    const saved = localStorage.getItem(ROLE_STORAGE_KEY);
    if (saved === "Admin" || saved === "Recepcionista" || saved === "Huésped" || saved === "Auditor") {
      return saved as UserRole;
    }
    return null;
  });

  useEffect(() => {
    if (roleOverride) {
      localStorage.setItem(ROLE_STORAGE_KEY, roleOverride);
    } else {
      localStorage.removeItem(ROLE_STORAGE_KEY);
    }
  }, [roleOverride]);

  // Si hay override manual activo (para testing de la pauta), tiene prioridad.
  // Si no, se usa el detectado en Azure. Si no hay ninguno, por defecto se asume "Huésped".
  const effectiveRole: UserRole = roleOverride || detectedRole || "Huésped";

  return {
    role: effectiveRole,
    azureRoles,
    isRoleOverridden: Boolean(roleOverride),
    setRoleOverride: (role: UserRole | null) => setRoleOverride(role),
    hasRole: (allowedRoles: UserRole[]) => allowedRoles.includes(effectiveRole),
    user: {
      name: currentAccount?.name || tokenClaims?.name || "Usuario",
      username: currentAccount?.username || "",
    },
  };
}
