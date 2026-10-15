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
