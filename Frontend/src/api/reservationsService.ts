import axiosClient from "./axiosClient";
import type { Reservation, CreateReservationRequest, ReservationStatus } from "../types";

export interface ReservationFilterParams {
  status?: ReservationStatus | string;
  from?: string;
  to?: string;
}

export const reservationsService = {
  /**
   * Obtiene la lista de reservas con filtros opcionales de estado y fechas
   */
  async getReservations(params?: ReservationFilterParams): Promise<Reservation[]> {
    const response = await axiosClient.get<Reservation[]>("/api/reservations", {
      params,
    });
    return response.data;
  },

  /**
   * Obtiene el detalle de una reserva por su ID
   */
  async getReservationById(id: string | number): Promise<Reservation> {
    const response = await axiosClient.get<Reservation>(`/api/reservations/${id}`);
    return response.data;
  },

  /**
   * Crea una nueva reserva (Huésped o Recepcionista)
   */
  async createReservation(payload: CreateReservationRequest): Promise<Reservation> {
    const response = await axiosClient.post<Reservation>("/api/reservations", payload);
    return response.data;
  },

  /**
   * Actualiza el estado de una reserva según la máquina de estados del caso
   */
  async updateReservationStatus(id: string | number, status: ReservationStatus | string): Promise<Reservation> {
    const response = await axiosClient.put<Reservation>(`/api/reservations/${id}/status`, {
      status,
    });
    return response.data;
  },
};

export default reservationsService;
