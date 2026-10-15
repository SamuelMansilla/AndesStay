# Tarea 13 — Pruebas automatizadas y de integración

- **Fase / Nº:** 13
- **Indicador(es) de la pauta:** **5 (20%)**, **7 (10%)**, **8 (7%)** — evidencia de calidad
- **Depende de:** 06, 10
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `test: cubrir ACK/DLQ, RabbitAdminService y validación de la API`
- **Riesgo de rollback:** bajo

## Objetivo
Respaldar con pruebas el cumplimiento de la pauta: ACK/NACK + DLQ en consumidores, encapsulación del admin service y validación de entrada de la API. La pauta pide que el backend "responda a pruebas básicas".

## Alcance
**Incluye**
- Test de contexto por microservicio (`contextLoads`).
- Test unitario de `RabbitAdminService` con `AmqpAdmin` mockeado.
- Test de validación de DTOs del admin (400).
- Test de consumidor con `spring-rabbit-test` (ACK y ruteo a DLQ) — usar broker local o `mock` de `Channel`.

**NO incluye**
- Cobertura exhaustiva de todos los módulos.

## Prerrequisitos
- Tareas 06 y 10 completadas.
- `spring-boot-starter-test` y `spring-rabbit-test` en los `pom.xml` correspondientes.

## Pasos
1. **notify** — test de listener:
   - Añadir dependencia `spring-rabbit-test` (test).
   - Test que publique un mensaje inválido y verifique que el `Channel` recibe `basicNack(requeue=false)` (con `Mockito` sobre `Channel`) y que el mensaje termina en DLQ (opcional con broker real).
2. **mqadmin** — test de `RabbitAdminService`:
   - Mockear `AmqpAdmin`; verificar que `createQueue` llama a `declareQueue(...)` con los argumentos correctos.
   - Verificar que `deleteQueue` llama `deleteQueue(name)`.
3. **mqadmin** — test de validación:
   - `@WebMvcTest(MqAdminController.class)` + `MockMvc`.
   - `POST /api/mqadmin/queues` con `name=""` → `400`.
   - `POST` con `name="amq.demo"` → `400`.
   - `POST` válido → `201` (con servicio mockeado).
4. Tests de contexto: `MsAndesstayMqadminApplicationTests.contextLoads()` (ajustar para no requerir broker, p. ej. `@SpringBootTest` con `spring.rabbitmq` apuntando a un broker o deshabilitando listeners).
5. Ejecutar `mvnw.cmd test` en los módulos afectados y dejar todo verde.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `.../mqadmin/src/test/java/.../RabbitAdminServiceTest.java` |
| Crear | `.../mqadmin/src/test/java/.../MqAdminControllerValidationTest.java` |
| Crear/Modificar | `.../notify/src/test/java/.../NotificationListenerAckTest.java` |
| Modificar | `pom.xml` de notify (dependencia `spring-rabbit-test`) |

## Criterios de aceptación (Definition of Done)
- [ ] `mvnw.cmd test` pasa en `notify` y `mqadmin`.
- [ ] Hay al menos: 1 test de ACK/DLQ, 1 de `RabbitAdminService`, 1 de validación (400).
- [ ] Los tests no dependen de un broker externo cuando sea evitable (usar mocks) o se documenta cómo levantarlo.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-mqadmin && .\mvnw.cmd test
cd ../ms-andesstay-notify && .\mvnw.cmd test
```

## Rollback
```bash
git revert <hash>
```
> Solo añade tests; revertir no afecta la funcionalidad.

## Notas
- Documentar en `docs/` cómo correr la prueba de DLQ end-to-end (payload inválido → cola DLQ + log).
- Si los tests de contexto requieren broker, usar `@Disabled` condicional o un perfil `test` con `spring.rabbitmq.listener.simple.auto-startup=false`.
