# Tarea 10 — mqadmin: API REST + validación

- **Fase / Nº:** 10
- **Indicador(es) de la pauta:** **Indicador 6 (13%)** y **Indicador 8 (7%)**
- **Depende de:** 09
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(mqadmin): exponer API REST de administración con validación de parámetros`
- **Riesgo de rollback:** bajo

## Objetivo
Exponer una API REST clara y coherente para administrar RabbitMQ, con **validación de entrada** que impida crear colas/exchanges con nombres vacíos o configuraciones inválidas, y mensajes de error claros.

## Alcance
**Incluye**
- `MqAdminController` con endpoints REST (rutas/verbos RESTful).
- DTOs con **Jakarta Validation** (`@NotBlank`, `@Pattern`, `@Size`, rangos).
- `@RestControllerAdvice` (`MqAdminExceptionHandler`) → **400** con detalle de campos.
- Reglas de negocio: nombre no vacío, caracteres `[A-Za-z0-9._-]`, longitud 3–255, prohibido prefijo `amq.`, tipo de exchange válido.

**NO incluye**
- Seguridad OAuth2 (tarea 11).
- Integración con BFF (tarea 11).

## Prerrequisitos
- Tarea 09 completada (`RabbitAdminService`).

## Pasos
1. Crear `controller/MqAdminController` (delgado: solo `@Valid` + delegación):

   | Método | Ruta | Acción |
   |--------|------|--------|
   | `GET` | `/api/mqadmin/queues` | Listar colas |
   | `POST` | `/api/mqadmin/queues` | Crear cola |
   | `DELETE` | `/api/mqadmin/queues/{name}` | Eliminar cola |
   | `POST` | `/api/mqadmin/queues/{name}/purge` | Purgar cola |
   | `GET` | `/api/mqadmin/exchanges` | Listar exchanges |
   | `POST` | `/api/mqadmin/exchanges` | Crear exchange |
   | `DELETE` | `/api/mqadmin/exchanges/{name}` | Eliminar exchange |
   | `GET` | `/api/mqadmin/bindings` | Listar bindings |
   | `POST` | `/api/mqadmin/bindings` | Crear binding |
   | `DELETE` | `/api/mqadmin/bindings` | Eliminar binding |

2. DTOs:
   ```java
   public class QueueRequestDTO {
     @NotBlank @Size(min=3, max=255)
     @Pattern(regexp="^(?!amq\\.)[A-Za-z0-9._-]+$",
              message="Nombre inválido: use letras, números, '.', '_' o '-' (no puede empezar por amq.)")
     private String name;
     private boolean durable = true;
     private boolean autoDelete = false;
     @Min(0) private Integer ttlMillis;
     private boolean deadLetter = true;
   }
   // ExchangeRequestDTO: name + @Pattern; type ∈ {direct,topic,fanout,headers}; flags
   // BindingRequestDTO: queue, exchange, routingKey @NotBlank
   ```
3. `@Valid` en los `@RequestBody` y `@Validated` en el controlador para `@PathVariable`.
4. `MqAdminExceptionHandler` (`@RestControllerAdvice`):
   - `MethodArgumentNotValidException` → 400 con `{"error":"...","fields":{campo:mensaje}}`.
   - `IllegalArgumentException` del servicio → 400/409.
   - Excepción de conexión AMQP → 502.
5. Respuestas con códigos correctos: `201 Created` al crear, `204 No Content` al eliminar, `200 OK` al listar.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `.../mqadmin/controller/MqAdminController.java` |
| Crear | `.../mqadmin/dto/{QueueRequestDTO,ExchangeRequestDTO,BindingRequestDTO}.java` |
| Crear | `.../mqadmin/exception/MqAdminExceptionHandler.java` |

## Criterios de aceptación (Definition of Done)
- [ ] Todos los endpoints tienen rutas/verbos RESTful coherentes.
- [ ] Los DTO usan anotaciones de validación; `@Valid` activo.
- [ ] **No** se puede crear una cola con nombre vacío, inválido o con prefijo `amq.`.
- [ ] Los mensajes de error son claros (400 con detalle).
- [ ] Se puede crear, listar, eliminar y purgar colas/exchanges/bindings vía REST.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-mqadmin && .\mvnw.cmd spring-boot:run

# Crear cola válida
curl -i -X POST http://localhost:8086/api/mqadmin/queues -H "Content-Type: application/json" \
  -d '{"name":"q.prueba.demo","durable":true}'
# Esperado: 201

# Crear cola inválida
curl -i -X POST http://localhost:8086/api/mqadmin/queues -H "Content-Type: application/json" \
  -d '{"name":""}'
# Esperado: 400 con detalle

# Eliminar
curl -i -X DELETE http://localhost:8086/api/mqadmin/queues/q.prueba.demo
# Esperado: 204
```

## Rollback
```bash
git revert <hash>
```
> El servicio quedará sin endpoints; no afecta a otros módulos.

## Notas
- El indicador 8 se evalúa por la validación: dejar explícitas las reglas y los mensajes.
- Documentar los endpoints (tabla en el README del microservicio).

---

## Resultado — 2026-10-16

### Refinamiento respecto a la tarea 09

Para dar códigos HTTP correctos, el servicio estrena una excepción propia:

- **`ResourceNotFoundException` → 404** para recursos no administrados (antes lanzaba `IllegalArgumentException`).
- **`IllegalArgumentException` → 409** para conflictos (p. ej. cola ya existente).
- La validación de nombre/tipo en el DTO se convierte en **400** antes de llegar al servicio.

### Archivos creados

| Archivo | Responsabilidad |
|---------|-----------------|
| `controller/MqAdminController.java` | Endpoints REST (delgado: `@Valid` + delegación) |
| `dto/QueueRequestDTO.java`, `dto/ExchangeRequestDTO.java`, `dto/BindingRequestDTO.java` | DTOs con Jakarta Validation |
| `dto/ResourceNaming.java` | Regex/mensaje de nombrado compartidos (charset, longitud, `amq.`) |
| `exception/ResourceNotFoundException.java` | Recurso no administrado (404) |
| `exception/MqAdminExceptionHandler.java` | `@RestControllerAdvice` (400 / 404 / 409 / 502) |

### Endpoints

| Método | Ruta | Éxito |
|--------|------|-------|
| `GET` | `/api/mqadmin/queues` | 200 |
| `POST` | `/api/mqadmin/queues` | 201 |
| `DELETE` | `/api/mqadmin/queues/{name}` | 204 |
| `POST` | `/api/mqadmin/queues/{name}/purge` | 200 (`{queue, purged}`) |
| `GET` | `/api/mqadmin/exchanges` | 200 |
| `POST` | `/api/mqadmin/exchanges` | 201 |
| `DELETE` | `/api/mqadmin/exchanges/{name}` | 204 |
| `GET` | `/api/mqadmin/bindings` | 200 |
| `POST` | `/api/mqadmin/bindings` | 201 |
| `DELETE` | `/api/mqadmin/bindings` | 204 |

### Verificación (app en 8086, **sin broker**)

| Petición | Resultado | Comprobado |
|----------|-----------|------------|
| `POST /queues` `{"name":""}` | **400** `fields.name` | nombre vacío |
| `POST /queues` `{"name":"amq.demo"}` | **400** | prefijo `amq.` prohibido |
| `POST /queues` `{"name":"ab"}` | **400** | longitud < 3 |
| `POST /queues` `{"name":"q.prueba.demo"}` | **502** | pasó la validación y llegó al broker (caído) |
| `POST /exchanges` `{"name":"ex.demo","type":"invalido"}` | **400** | tipo de exchange |
| `DELETE /queues/ab` | **400** | validación del `@PathVariable` |
| `DELETE /queues/q.inexistente` | **404** | recurso no administrado |
| `POST /bindings` `{"queue":"","exchange":"ex.x","routingKey":"rk"}` | **400** | campos obligatorios |
| `GET /queues` | **502** | broker inalcanzable → 502 |

```text
POST   /api/mqadmin/queues   -> 400  {"error":"Datos de entrada inválidos","fields":{"name":"Nombre inválido: ..."}}
POST   /api/mqadmin/queues   -> 502  {"error":"No se pudo contactar el broker RabbitMQ: java.net.ConnectException: Connection refused"}
DELETE /api/mqadmin/queues/q.inexistente -> 404  {"error":"La cola no está administrada: q.inexistente"}
GET    /api/mqadmin/queues   -> 502  {"error":"No se pudo contactar el broker RabbitMQ: ..."}
```

### Limitación de entorno

Sin broker no se pueden ejercitar los códigos de **éxito** (201/200/204: crear/listar/purgar). Se cubrirán en la **tarea 13** con `AmqpAdmin` mockeado (`@WebMvcTest` del controlador + test del servicio), que es justo lo que el plan prevé para no depender de un broker externo.

### Rollback

```bash
git revert <hash>
```
Deja el microservicio sin endpoints REST (vuelve al estado de la tarea 09); no afecta a otros módulos.
