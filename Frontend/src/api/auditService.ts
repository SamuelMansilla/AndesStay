import axiosClient from "./axiosClient";
import type { AuditEvent } from "../types";

export const auditService = {
  /**
   * Obtiene la línea de tiempo completa de eventos de auditoría (read-only)
   */
  async getAuditLogs(): Promise<AuditEvent[]> {
    const response = await axiosClient.get<AuditEvent[]>("/api/audit");
    return response.data;
  },

  /**
   * Obtiene los eventos de auditoría filtrados por un actor específico
   */
  async getAuditLogsByActor(actor: string): Promise<AuditEvent[]> {
    const response = await axiosClient.get<AuditEvent[]>(`/api/audit/actor/${encodeURIComponent(actor)}`);
    return response.data;
  },
};

export default auditService;
