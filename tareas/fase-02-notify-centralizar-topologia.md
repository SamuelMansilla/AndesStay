# Tarea 02 — notify: centralizar la topología RabbitMQ

- **Fase / Nº:** 02
- **Indicador(es) de la pauta:** **Indicador 1 (12%)** — nombres centralizados
- **Depende de:** 01
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(notify): centralizar topología RabbitMQ en MessagingProperties`
- **Riesgo de rollback:** bajo (solo configuración)

## Objetivo
Eliminar todo nombre de cola/exchange/binding/routing key escrito como literal en Java. Toda la topología debe leerse desde `application.yml` mediante una clase `@ConfigurationProperties`, en preparación para migrar a la convención `q.<dominio>.<acción>`.

## Alcance
**Incluye**
- Crear `MessagingProperties` (`@ConfigurationProperties(prefix="andesstay.messaging")`).
- Migrar `application.properties` → `application.yml` con el árbol `andesstay.messaging.*`.
- Añadir las claves de los nuevos casos de uso (tickets, documentos) aunque sus beans lleguen en la tarea 03.

**NO incluye**
- Declarar los beans `Queue`/`Exchange`/`Binding` (tarea 03).
- Cambiar el comportamiento de producers/consumers (tareas 04–06).

## Prerrequisitos
- Tarea 01 completada (`notify` compila).

## Pasos
1. Crear la convención de nombres en configuración, sustituyendo los actuales `q.cmd.notify.*`:
   - Exchange: `ex.andesstay.notifications`
   - Colas: `q.notify.email`, `q.notify.sms`, `q.notify.push`, `q.housekeeping.tickets`, `q.docs.generate`
   - DLX: `ex.andesstay.dlx` · DLQ: `q.andesstay.dlq` · key DLQ: `dlq.routing.key`
   - Routing keys: `notify.email`, `notify.sms`, `notify.push`, `housekeeping.ticket`, `docs.generate`
2. Crear `application.yml` (reemplaza `application.properties`) con:
   ```yaml
   spring:
     rabbitmq:
       host: ${RABBITMQ_HOST:localhost}
       port: ${RABBITMQ_PORT:5672}
       username: ${RABBITMQ_USER:guest}
       password: ${RABBITMQ_PASS:guest}
       listener:
         simple:
           retry: { enabled: true, initial-interval: 2000ms, max-attempts: 3 }
   andesstay:
     messaging:
       exchange: ex.andesstay.notifications
       dlx: ex.andesstay.dlx
       dlq: q.andesstay.dlq
       dlq-routing-key: dlq.routing.key
       queues:
         notify-email: q.notify.email
         notify-sms: q.notify.sms
         notify-push: q.notify.push
         housekeeping-tickets: q.housekeeping.tickets
         docs-generate: q.docs.generate
       routing-keys:
         notify-email: notify.email
         notify-sms: notify.sms
         notify-push: notify.push
         housekeeping-ticket: housekeeping.ticket
         docs-generate: docs.generate
   ```
3. Crear `config/MessagingProperties.java` con getters/setters (o `record` anidado) que mapee **todo** el árbol.
4. Añadir `@ConfigurationPropertiesScan` o `@EnableConfigurationProperties(MessagingProperties.class)` en `MsAndesstayNotifyApplication`.
5. Refactorizar `RabbitMQConfig` para inyectar `MessagingProperties` en lugar de `@Value` sueltos (los beans aún se definen en tarea 03; aquí solo se prepara la fuente de datos).

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `Backend-api/ms-andesstay-notify/src/main/resources/application.yml` |
| Crear | `.../notify/config/MessagingProperties.java` |
| Modificar | `.../notify/config/RabbitMQConfig.java` (usar `MessagingProperties`) |
| Modificar | `.../notify/MsAndesstayNotifyApplication.java` |
| Eliminar | `.../notify/src/main/resources/application.properties` |

## Criterios de aceptación (Definition of Done)
- [ ] No existe ningún nombre de cola/exchange/routing key como **literal** en clases Java.
- [ ] Toda la topología se lee de `andesstay.messaging.*`.
- [ ] El servicio compila y arranca igual que antes.
- [ ] Documentado (comentario en YAML) el significado de cada clave.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-notify
.\mvnw.cmd -q clean package -DskipTests
.\mvnw.cmd spring-boot:run
# Revisar en la UI de RabbitMQ que las colas existentes siguen declarándose.
# Buscar literales prohibidos:
Select-String -Path src/**/*.java -Pattern '"q\.|"ex\.|notify\.email'
```

## Rollback
```bash
git revert <hash>
```
> Bajo riesgo: solo cambia la fuente de los nombres, no la topología declarada.

## Notas
- Este cambio de nombres **rompe** la compatibilidad con colas ya creadas en el broker local. Como estamos en desarrollo, borrar las colas viejas en la UI o usar `-p` en compose (volumen limpio).
- Mantener ambos casos (viejos y nuevos) solo si hay despliegues productivos; en este proyecto no se requiere.
