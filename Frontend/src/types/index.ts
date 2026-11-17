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

// ── Administración de RabbitMQ (ms-andesstay-mqadmin) ────────────────────────

export interface QueueInfo {
  name: string;
  durable: boolean;
  autoDelete: boolean;
  exclusive: boolean;
  messageCount: number | null;
  consumerCount: number | null;
  arguments?: Record<string, unknown> | null;
}

export interface ExchangeInfo {
  name: string;
  type: string;
  durable: boolean;
  autoDelete: boolean;
}

export interface BindingInfo {
  queue: string;
  exchange: string;
  routingKey: string;
}

export interface QueueRequest {
  name: string;
  durable?: boolean;
  autoDelete?: boolean;
  ttlMillis?: number | null;
  deadLetter?: boolean;
}

export interface ExchangeRequest {
  name: string;
  type?: string;
  durable?: boolean;
  autoDelete?: boolean;
}

export interface BindingRequest {
  queue: string;
  exchange: string;
  routingKey: string;
}

export interface PublishRequest {
  /** Exchange administrado de destino (modo exchange). Opcional. */
  exchange?: string;
  /** Routing key (modo exchange; acepta cadena vacía). */
  routingKey?: string;
  /** Cola administrada de destino, vía exchange por defecto (modo cola). Opcional. */
  queue?: string;
  /** Cuerpo del mensaje (JSON en texto). */
  payload: string;
}

export interface PublishResult {
  destination: string;
  routingKey: string;
  bytes: number;
  contentType: string;
}

// ── Administración de Kafka (ms-andesstay-kafkaadmin) ────────────────────────

export interface KafkaTopicInfo {
  name: string;
  partitions: number;
  replicationFactor: number;
  internal: boolean;
  configs: Record<string, string>;
}

export interface KafkaPartitionDetail {
  partition: number;
  leader: number;
  replicas: number[];
  isr: number[];
}

export interface KafkaTopicDetail {
  name: string;
  partitions: number;
  replicationFactor: number;
  configs: Record<string, string>;
  partitionsDetail: KafkaPartitionDetail[];
}

export interface KafkaDeclaredTopic {
  name: string;
  partitions: number;
  replicationFactor: number;
  retentionMs: number;
  retentionBytes: number;
  cleanupPolicy: string;
  minInsyncReplicas: number;
}

export interface KafkaConsumerGroupInfo {
  groupId: string;
  state: string;
  members: number;
  totalLag: number;
}

export interface KafkaPartitionLag {
  topic: string;
  partition: number;
  currentOffset: number;
  logEndOffset: number;
  lag: number;
}

export interface KafkaConsumerGroupDetail {
  groupId: string;
  state: string;
  coordinator: string | null;
  members: {
    memberId: string;
    clientId: string;
    host: string;
    assignment: string[];
  }[];
  partitions: KafkaPartitionLag[];
  totalLag: number;
}

export interface CreateKafkaTopicRequest {
  name: string;
  partitions: number;
  replicationFactor: number;
  configs?: Record<string, string>;
}

export interface KafkaLoadTestResult {
  sent: number;
  durationMs: number;
  throughputPerSecond: number;
}
