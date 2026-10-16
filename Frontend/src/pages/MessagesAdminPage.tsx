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

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#0f172a" }}>Administración de Mensajería (RabbitMQ)</h1>
          <p style={{ color: "#64748b", margin: "0.25rem 0 0 0" }}>
            Colas, exchanges y bindings del broker, vía ms-andesstay-mqadmin.
          </p>
        </div>
        <button onClick={() => void refresh()} disabled={loading} style={buttonStyle}>
          {loading ? "Cargando..." : "↻ Refrescar"}
        </button>
      </div>

      {error && <div style={alertErrorStyle}>⛔ {error}</div>}
      {notice && <div style={alertOkStyle}>✅ {notice}</div>}

      {/* COLAS */}
      <section style={cardStyle}>
        <h2 style={sectionTitleStyle}>Colas ({queues.length})</h2>

        <form onSubmit={handleCreateQueue} style={formRowStyle}>
          <input
            placeholder="Nombre (q.mi.cola)"
            value={newQueue.name}
            onChange={(e) => setNewQueue((s) => ({ ...s, name: e.target.value }))}
            style={{ ...inputStyle, flex: 2 }}
            required
          />
          <input
            placeholder="TTL (ms, opcional)"
            value={newQueue.ttl}
            onChange={(e) => setNewQueue((s) => ({ ...s, ttl: e.target.value }))}
            style={{ ...inputStyle, flex: 1 }}
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
          <button type="submit" style={primaryButtonStyle}>Crear cola</button>
        </form>

        <table style={tableStyle}>
          <thead>
            <tr style={theadRowStyle}>
              <th style={thStyle}>Nombre</th>
              <th style={thStyle}>Mensajes</th>
              <th style={thStyle}>Consumidores</th>
              <th style={thStyle}>Durable</th>
              <th style={thStyle}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {queues.length === 0 && (
              <tr><td colSpan={5} style={emptyStyle}>{loading ? "Cargando colas..." : "Sin colas."}</td></tr>
            )}
            {queues.map((q) => (
              <tr key={q.name} style={tbodyRowStyle}>
                <td style={{ ...tdStyle, fontWeight: 600 }}>{q.name}</td>
                <td style={tdStyle}>{q.messageCount ?? "—"}</td>
                <td style={tdStyle}>{q.consumerCount ?? "—"}</td>
                <td style={tdStyle}>{q.durable ? "sí" : "no"}</td>
                <td style={tdStyle}>
                  <button onClick={() => handlePurgeQueue(q.name)} style={secondaryButtonStyle}>Purgar</button>
                  <button onClick={() => handleDeleteQueue(q.name)} style={dangerButtonStyle}>Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* EXCHANGES */}
      <section style={cardStyle}>
        <h2 style={sectionTitleStyle}>Exchanges ({exchanges.length})</h2>
        <form onSubmit={handleCreateExchange} style={formRowStyle}>
          <input
            placeholder="Nombre (ex.mi.exchange)"
            value={newExchange.name}
            onChange={(e) => setNewExchange((s) => ({ ...s, name: e.target.value }))}
            style={{ ...inputStyle, flex: 2 }}
            required
          />
          <select
            value={newExchange.type}
            onChange={(e) => setNewExchange((s) => ({ ...s, type: e.target.value }))}
            style={{ ...inputStyle, flex: 1 }}
          >
            <option value="direct">direct</option>
            <option value="topic">topic</option>
            <option value="fanout">fanout</option>
            <option value="headers">headers</option>
          </select>
          <button type="submit" style={primaryButtonStyle}>Crear exchange</button>
        </form>

        <table style={tableStyle}>
          <thead>
            <tr style={theadRowStyle}>
              <th style={thStyle}>Nombre</th>
              <th style={thStyle}>Tipo</th>
              <th style={thStyle}>Durable</th>
              <th style={thStyle}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {exchanges.length === 0 && (
              <tr><td colSpan={4} style={emptyStyle}>{loading ? "Cargando exchanges..." : "Sin exchanges."}</td></tr>
            )}
            {exchanges.map((x) => (
              <tr key={x.name} style={tbodyRowStyle}>
                <td style={{ ...tdStyle, fontWeight: 600 }}>{x.name}</td>
                <td style={tdStyle}>{x.type}</td>
                <td style={tdStyle}>{x.durable ? "sí" : "no"}</td>
                <td style={tdStyle}>
                  <button onClick={() => handleDeleteExchange(x.name)} style={dangerButtonStyle}>Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* BINDINGS */}
      <section style={cardStyle}>
        <h2 style={sectionTitleStyle}>Bindings ({bindings.length})</h2>
        <form onSubmit={handleCreateBinding} style={formRowStyle}>
          <select
            value={newBinding.exchange}
            onChange={(e) => setNewBinding((s) => ({ ...s, exchange: e.target.value }))}
            style={{ ...inputStyle, flex: 1 }}
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
            style={{ ...inputStyle, flex: 1 }}
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
            style={{ ...inputStyle, flex: 1 }}
            required
          />
          <button type="submit" style={primaryButtonStyle}>Crear binding</button>
        </form>

        <table style={tableStyle}>
          <thead>
            <tr style={theadRowStyle}>
              <th style={thStyle}>Exchange</th>
              <th style={thStyle}>Routing key</th>
              <th style={thStyle}>Cola</th>
              <th style={thStyle}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {bindings.length === 0 && (
              <tr><td colSpan={4} style={emptyStyle}>{loading ? "Cargando bindings..." : "Sin bindings."}</td></tr>
            )}
            {bindings.map((b) => (
              <tr key={`${b.exchange}|${b.routingKey}|${b.queue}`} style={tbodyRowStyle}>
                <td style={{ ...tdStyle, fontWeight: 600 }}>{b.exchange}</td>
                <td style={tdStyle}>{b.routingKey}</td>
                <td style={tdStyle}>{b.queue}</td>
                <td style={tdStyle}>
                  <button onClick={() => handleDeleteBinding(b)} style={dangerButtonStyle}>Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

// ── Estilos (inline, consistentes con el resto de páginas) ───────────────────
const cardStyle: React.CSSProperties = {
  background: "white",
  border: "1px solid #e2e8f0",
  borderRadius: "8px",
  padding: "1.25rem",
  marginBottom: "1.5rem",
};
const sectionTitleStyle: React.CSSProperties = { marginTop: 0, fontSize: "1rem", color: "#1e293b" };
const tableStyle: React.CSSProperties = { width: "100%", borderCollapse: "collapse", fontSize: "0.875rem" };
const theadRowStyle: React.CSSProperties = { borderBottom: "1px solid #e2e8f0", color: "#64748b", textAlign: "left" };
const tbodyRowStyle: React.CSSProperties = { borderBottom: "1px solid #f1f5f9" };
const thStyle: React.CSSProperties = { padding: "0.5rem" };
const tdStyle: React.CSSProperties = { padding: "0.5rem" };
const emptyStyle: React.CSSProperties = { padding: "1.5rem", textAlign: "center", color: "#94a3b8" };
const formRowStyle: React.CSSProperties = { display: "flex", gap: "0.5rem", flexWrap: "wrap", alignItems: "center", marginBottom: "1rem" };
const inputStyle: React.CSSProperties = { padding: "0.4rem 0.6rem", borderRadius: "6px", border: "1px solid #cbd5e1" };
const checkStyle: React.CSSProperties = { fontSize: "0.8rem", color: "#475569", display: "flex", alignItems: "center", gap: "0.25rem" };
const buttonStyle: React.CSSProperties = { background: "#f1f5f9", color: "#334155", border: "1px solid #cbd5e1", padding: "0.4rem 0.8rem", borderRadius: "6px", fontWeight: 600, cursor: "pointer" };
const primaryButtonStyle: React.CSSProperties = { background: "#0284c7", color: "white", border: "none", padding: "0.45rem 0.9rem", borderRadius: "6px", fontWeight: 600, cursor: "pointer" };
const secondaryButtonStyle: React.CSSProperties = { background: "#f8fafc", color: "#334155", border: "1px solid #cbd5e1", padding: "0.25rem 0.6rem", borderRadius: "4px", fontWeight: 600, cursor: "pointer", marginRight: "0.4rem" };
const dangerButtonStyle: React.CSSProperties = { background: "#fee2e2", color: "#b91c1c", border: "1px solid #fecaca", padding: "0.25rem 0.6rem", borderRadius: "4px", fontWeight: 600, cursor: "pointer" };
const alertErrorStyle: React.CSSProperties = { padding: "0.75rem 1rem", marginBottom: "1rem", background: "#fef2f2", color: "#b91c1c", borderRadius: "6px", border: "1px solid #fecaca", fontSize: "0.875rem" };
const alertOkStyle: React.CSSProperties = { padding: "0.75rem 1rem", marginBottom: "1rem", background: "#f0fdf4", color: "#15803d", borderRadius: "6px", border: "1px solid #bbf7d0", fontSize: "0.875rem" };
