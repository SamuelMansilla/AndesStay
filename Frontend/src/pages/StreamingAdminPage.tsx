import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import kafkaAdminService from "../api/kafkaAdminService";
import type {
  KafkaConsumerGroupDetail,
  KafkaConsumerGroupInfo,
  KafkaDeclaredTopic,
  KafkaLoadTestResult,
  KafkaTopicInfo,
} from "../types";

interface ApiErrorData {
  error?: string;
  message?: string;
  fields?: Record<string, string>;
}

function describeError(err: unknown): string {
  const e = err as { response?: { status?: number; data?: ApiErrorData }; message?: string };
  const status = e.response?.status;
  const data = e.response?.data;

  if (status === 401) return "No autenticado (401): inicia sesión con una cuenta Admin.";
  if (status === 403) return "Acceso denegado (403): se requiere el rol Admin.";
  if (status === 502 || status === 504) return "No se pudo contactar el clúster Kafka (502/504).";

  if (data?.fields) {
    const detail = Object.entries(data.fields)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join(" · ");
    return `${data.error ?? "Datos inválidos"} → ${detail}`;
  }
  if (data?.error) return data.error;
  if (data?.message) return data.message;
  return e.message ?? "Error desconocido";
}

function formatRetention(ms: string | undefined): string {
  if (!ms) return "—";
  const numeric = Number(ms);
  if (!Number.isFinite(numeric) || numeric < 0) return ms;
  const days = numeric / 86_400_000;
  return days >= 1 ? `${days.toFixed(days >= 10 ? 0 : 1)} d` : `${(numeric / 3_600_000).toFixed(1)} h`;
}

function retentionDaysToMs(days: string): string | undefined {
  const numeric = Number(days);
  if (!days.trim() || !Number.isFinite(numeric) || numeric <= 0) return undefined;
  return String(Math.round(numeric * 86_400_000));
}

export function StreamingAdminPage() {
  const [topics, setTopics] = useState<KafkaTopicInfo[]>([]);
  const [declared, setDeclared] = useState<KafkaDeclaredTopic[]>([]);
  const [groups, setGroups] = useState<KafkaConsumerGroupInfo[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<KafkaConsumerGroupDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [newTopic, setNewTopic] = useState({
    name: "",
    partitions: "3",
    replicationFactor: "3",
    retentionDays: "7",
  });
  const [loadTest, setLoadTest] = useState({ count: "5000", topic: "reservations.events" });
  const [loadResult, setLoadResult] = useState<KafkaLoadTestResult | null>(null);

  const declaredNames = useMemo(() => new Set(declared.map((d) => d.name)), [declared]);
  const totalLag = useMemo(() => groups.reduce((sum, group) => sum + group.totalLag, 0), [groups]);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [topicList, declaredList, groupList] = await Promise.all([
        kafkaAdminService.getTopics(),
        kafkaAdminService.getDeclaredTopics(),
        kafkaAdminService.getConsumerGroups(),
      ]);
      setTopics(topicList);
      setDeclared(declaredList);
      setGroups(groupList);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const runAction = async (action: () => Promise<void>, successMessage: string) => {
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(successMessage);
      await refresh();
    } catch (err) {
      setError(describeError(err));
    }
  };

  const handleCreateTopic = (event: FormEvent) => {
    event.preventDefault();
    const retention = retentionDaysToMs(newTopic.retentionDays);
    void runAction(async () => {
      await kafkaAdminService.createTopic({
        name: newTopic.name.trim(),
        partitions: Number(newTopic.partitions),
        replicationFactor: Number(newTopic.replicationFactor),
        configs: retention ? { "retention.ms": retention } : undefined,
      });
      setNewTopic({ name: "", partitions: "3", replicationFactor: "3", retentionDays: "7" });
    }, `Tópico '${newTopic.name.trim()}' creado.`);
  };

  const handleDeleteTopic = (name: string) => {
    if (!window.confirm(`¿Eliminar el tópico '${name}'? Esta acción no se puede deshacer.`)) return;
    void runAction(async () => {
      await kafkaAdminService.deleteTopic(name);
      setSelectedGroup(null);
    }, `Tópico '${name}' eliminado.`);
  };

  const handleEditRetention = (topic: KafkaTopicInfo) => {
    const current = formatRetention(topic.configs["retention.ms"]);
    const input = window.prompt(`Retención de '${topic.name}' en días (actual: ${current})`, "7");
    if (input === null) return;
    const retention = retentionDaysToMs(input);
    if (!retention) {
      setError("Ingresa un número de días mayor que 0.");
      return;
    }
    void runAction(async () => {
      await kafkaAdminService.updateConfigs(topic.name, { "retention.ms": retention });
    }, `Retención de '${topic.name}' actualizada.`);
  };

  const handleSelectGroup = async (groupId: string) => {
    setError(null);
    try {
      setSelectedGroup(await kafkaAdminService.getConsumerGroup(groupId));
    } catch (err) {
      setError(describeError(err));
    }
  };

  const handleLoadTest = (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setNotice(null);
    setLoadResult(null);
    void kafkaAdminService
      .loadTest(Number(loadTest.count), loadTest.topic)
      .then((result) => {
        setLoadResult(result);
        setNotice(`Carga enviada: ${result.sent} mensajes en ${result.durationMs} ms.`);
        return refresh();
      })
      .catch((err) => setError(describeError(err)));
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Streaming · Kafka</h1>
          <p className="page-subtitle">
            Tópicos, configuraciones, consumer groups y lag del clúster de streaming.
          </p>
        </div>
        <button className="btn btn-secondary" onClick={() => void refresh()} disabled={loading}>
          {loading ? "Actualizando…" : "Actualizar"}
        </button>
      </div>

      {error && <div className="alert alert--error">{error}</div>}
      {notice && <div className="alert alert--success">{notice}</div>}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))", gap: "1rem", marginBottom: "1.25rem" }}>
        <div className="card">
          <p className="card-title">Tópicos</p>
          <p style={{ fontSize: "1.8rem", fontWeight: 800, margin: 0 }}>{topics.length}</p>
        </div>
        <div className="card">
          <p className="card-title">Consumer groups</p>
          <p style={{ fontSize: "1.8rem", fontWeight: 800, margin: 0 }}>{groups.length}</p>
        </div>
        <div className="card">
          <p className="card-title">Lag total</p>
          <p style={{ fontSize: "1.8rem", fontWeight: 800, margin: 0, color: totalLag > 0 ? "#b45309" : "#166534" }}>
            {totalLag.toLocaleString("es-CL")}
          </p>
        </div>
      </div>

      <div className="card section-gap">
        <h2 className="section-title">Tópicos</h2>
        <div className="table-card" style={{ marginTop: "0.75rem" }}>
          <table>
            <thead>
              <tr>
                <th>Tópico</th>
                <th>Particiones</th>
                <th>RF</th>
                <th>Retención</th>
                <th>Cleanup</th>
                <th>Origen</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {topics.map((topic) => (
                <tr key={topic.name}>
                  <td style={{ fontFamily: "monospace" }}>{topic.name}</td>
                  <td>{topic.partitions}</td>
                  <td>{topic.replicationFactor}</td>
                  <td>{formatRetention(topic.configs["retention.ms"])}</td>
                  <td>{topic.configs["cleanup.policy"] ?? "—"}</td>
                  <td>
                    {declaredNames.has(topic.name) ? (
                      <span className="badge badge--violet">declarado</span>
                    ) : (
                      <span className="badge badge--slate">ad-hoc</span>
                    )}
                  </td>
                  <td style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap" }}>
                    <button className="btn btn-sm btn-secondary" onClick={() => handleEditRetention(topic)}>
                      Retención
                    </button>
                    {!declaredNames.has(topic.name) && (
                      <button className="btn btn-sm btn-danger" onClick={() => handleDeleteTopic(topic.name)}>
                        Eliminar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {topics.length === 0 && !loading && (
                <tr>
                  <td colSpan={7}>No hay tópicos visibles.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "1.25rem" }}>
        <div className="card">
          <h2 className="section-title">Crear tópico</h2>
          <form onSubmit={handleCreateTopic} style={{ display: "grid", gap: "0.7rem", marginTop: "0.75rem" }}>
            <label>
              Nombre
              <input
                value={newTopic.name}
                onChange={(e) => setNewTopic({ ...newTopic, name: e.target.value })}
                placeholder="ej: reservas.auditoria"
                required
              />
            </label>
            <label>
              Particiones
              <input
                type="number"
                min={1}
                max={50}
                value={newTopic.partitions}
                onChange={(e) => setNewTopic({ ...newTopic, partitions: e.target.value })}
              />
            </label>
            <label>
              Factor de replicación
              <input
                type="number"
                min={1}
                max={3}
                value={newTopic.replicationFactor}
                onChange={(e) => setNewTopic({ ...newTopic, replicationFactor: e.target.value })}
              />
            </label>
            <label>
              Retención (días)
              <input
                type="number"
                min={1}
                value={newTopic.retentionDays}
                onChange={(e) => setNewTopic({ ...newTopic, retentionDays: e.target.value })}
              />
            </label>
            <button className="btn btn-primary" type="submit">
              Crear tópico
            </button>
          </form>
        </div>

        <div className="card">
          <h2 className="section-title">Prueba de carga</h2>
          <p className="page-subtitle" style={{ marginTop: "0.4rem" }}>
            Genera eventos válidos en el tópico elegido para observar consumer lag y rebalanceo.
          </p>
          <form onSubmit={handleLoadTest} style={{ display: "grid", gap: "0.7rem", marginTop: "0.75rem" }}>
            <label>
              Tópico
              <input
                value={loadTest.topic}
                onChange={(e) => setLoadTest({ ...loadTest, topic: e.target.value })}
              />
            </label>
            <label>
              Cantidad
              <input
                type="number"
                min={1}
                max={200000}
                value={loadTest.count}
                onChange={(e) => setLoadTest({ ...loadTest, count: e.target.value })}
              />
            </label>
            <button className="btn btn-primary" type="submit">
              Enviar carga
            </button>
          </form>
          {loadResult && (
            <div className="alert alert--info" style={{ marginTop: "0.75rem" }}>
              {loadResult.sent.toLocaleString("es-CL")} mensajes · {loadResult.durationMs} ms ·{" "}
              {loadResult.throughputPerSecond.toFixed(2)} msg/s
            </div>
          )}
        </div>
      </div>

      <div className="card section-gap">
        <h2 className="section-title">Consumer groups</h2>
        <div className="table-card" style={{ marginTop: "0.75rem" }}>
          <table>
            <thead>
              <tr>
                <th>Grupo</th>
                <th>Estado</th>
                <th>Miembros</th>
                <th>Lag</th>
                <th>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {groups.map((group) => (
                <tr key={group.groupId}>
                  <td style={{ fontFamily: "monospace" }}>{group.groupId}</td>
                  <td>
                    <span className={`badge ${group.state === "Stable" ? "badge--green" : "badge--amber"}`}>
                      {group.state}
                    </span>
                  </td>
                  <td>{group.members}</td>
                  <td style={{ fontWeight: 700, color: group.totalLag > 0 ? "#b45309" : "#166534" }}>
                    {group.totalLag.toLocaleString("es-CL")}
                  </td>
                  <td>
                    <button className="btn btn-sm btn-secondary" onClick={() => void handleSelectGroup(group.groupId)}>
                      Ver particiones
                    </button>
                  </td>
                </tr>
              ))}
              {groups.length === 0 && !loading && (
                <tr>
                  <td colSpan={5}>No hay consumer groups activos.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {selectedGroup && (
          <div style={{ marginTop: "1rem" }}>
            <h3 className="section-title" style={{ fontSize: "1rem" }}>
              {selectedGroup.groupId} · lag {selectedGroup.totalLag.toLocaleString("es-CL")}
            </h3>
            <div className="table-card" style={{ marginTop: "0.5rem" }}>
              <table>
                <thead>
                  <tr>
                    <th>Tópico</th>
                    <th>Partición</th>
                    <th>Offset actual</th>
                    <th>Log end offset</th>
                    <th>Lag</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedGroup.partitions.map((partition) => (
                    <tr key={`${partition.topic}-${partition.partition}`}>
                      <td>{partition.topic}</td>
                      <td>{partition.partition}</td>
                      <td>{partition.currentOffset.toLocaleString("es-CL")}</td>
                      <td>{partition.logEndOffset.toLocaleString("es-CL")}</td>
                      <td style={{ fontWeight: 700 }}>{partition.lag.toLocaleString("es-CL")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default StreamingAdminPage;
