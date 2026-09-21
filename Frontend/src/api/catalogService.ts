import axiosClient from "./axiosClient";
import type { Unit } from "../types";

export const catalogService = {
  /**
   * Obtiene la lista de todas las unidades del catálogo (habitaciones/cabañas)
   */
  async getUnits(): Promise<Unit[]> {
    const response = await axiosClient.get<Unit[]>("/api/catalog/units");
    return response.data;
  },

  /**
   * Obtiene una unidad por su ID
   */
  async getUnitById(id: string | number): Promise<Unit> {
    const response = await axiosClient.get<Unit>(`/api/catalog/units/${id}`);
    return response.data;
  },

  /**
   * Crea una nueva unidad en el catálogo (Admin)
   */
  async createUnit(unit: Omit<Unit, "id">): Promise<Unit> {
    const response = await axiosClient.post<Unit>("/api/catalog/units", unit);
    return response.data;
  },

  /**
   * Actualiza una unidad existente (tarifa o disponibilidad)
   */
  async updateUnit(id: string | number, unit: Partial<Unit>): Promise<Unit> {
    const response = await axiosClient.put<Unit>(`/api/catalog/units/${id}`, unit);
    return response.data;
  },
};

export default catalogService;
