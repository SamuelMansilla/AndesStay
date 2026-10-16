# Tarea 09 — mqadmin: RabbitAdminService

- **Fase / Nº:** 09
- **Indicador(es) de la pauta:** **Indicador 7 (10%)** — lógica de administración encapsulada en un servicio
- **Depende de:** 08
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(mqadmin): encapsular administración de RabbitMQ en RabbitAdminService`
- **Riesgo de rollback:** bajo

## Objetivo
Encapsular **toda** la interacción con `AmqpAdmin` en un servicio dedicado. Los futuros controladores no deben conocer detalles de RabbitMQ: solo llaman a métodos de alto nivel.

## Alcance
**Incluye**
- `RabbitAdminService` como único punto de acceso a `org.springframework.amqp.core.AmqpAdmin` y a la consulta de estado (`RabbitManagementService`/`AmqpAdmin.getQueueProperties`).
- Métodos de alto nivel para queues, exchanges, bindings y purga.
- Modelos de salida (`QueueInfo`, `ExchangeInfo`, `BindingInfo`).

**NO incluye**
- Endpoints REST (tarea 10).
- Validación de entrada (tarea 10).

## Prerrequisitos
- Tarea 08 completada (microservicio arranca).

## Pasos
1. Asegurar un bean `AmqpAdmin` disponible:
   ```java
   @Bean
   public AmqpAdmin amqpAdmin(ConnectionFactory cf) {
       return new RabbitAdmin(cf);
   }
   ```
   (Spring Boot ya lo autoconfigura si hay `ConnectionFactory`, pero declararlo explícito facilita la lectura.)
2. Crear `model/QueueInfo`, `ExchangeInfo`, `BindingInfo` (name, durable, autoDelete, arguments, tipo, mensajes, etc.).
3. Crear `service/RabbitAdminService` con métodos:
   - **Colas:** `createQueue(QueueRequest)`, `deleteQueue(String name)`, `purgeQueue(String name)`, `listQueues()`.
   - **Exchanges:** `createExchange(ExchangeRequest)`, `deleteExchange(String name)`, `listExchanges()`.
   - **Bindings:** `createBinding(BindingRequest)`, `deleteBinding(BindingRequest)`, `listBindings()`.
4. Usar `amqpAdmin.declareQueue(...)`, `declareExchange(...)`, `declareBinding(...)`, `deleteQueue(...)`, `deleteExchange(...)`, `removeBinding(...)`, `purgeQueue(...)`.
5. Para listar, usar `RabbitAdmin.getQueueProperties(name)` (requiere que el `RabbitAdmin` tenga el `ConnectionFactory`) o `AmqpAdmin.getQueueProperties`. Documentar la limitación de la librería (no expone listado nativo completo → build por nombres conocidos/config).
6. Registrar cada operación en `INFO` (auditable).

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `.../mqadmin/service/RabbitAdminService.java` |
| Crear | `.../mqadmin/config/RabbitAdminConfig.java` |
| Crear | `.../mqadmin/model/{QueueInfo,ExchangeInfo,BindingInfo}.java` |
| Crear | `.../mqadmin/service/dto/{QueueRequest,ExchangeRequest,BindingRequest}.java` (internos del servicio) |

## Criterios de aceptación (Definition of Done)
- [ ] Ninguna llamada a `AmqpAdmin`/`RabbitAdmin` fuera de `RabbitAdminService`.
- [ ] El servicio expone métodos de alto nivel (crear/eliminar/listar/purgar) para queues, exchanges y bindings.
- [ ] Cada operación se registra en logs.
- [ ] Compila y arranca.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-mqadmin && .\mvnw.cmd -q clean package -DskipTests
# Crear un test rápido o invocar desde un CommandLineRunner temporal (solo para verificar)
Select-String -Path src/main/java -Pattern "AmqpAdmin|RabbitAdmin" -Recurse
# Solo debe aparecer en RabbitAdminService y RabbitAdminConfig
```

## Rollback
```bash
git revert <hash>
```
> Servicio nuevo sin consumidores aún; revertir no afecta a otros módulos.

## Notas
- El indicador 7 exige que la lógica esté **en un servicio**; evitar atajos en el controlador.
- Si `listQueues` es limitado por la API, documentar que se listan las colas gestionadas/conocidas desde configuración.

---

## Resultado — 2026-10-16

### Estrategia de listado (limitación de la librería)

Spring AMQP **no** expone un listado nativo completo del broker (haría falta la Management HTTP API). El servicio mantiene un **registro de recursos administrados**:

- se **siembra** al arrancar con la topología declarada en `andesstay.messaging.*` (indicador 1), **sin contactar al broker** (el dueño de esa topología es `notify`);
- se **actualiza** con cada `create*`/`delete*` de la API.

El conteo de mensajes/consumidores sí se consulta al broker en el listado (`AmqpAdmin.getQueueInfo`), que devuelve `null` si la cola aún no existe; los errores de conexión se propagan (→ 502 en la tarea 10).

### Archivos creados

| Archivo | Responsabilidad |
|---------|-----------------|
| `config/RabbitAdminConfig.java` | Bean `AmqpAdmin` (`RabbitAdmin` con `autoStartup=false`) |
| `service/RabbitAdminService.java` | Único punto de acceso a `AmqpAdmin` (indicador 7) |
| `model/{QueueInfo,ExchangeInfo,BindingInfo}.java` | Modelos de salida (records) |
| `service/dto/{QueueRequest,ExchangeRequest,BindingRequest}.java` | Peticiones internas del servicio |

`RabbitAdminService` expone: `createQueue/deleteQueue/purgeQueue/listQueues`, `createExchange/deleteExchange/listExchanges`, `createBinding/deleteBinding/listBindings`. Cada operación se registra en `INFO` con el prefijo `[MQADMIN]`. Las reglas de negocio (recurso duplicado, recurso no administrado, tipo de exchange no soportado) lanzan `IllegalArgumentException`, que la tarea 10 traduce a un código HTTP.

### Verificación

- **Compila:** `.\mvnw.cmd -B clean package -DskipTests` → **BUILD SUCCESS**.
- **Encapsulación (indicador 7):** grep de `AmqpAdmin|RabbitAdmin` sobre `src/main/java` → aparece **solo** en `RabbitAdminService` y `RabbitAdminConfig` (más menciones en Javadoc).
- **Arranque** (`java -jar`, sin broker disponible):
  ```text
  INFO  c.d.m.service.RabbitAdminService : [MQADMIN] Topología declarada registrada: 2 exchanges, 6 colas, 6 bindings
  INFO  c.d.m.MsAndesstayMqadminApplication : Started MsAndesstayMqadminApplication in 2.01 seconds
  ```
  Sin `ERROR` ni trazas al arrancar (la siembra no toca el broker).

### Limitación de entorno

Igual que en la tarea 08, **no hay broker** en esta máquina, por lo que las operaciones contra el broker (declarar/eliminar/purgar reales) no se pudieron ejercitar aquí; se cubrirán con `AmqpAdmin` mockeado en la tarea 13 (`RabbitAdminServiceTest`).

### Rollback

```bash
git revert <hash>
```
Servicio aún sin endpoints públicos; revertir no afecta a otros módulos.
