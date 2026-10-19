import axiosClient from "./axiosClient";
import type {
  BindingInfo,
  BindingRequest,
  ExchangeInfo,
  ExchangeRequest,
  PublishRequest,
  QueueInfo,
  QueueRequest,
} from "../types";

/**
 * Servicio de administración de RabbitMQ.
 *
 * Consume el BFF (`/api/mqadmin/**`), que propaga el Bearer hacia
 * `ms-andesstay-mqadmin`. Todas las operaciones requieren rol Admin.
 */
export const mqAdminService = {
  async getQueues(): Promise<QueueInfo[]> {
    const response = await axiosClient.get<QueueInfo[]>("/api/mqadmin/queues");
    return response.data;
  },

  async createQueue(payload: QueueRequest): Promise<QueueInfo> {
    const response = await axiosClient.post<QueueInfo>("/api/mqadmin/queues", payload);
    return response.data;
  },

  async deleteQueue(name: string): Promise<void> {
    await axiosClient.delete(`/api/mqadmin/queues/${encodeURIComponent(name)}`);
  },

  async purgeQueue(name: string): Promise<{ queue: string; purged: number }> {
    const response = await axiosClient.post<{ queue: string; purged: number }>(
      `/api/mqadmin/queues/${encodeURIComponent(name)}/purge`
    );
    return response.data;
  },

  async getExchanges(): Promise<ExchangeInfo[]> {
    const response = await axiosClient.get<ExchangeInfo[]>("/api/mqadmin/exchanges");
    return response.data;
  },

  async createExchange(payload: ExchangeRequest): Promise<ExchangeInfo> {
    const response = await axiosClient.post<ExchangeInfo>("/api/mqadmin/exchanges", payload);
    return response.data;
  },

  async deleteExchange(name: string): Promise<void> {
    await axiosClient.delete(`/api/mqadmin/exchanges/${encodeURIComponent(name)}`);
  },

  async getBindings(): Promise<BindingInfo[]> {
    const response = await axiosClient.get<BindingInfo[]>("/api/mqadmin/bindings");
    return response.data;
  },

  /** Publica un mensaje de prueba para generar tráfico visible en el Management UI. */
  async publishMessage(payload: PublishRequest): Promise<PublishResult> {
    const response = await axiosClient.post<PublishResult>("/api/mqadmin/publish", payload);
    return response.data;
  },

  async createBinding(payload: BindingRequest): Promise<BindingInfo> {
    const response = await axiosClient.post<BindingInfo>("/api/mqadmin/bindings", payload);
    return response.data;
  },

  async deleteBinding(payload: BindingRequest): Promise<void> {
    // DELETE con cuerpo: identifica el binding por (exchange, queue, routingKey).
    await axiosClient.delete("/api/mqadmin/bindings", { data: payload });
  },
};

export default mqAdminService;
