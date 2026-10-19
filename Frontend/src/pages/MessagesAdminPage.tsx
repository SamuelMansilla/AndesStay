import { useCallback, useEffect, useState } from "react";
import mqAdminService from "../api/mqAdminService";
import type { BindingInfo, ExchangeInfo, QueueInfo } from "../types";

interface ApiErrorData {
  error?: string;
  message?: string;
  fields?: Record<string, string>;
}

/** Extrae un mensaje legible de un error de Axios (400/401/403/502…). */
function describeError(err: unknown): string {
  const e = err as { response?: { status?: number; data?: ApiErrorData }; message?: string };
  const status = e.response?.status;
  const data = e.response?.data;

  if (status === 401) return "No autenticado (401): inicia sesión con una cuenta Admin.";
  if (status === 403) return "Acceso denegado (403): se requiere el rol Admin.";
  if (status === 502) return "No se pudo contactar el broker RabbitMQ (502).";

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

/** Etiqueta visual de una cola: roja si es DLQ, ámbar si tiene mensajes pendientes, verde si está sana. */
function queueBadge(q: QueueInfo): { className: string; label: string } {
  const name = q.name.toLowerCase();
  if (name.includes("dlq") || name.includes("dead") || name.includes("deadletter")) {
    return { className: "badge badge--red", label: "DLQ" };
  }
  if ((q.messageCount ?? 0) > 0) {
    return { className: "badge badge--amber", label: `${q.messageCount} pendiente${q.messageCount === 1 ? "" : "s"}` };
  }
  return { className: "badge badge--green", label: "activa" };
}

export function MessagesAdminPage() {
  const [queues, setQueues] = useState<QueueInfo[]>([]);
  const [exchanges, setExchanges] = useState<ExchangeInfo[]>([]);
  const [bindings, setBindings] = useState<BindingInfo[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [newQueue, setNewQueue] = useState({ name: "", durable: true, autoDelete: false, deadLetter: true, ttl: "" });
  const [newExchange, setNewExchange] = useState({ name: "", type: "direct" });
  const [newBinding, setNewBinding] = useState({ queue: "", exchange: "", routingKey: "" });
  const [testMessage, setTestMessage] = useState({
    destinationType: "queue" as "queue" | "exchange",
    queue: "",
    exchange: "",
    routingKey: "",
    payload:
      '{"reservationId":1,"recipient":"huesped@andesstay.cl","channel":"EMAIL","subject":"Prueba desde el panel","body":"Hola desde AndesStay"}',
  });
  const [publishing, setPublishing] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [queueList, exchangeList, bindingList] = await Promise.all([
        mqAdminService.getQueues(),
        mqAdminService.getExchanges(),
        mqAdminService.getBindings(),
      ]);
      setQueues(queueList);
      setExchanges(exchangeList);
      setBindings(bindingList);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  // Pre-selección amable: mejor cola de demo y exchange principal
  useEffect(() => {
    setTestMessage((s) => {
      const next = { ...s };
      if (!next.queue && queues.length > 0) {
        const demo = queues.find((q) => /^q\.demo/.test(q.name)) ?? queues[0];
        next.queue = demo.name;
      }
      if (!next.exchange && exchanges.length > 0) {
        const principal = exchanges.find((x) => x.name.includes("notifications")) ?? exchanges[0];
        next.exchange = principal.name;
      }
      return next;
    });
  }, [queues, exchanges]);

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

  const handleCreateQueue = (e: React.FormEvent) => {
    e.preventDefault();
    const ttl = newQueue.ttl.trim() === "" ? null : Number(newQueue.ttl);
    void runAction(async () => {
      await mqAdminService.createQueue({
        name: newQueue.name,
        durable: newQueue.durable,
        autoDelete: newQueue.autoDelete,
        deadLetter: newQueue.deadLetter,
        ttlMillis: ttl,
      });
      setNewQueue({ name: "", durable: true, autoDelete: false, deadLetter: true, ttl: "" });
    }, `Cola "${newQueue.name}" creada.`);
  };

  const handleDeleteQueue = (name: string) => {
    if (!window.confirm(`¿Eliminar la cola "${name}"?`)) return;
    void runAction(() => mqAdminService.deleteQueue(name), `Cola "${name}" eliminada.`);
  };

  const handlePurgeQueue = (name: string) => {
    void runAction(
      async () => {
        const result = await mqAdminService.purgeQueue(name);
        setNotice(`Cola "${name}" purgada (${result.purged} mensajes).`);
      },
      `Cola "${name}" purgada.`
    );
  };

  const handleCreateExchange = (e: React.FormEvent) => {
    e.preventDefault();
    void runAction(async () => {
      await mqAdminService.createExchange({ name: newExchange.name, type: newExchange.type, durable: true });
      setNewExchange({ name: "", type: "direct" });
    }, `Exchange "${newExchange.name}" creado.`);
  };

  const handleDeleteExchange = (name: string) => {
    if (!window.confirm(`¿Eliminar el exchange "${name}"?`)) return;
    void runAction(() => mqAdminService.deleteExchange(name), `Exchange "${name}" eliminado.`);
  };

  const handleCreateBinding = (e: React.FormEvent) => {
    e.preventDefault();
    void runAction(async () => {
      await mqAdminService.createBinding(newBinding);
      setNewBinding({ queue: "", exchange: "", routingKey: "" });
    }, "Binding creado.");
  };

  const handleDeleteBinding = (binding: BindingInfo) => {
    void runAction(
      () => mqAdminService.deleteBinding(binding),
      `Binding ${binding.exchange} → ${binding.queue} eliminado.`
    );
  };

  const handlePublishTestMessage = (e: React.FormEvent) => {
    e.preventDefault();
    setPublishing(true);
    setError(null);
    setNotice(null);
    const body =
      testMessage.destinationType === "queue"
        ? { queue: testMessage.queue, payload: testMessage.payload }
        : { exchange: testMessage.exchange, routingKey: testMessage.routingKey, payload: testMessage.payload };
    void (async () => {
      try {
        const result = await mqAdminService.publishMessage(body);
        setNotice(
          `📤 Mensaje publicado: destino ${result.destination} --[${result.routingKey}] (${result.bytes} bytes). Observa los contadores y las gráficas en la Management UI de RabbitMQ.`
        );
        await refresh();
      } catch (err) {
        setError(describeError(err));
      } finally {
        setPublishing(false);
      }
    })();
  };

  return (
    <div className="page">
      <div className="page-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <h1 className="page-title">Administración de Mensajería (RabbitMQ)</h1>
          <p className="page-subtitle">
            Colas, exchanges y bindings del broker, vía ms-andesstay-mqadmin.
          </p>
        </div>
        <button onClick={() => void refresh()} disabled={loading} className="btn btn-secondary btn-sm">
          {loading ? "Cargando..." : "↻ Refrescar"}
        </button>
      </div>

      {error && <div className="alert alert--error" style={{ marginBottom: "1rem" }}>⛔ {error}</div>}
      {notice && <div className="alert alert--success" style={{ marginBottom: "1rem" }}>✅ {notice}</div>}

      {loading && queues.length === 0 && exchanges.length === 0 && bindings.length === 0 && (
        <div style={{ display: "flex", justifyContent: "center", padding: "3rem 0" }}>
          <div className="spinner" />
        </div>
      )}

      {/* ENVIAR MENSAJE DE PRUEBA */}
      <section className="section-gap">
        <h2 className="section-title">🚀 Enviar mensaje de prueba (generar tráfico)</h2>
        <div className="card accent-top accent-top--blue">
          <p style={{ color: "#64748b", fontSize: "0.85rem", margin: "0 0 0.9rem 0" }}>
            Publica un mensaje real al broker. <strong>Directo a una cola</strong> hace que el contador
            "Mensajes" de esa cola suba y baje aquí mismo;{" "}
            <strong>vía exchange</strong> respeta los bindings y verás el pico de tráfico en la
            Management UI de RabbitMQ.
          </p>
          <form onSubmit={handlePublishTestMessage} style={{ display: "flex", flexDirection: "column", gap: "0.65rem" }}>
            <div style={formRowStyle}>
              <select
                value={testMessage.destinationType}
                onChange={(e) =>
                  setTestMessage((s) => ({ ...s, destinationType: e.target.value as "queue" | "exchange" }))
                }
                style={{ flex: "0 0 260px" }}
              >
                <option value="queue">📦 Directo a una cola</option>
                <option value="exchange">🔁 Vía exchange (+ routing key)</option>
              </select>

              {testMessage.destinationType === "queue" ? (
                <select
                  value={testMessage.queue}
                  onChange={(e) => setTestMessage((s) => ({ ...s, queue: e.target.value }))}
                  style={{ flex: 1 }}
                  required
                >
                  <option value="">Cola…</option>
                  {queues.map((q) => (
                    <option key={q.name} value={q.name}>{q.name}</option>
                  ))}
                </select>
              ) : (
                <>
                  <select
                    value={testMessage.exchange}
                    onChange={(e) => setTestMessage((s) => ({ ...s, exchange: e.target.value }))}
                    style={{ flex: 1 }}
                    required
                  >
                    <option value="">Exchange…</option>
                    {exchanges.map((x) => (
                      <option key={x.name} value={x.name}>{x.name}</option>
                    ))}
                  </select>
                  <input
                    placeholder="Routing key (ej. notify.email)"
                    value={testMessage.routingKey}
                    onChange={(e) => setTestMessage((s) => ({ ...s, routingKey: e.target.value }))}
                    style={{ flex: 1 }}
                  />
                </>
              )}
            </div>

            <textarea
              value={testMessage.payload}
              onChange={(e) => setTestMessage((s) => ({ ...s, payload: e.target.value }))}
              rows={3}
              spellCheck={false}
              style={{
                fontFamily: "ui-monospace, SFMono-Regular, Consolas, monospace",
                fontSize: "0.82rem",
                resize: "vertical",
              }}
              required
            />

            <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", flexWrap: "wrap" }}>
              <button type="submit" className="btn btn-primary" disabled={publishing}>
                {publishing ? "Publicando…" : "📤 Publicar mensaje"}
              </button>
              <span style={{ fontSize: "0.75rem", color: "#94a3b8" }}>
                Tip: envía a una cola sin consumidores para ver "Mensajes: 1" acumulado en la tabla.
              </span>
            </div>
          </form>
        </div>
      </section>

      {/* COLAS */}
      <section className="section-gap">
        <h2 className="section-title">📮 Colas ({queues.length})</h2>

        <div className="card accent-top accent-top--teal" style={{ marginBottom: "1rem" }}>
          <form onSubmit={handleCreateQueue} style={formRowStyle}>
            <input
              placeholder="Nombre (q.mi.cola)"
              value={newQueue.name}
              onChange={(e) => setNewQueue((s) => ({ ...s, name: e.target.value }))}
              style={{ flex: 2 }}
              required
            />
            <input
              placeholder="TTL (ms, opcional)"
              value={newQueue.ttl}
              onChange={(e) => setNewQueue((s) => ({ ...s, ttl: e.target.value }))}
              style={{ flex: 1 }}
            />
            <label style={checkStyle}>
              <input type="checkbox" checked={newQueue.durable} onChange={(e) => setNewQueue((s) => ({ ...s, durable: e.target.checked }))} /> durable
            </label>
            <label style={checkStyle}>
              <input type="checkbox" checked={newQueue.deadLetter} onChange={(e) => setNewQueue((s) => ({ ...s, deadLetter: e.target.checked }))} /> DLQ
            </label>
            <label style={checkStyle}>
              <input type="checkbox" checked={newQueue.autoDelete} onChange={(e) => setNewQueue((s) => ({ ...s, autoDelete: e.target.checked }))} /> auto-delete
            </label>
            <button type="submit" className="btn btn-primary btn-sm">Crear cola</button>
          </form>
        </div>

        <div className="card table-card">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Estado</th>
                <th>Mensajes</th>
                <th>Consumidores</th>
                <th>Durable</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {queues.length === 0 && !loading && (
                <tr>
                  <td colSpan={6}>
                    <div className="empty-state">
                      <span className="empty-icon">✉️</span>
                      <span>Sin colas.</span>
                    </div>
                  </td>
                </tr>
              )}
              {queues.map((q) => {
                const badge = queueBadge(q);
                return (
                  <tr key={q.name}>
                    <td style={{ fontWeight: 600 }}>{q.name}</td>
                    <td><span className={badge.className}>{badge.label}</span></td>
                    <td>{q.messageCount ?? "—"}</td>
                    <td>{q.consumerCount ?? "—"}</td>
                    <td>
                      {q.durable
                        ? <span className="badge badge--green">durable</span>
                        : <span className="badge badge--slate">efímera</span>}
                    </td>
                    <td>
                      <button onClick={() => handlePurgeQueue(q.name)} className="btn btn-secondary btn-sm" style={{ marginRight: "0.4rem" }}>
                        Purgar
                      </button>
                      <button onClick={() => handleDeleteQueue(q.name)} className="btn btn-danger btn-sm">Eliminar</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      {/* EXCHANGES */}
      <section className="section-gap">
        <h2 className="section-title">🔁 Exchanges ({exchanges.length})</h2>

        <div className="card accent-top accent-top--violet" style={{ marginBottom: "1rem" }}>
          <form onSubmit={handleCreateExchange} style={formRowStyle}>
            <input
              placeholder="Nombre (ex.mi.exchange)"
              value={newExchange.name}
              onChange={(e) => setNewExchange((s) => ({ ...s, name: e.target.value }))}
              style={{ flex: 2 }}
              required
            />
            <select
              value={newExchange.type}
              onChange={(e) => setNewExchange((s) => ({ ...s, type: e.target.value }))}
              style={{ flex: 1 }}
            >
              <option value="direct">direct</option>
              <option value="topic">topic</option>
              <option value="fanout">fanout</option>
              <option value="headers">headers</option>
            </select>
            <button type="submit" className="btn btn-primary btn-sm">Crear exchange</button>
          </form>
        </div>

        <div className="card table-card">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Tipo</th>
                <th>Durable</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {exchanges.length === 0 && !loading && (
                <tr>
                  <td colSpan={4}>
                    <div className="empty-state">
                      <span className="empty-icon">✉️</span>
                      <span>Sin exchanges.</span>
                    </div>
                  </td>
                </tr>
              )}
              {exchanges.map((x) => (
                <tr key={x.name}>
                  <td style={{ fontWeight: 600 }}>{x.name}</td>
                  <td><span className="badge badge--violet">{x.type}</span></td>
                  <td>
                    {x.durable
                      ? <span className="badge badge--green">durable</span>
                      : <span className="badge badge--slate">efímero</span>}
                  </td>
                  <td>
                    <button onClick={() => handleDeleteExchange(x.name)} className="btn btn-danger btn-sm">Eliminar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* BINDINGS */}
      <section className="section-gap">
        <h2 className="section-title">🔗 Bindings ({bindings.length})</h2>

        <div className="card accent-top accent-top--teal" style={{ marginBottom: "1rem" }}>
          <form onSubmit={handleCreateBinding} style={formRowStyle}>
            <select
              value={newBinding.exchange}
              onChange={(e) => setNewBinding((s) => ({ ...s, exchange: e.target.value }))}
              style={{ flex: 1 }}
              required
            >
              <option value="">Exchange…</option>
              {exchanges.map((x) => (
                <option key={x.name} value={x.name}>{x.name}</option>
              ))}
            </select>
            <select
              value={newBinding.queue}
              onChange={(e) => setNewBinding((s) => ({ ...s, queue: e.target.value }))}
              style={{ flex: 1 }}
              required
            >
              <option value="">Cola…</option>
              {queues.map((q) => (
                <option key={q.name} value={q.name}>{q.name}</option>
              ))}
            </select>
            <input
              placeholder="Routing key"
              value={newBinding.routingKey}
              onChange={(e) => setNewBinding((s) => ({ ...s, routingKey: e.target.value }))}
              style={{ flex: 1 }}
              required
            />
            <button type="submit" className="btn btn-primary btn-sm">Crear binding</button>
          </form>
        </div>

        <div className="card table-card">
          <table>
            <thead>
              <tr>
                <th>Exchange</th>
                <th>Routing key</th>
                <th>Cola</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {bindings.length === 0 && !loading && (
                <tr>
                  <td colSpan={4}>
                    <div className="empty-state">
                      <span className="empty-icon">✉️</span>
                      <span>Sin bindings.</span>
                    </div>
                  </td>
                </tr>
              )}
              {bindings.map((b) => (
                <tr key={`${b.exchange}|${b.routingKey}|${b.queue}`}>
                  <td style={{ fontWeight: 600 }}>{b.exchange}</td>
                  <td><span className="badge badge--teal">{b.routingKey}</span></td>
                  <td>{b.queue}</td>
                  <td>
                    <button onClick={() => handleDeleteBinding(b)} className="btn btn-danger btn-sm">Eliminar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

// ── Estilos de layout (la apariencia proviene del sistema de diseño global) ──
const formRowStyle: React.CSSProperties = { display: "flex", gap: "0.5rem", flexWrap: "wrap", alignItems: "center" };
const checkStyle: React.CSSProperties = { display: "inline-flex", alignItems: "center", gap: "0.35rem" };
