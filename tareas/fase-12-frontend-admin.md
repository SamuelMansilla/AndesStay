# Tarea 12 — frontend: panel de administración de mensajería

- **Fase / Nº:** 12
- **Indicador(es) de la pauta:** **Indicador 6** (apoyo demostrativo) + "frontend completo y funcional"
- **Depende de:** 11
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(frontend): panel de administración de colas y exchanges RabbitMQ`
- **Riesgo de rollback:** bajo

## Objetivo
Agregar una vista en el SPA (React) que permita listar, crear y eliminar colas, exchanges y bindings consumiendo el BFF (`/api/mqadmin/**`), demostrando el microservicio administrador de forma visual. Visible solo para rol **Admin**.

## Alcance
**Incluye**
- `mqAdminService.ts` (Axios) con métodos CRUD.
- Tipos en `types/index.ts`.
- `MessagesAdminPage.tsx` (ruta `/messaging`).
- Entrada en `Navbar` y ruta protegida `allowedRoles={["Admin"]}` en `App.tsx`.

**NO incluye**
- Cambios de backend (ya hechos en tareas 10–11).

## Prerrequisitos
- Tarea 11 completada (BFF expone `/api/mqadmin/**`).
- Sesión de Admin válida en MSAL.

## Pasos
1. `src/types/index.ts`: agregar `QueueInfo`, `ExchangeInfo`, `BindingInfo`, y requests.
2. `src/api/mqAdminService.ts`:
   ```ts
   export const mqAdminService = {
     getQueues: () => axiosClient.get<QueueInfo[]>("/api/mqadmin/queues").then(r => r.data),
     createQueue: (payload) => axiosClient.post("/api/mqadmin/queues", payload).then(r => r.data),
     deleteQueue: (name: string) => axiosClient.delete(`/api/mqadmin/queues/${encodeURIComponent(name)}`),
     purgeQueue: (name: string) => axiosClient.post(`/api/mqadmin/queues/${encodeURIComponent(name)}/purge`),
     getExchanges: () => ...,
     createExchange: (payload) => ...,
     deleteExchange: (name) => ...,
     getBindings: () => ...,
     createBinding: (payload) => ...,
     deleteBinding: (payload) => ...,
   };
   ```
3. `src/pages/MessagesAdminPage.tsx`:
   - Tabla de colas (nombre, mensajes, durable) con acciones crear/eliminar/purgar.
   - Sección de exchanges y bindings.
   - Manejo de errores 400/401/403 con mensajes claros.
   - Estados de carga.
4. `src/App.tsx`: ruta `/messaging` con `<ProtectedRoute allowedRoles={["Admin"]}>`.
5. `src/components/Navbar.tsx`: enlace "Mensajería" visible para Admin.
6. `npm run build` sin errores.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `Frontend/src/api/mqAdminService.ts` |
| Crear | `Frontend/src/pages/MessagesAdminPage.tsx` |
| Modificar | `Frontend/src/types/index.ts` |
| Modificar | `Frontend/src/App.tsx` |
| Modificar | `Frontend/src/components/Navbar.tsx` |

## Criterios de aceptación (Definition of Done)
- [ ] La vista lista colas/exchanges/bindings reales del broker.
- [ ] Se puede crear y eliminar desde la UI.
- [ ] Errores de validación (400) se muestran al usuario.
- [ ] Solo visible para Admin.
- [ ] `npm run build` sin errores de TypeScript.

## Cómo verificar
```bash
cd Frontend && npm run build && npm run dev
# Iniciar sesión como Admin → ir a /messaging
# Crear/eliminar una cola y verificar en la UI de RabbitMQ
```

## Rollback
```bash
git revert <hash>
```
> Añadido de UI aislado; revertir no afecta al backend.

## Notas
- Mantener el estilo de las páginas existentes (`CatalogPage`, `ReportsPage`).
- El encargo autoriza React (no Angular) según confirmación del docente — ver `context.md` §10.
