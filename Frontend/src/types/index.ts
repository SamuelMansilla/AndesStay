export type UserRole = "Admin" | "Recepcionista" | "Huésped" | "Auditor";

export type ReservationStatus =
  | "CREADA"
  | "CONFIRMADA"
  | "CHECKIN_PENDIENTE"
  | "EN_ESTADÍA"
  | "CHECKOUT"
  | "CANCELADA";

export interface Reservation {
  id: string;
  guestName: string;
  guestEmail: string;
  unitId: string;
  unitName: string;
  checkInDate: string;
  checkOutDate: string;
  status: ReservationStatus;
  totalPrice: number;
  createdAt: string;
}

export interface Unit {
  id: string;
  name: string;
  type: "Habitación" | "Cabaña" | "Lodge";
  capacity: number;
  pricePerNight: number;
  available: boolean;
  amenities: string[];
}

export interface AuditEvent {
  id: string;
  reservationId: string;
  action: string;
  performedBy: string;
  timestamp: string;
  details: string;
  eventType: "CREACION" | "CONFIRMACION" | "CHECK_IN" | "CHECK_OUT" | "CANCELACION";
}

export interface KpiSummary {
  reservationsToday: number;
  activeOccupancyRate: number; // Porcentaje
  averageCycleHours: number;
  topUnits: { name: string; bookings: number }[];
}
