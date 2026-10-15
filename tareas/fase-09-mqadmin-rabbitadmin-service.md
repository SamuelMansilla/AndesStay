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
