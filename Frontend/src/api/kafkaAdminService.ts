import axiosClient from "./axiosClient";
import type {
  CreateKafkaTopicRequest,
  KafkaConsumerGroupDetail,
  KafkaConsumerGroupInfo,
  KafkaDeclaredTopic,
  KafkaLoadTestResult,
  KafkaTopicDetail,
  KafkaTopicInfo,
} from "../types";

/**
 * Servicio de administración de Kafka.
 *
 * Consume el BFF (`/api/kafkaadmin/**`), que propaga el Bearer hacia
 * `ms-andesstay-kafkaadmin`. Todas las operaciones requieren rol Admin.
 */
export const kafkaAdminService = {
  async getTopics(): Promise<KafkaTopicInfo[]> {
    const response = await axiosClient.get<KafkaTopicInfo[]>("/api/kafkaadmin/topics");
    return response.data;
  },

  async getDeclaredTopics(): Promise<KafkaDeclaredTopic[]> {
    const response = await axiosClient.get<KafkaDeclaredTopic[]>("/api/kafkaadmin/topics/declared");
    return response.data;
  },

  async getTopic(name: string): Promise<KafkaTopicDetail> {
    const response = await axiosClient.get<KafkaTopicDetail>(
      `/api/kafkaadmin/topics/${encodeURIComponent(name)}`
    );
    return response.data;
  },

  async createTopic(payload: CreateKafkaTopicRequest): Promise<KafkaTopicDetail> {
    const response = await axiosClient.post<KafkaTopicDetail>("/api/kafkaadmin/topics", payload);
    return response.data;
  },

  async updateConfigs(name: string, configs: Record<string, string>): Promise<KafkaTopicDetail> {
    const response = await axiosClient.put<KafkaTopicDetail>(
      `/api/kafkaadmin/topics/${encodeURIComponent(name)}/configs`,
      { configs }
    );
    return response.data;
  },

  async increasePartitions(name: string, totalPartitions: number): Promise<KafkaTopicDetail> {
    const response = await axiosClient.post<KafkaTopicDetail>(
      `/api/kafkaadmin/topics/${encodeURIComponent(name)}/partitions`,
      { totalPartitions }
    );
    return response.data;
  },

  async deleteTopic(name: string): Promise<void> {
    await axiosClient.delete(`/api/kafkaadmin/topics/${encodeURIComponent(name)}`);
  },

  async getConsumerGroups(): Promise<KafkaConsumerGroupInfo[]> {
    const response = await axiosClient.get<KafkaConsumerGroupInfo[]>(
      "/api/kafkaadmin/consumer-groups"
    );
    return response.data;
  },

  async getConsumerGroup(groupId: string): Promise<KafkaConsumerGroupDetail> {
    const response = await axiosClient.get<KafkaConsumerGroupDetail>(
      `/api/kafkaadmin/consumer-groups/${encodeURIComponent(groupId)}`
    );
    return response.data;
  },

  async publish(topic: string, key: string, payload: string) {
    const response = await axiosClient.post(
      `/api/kafkaadmin/topics/${encodeURIComponent(topic)}/publish`,
      { key, payload }
    );
    return response.data;
  },

  async loadTest(count: number, topic: string): Promise<KafkaLoadTestResult> {
    const response = await axiosClient.post<KafkaLoadTestResult>("/api/kafkaadmin/load-test", {
      count,
      topic,
    });
    return response.data;
  },
};

export default kafkaAdminService;
