export type UserRole = "Admin" | "Recepcionista" | "Huésped" | "Auditor";

export type ReservationStatus =
  | "CREADA"
  | "CONFIRMADA"
  | "CHECKIN_PENDIENTE"
  | "EN_ESTADIA"
  | "EN_ESTADÍA"
  | "CHECKOUT"
  | "CANCELADA";

export interface Reservation {
  id: string | number;
  guestName?: string;
  guestEmail: string;
  unitId: string | number;
  unitName?: string;
  checkInDate: string;
  checkOutDate: string;
  status: ReservationStatus;
  totalPrice?: number;
  createdAt: string;
}

export interface CreateReservationRequest {
  guestEmail: string;
  unitId: number;
  checkInDate: string;
  checkOutDate: string;
}

export interface Unit {
  id: string | number;
  name: string;
  type: "Habitación" | "Cabaña" | "Lodge" | string;
  capacity: number;
  price?: number;
  pricePerNight?: number;
  available: boolean;
  amenities?: string[];
}

export interface AuditEvent {
  id: string | number;
  reservationId?: string;
  action?: string;
  performedBy?: string;
  actor?: string;
  timestamp: string;
  details?: string;
  payload?: string;
  eventType: string;
}

export interface KpiSummary {
  reservationsToday: number;
  activeOccupancyRate: number; // Porcentaje
  averageCycleHours: number;
  totalReservations?: number;
  range?: string;
  topUnits?: { name: string; bookings: number; revenue?: string }[];
}

export interface TopUnitItem {
  unitId?: number;
  name: string;
  bookings: number;
  revenue?: string | number;
}
