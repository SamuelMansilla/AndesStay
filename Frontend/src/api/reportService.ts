import axiosClient from "./axiosClient";
import type { KpiSummary, TopUnitItem } from "../types";

export const reportService = {
  /**
   * Obtiene las métricas y KPIs clave según el rango temporal (default: last24h)
   */
  async getKpis(range: string = "last24h"): Promise<KpiSummary> {
    const response = await axiosClient.get<KpiSummary>("/api/report/kpis", {
      params: { range },
    });
    return response.data;
  },

  /**
   * Obtiene el listado de unidades más demandadas (top-units) según el rango temporal
   */
  async getTopUnits(range: string = "last7d"): Promise<TopUnitItem[]> {
    const response = await axiosClient.get<TopUnitItem[]>("/api/report/top-units", {
      params: { range },
    });
    return response.data;
  },
};

export default reportService;
