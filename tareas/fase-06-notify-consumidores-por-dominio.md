# Tarea 06 — notify: consumidores agrupados por dominio funcional

- **Fase / Nº:** 06
- **Indicador(es) de la pauta:** **Indicador 4 (15%)** — consumidores `@RabbitListener` agrupados por dominio
- **Depende de:** 04, 05
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `refactor(notify): reorganizar consumidores por dominio funcional`
- **Riesgo de rollback:** bajo

## Objetivo
Reorganizar los `@RabbitListener` en paquetes por **dominio funcional** (notifications, housekeeping, documents, dlq) para que la estructura del código refleje la separación de responsabilidades que pide la pauta.

## Alcance
**Incluye**
- Un paquete por dominio bajo `messaging/`.
- Listeners separados: email, sms, push (notifications); tickets (housekeeping); documentos (documents); DLQ (dlq).
- Cada listener aplica el patrón ACK/DLQ de la tarea 05.

**NO incluye**
- La lógica del broker (ya resuelta en 05).
- Pruebas (tarea 13).

## Prerrequisitos
- Tareas 04 y 05 completadas.

## Pasos
1. Reestructurar:
   ```
   messaging/
     notifications/  EmailListener, SmsListener, PushListener
     housekeeping/   HousekeepingTicketListener
     documents/      DocumentGenerationListener
     dlq/            DeadLetterListener
   ```
2. Mover la lógica de negocio de cada canal a un servicio (`NotificationService.sendEmail()`, etc.) y dejar el listener **delgado** (parsear → delegar → ACK).
3. `DeadLetterListener`: consume `q.andesstay.dlq`, registra en WARN el evento y sus headers (`x-death`, `x-original-queue`, `x-exception-message`); no reencola.
4. Añadir comentario de cabecera en cada listener indicando: dominio, cola, routing key y decisión de ACK.
5. Eliminar `NotificationListener` monolítico.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `.../messaging/notifications/{Email,Sms,Push}Listener.java` |
| Crear | `.../messaging/housekeeping/HousekeepingTicketListener.java` |
| Crear | `.../messaging/documents/DocumentGenerationListener.java` |
| Crear | `.../messaging/dlq/DeadLetterListener.java` |
| Crear | `.../service/NotificationService.java`, `HousekeepingService.java`, `DocumentService.java` |
| Eliminar | `.../service/NotificationListener.java` |

## Criterios de aceptación (Definition of Done)
- [ ] Existe **un paquete por dominio funcional**, con sus listeners.
- [ ] Ningún paquete mezcla dominios.
- [ ] Todos los consumidores usan `@RabbitListener` y el patrón ACK.
- [ ] El listener de DLQ registra los mensajes fallidos.
- [ ] Compila y arranca; el envío end-to-end sigue funcionando.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-notify
.\mvnw.cmd -q clean package -DskipTests
.\mvnw.cmd spring-boot:run
# Enviar por cada canal y observar logs por dominio
```

## Rollback
```bash
git revert <hash>
```
> Reorganización de paquetes; revertir no afecta la topología del broker.

## Notas
- La agrupación por dominio es lo que evalúa explícitamente el indicador 4: nombrar paquetes de forma autoexplicativa (`notifications`, `housekeeping`, `documents`, `dlq`).
