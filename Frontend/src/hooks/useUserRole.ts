import { useMsal } from "@azure/msal-react";
import type { UserRole } from "../types";

/**
 * Rol del usuario obtenido EXCLUSIVAMENTE desde los claims del ID Token
 * emitido por Microsoft Azure Entra ID (App Roles configuradas en el Portal).
 * El frontend no permite seleccionar ni simular roles: eso lo administra Azure.
 */
export function useUserRole() {
  const { accounts } = useMsal();
  const currentAccount = accounts[0];

  // Extraer los roles nativos desde el ID Token de Azure Entra ID
  const tokenClaims = currentAccount?.idTokenClaims as { roles?: string[]; name?: string } | undefined;
  const azureRoles: string[] = tokenClaims?.roles || [];

  // Mapeo seguro a UserRole admitido por el caso. Si Azure no asignó ningún rol,
  // por defecto se asume "Huésped" (permisos mínimos).
  const role: UserRole = ((): UserRole => {
    if (azureRoles.includes("Admin")) return "Admin";
    if (azureRoles.includes("Recepcionista") || azureRoles.includes("Operador")) return "Recepcionista";
    if (azureRoles.includes("Auditor")) return "Auditor";
    if (azureRoles.includes("Huésped") || azureRoles.includes("Cliente")) return "Huésped";
    return "Huésped";
  })();

  return {
    role,
    azureRoles,
    hasRole: (allowedRoles: UserRole[]) => allowedRoles.includes(role),
    user: {
      name: currentAccount?.name || tokenClaims?.name || "Usuario",
      username: currentAccount?.username || "",
    },
  };
}
