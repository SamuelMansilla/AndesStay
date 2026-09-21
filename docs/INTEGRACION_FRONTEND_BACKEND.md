# Documentación de Integración: Frontend, BFF y Microservicios (AndesStay)

## 1. Visión General y Arquitectura

La plataforma **AndesStay** implementa una arquitectura desacoplada orientada a microservicios bajo el siguiente flujo de comunicación y seguridad:

```
[Frontend SPA (React 19 + MSAL)]
               │
               ▼  (Bearer Token JWT Azure Entra ID)
[AWS API Gateway / Proxy Local]
               │
               ▼
   [ms-andesstay-bff (Port 8080)]  <--- Valida JWT, CORS y orquesta
               │
   ┌───────────┼───────────┬───────────┬───────────┐
   ▼           ▼           ▼           ▼           ▼
[catalog] [reservations] [report]   [audit]    [notify]
 (8082)      (8081)       (8084)     (8085)     (8083 - RabbitMQ)
   │           │           │           │
   └───────────┴─────┬─────┴───────────┘
                     ▼
  [Supabase PostgreSQL (AWS US-East-1)]
```

---

## 2. Cambios Realizados en el Frontend (`Frontend/`)

### A. Capa de Servicios de API (`src/api/`)
Se implementaron servicios tipados que utilizan el cliente singleton `axiosClient` con interceptores de autenticación Bearer token:
- **`reservationsService.ts`:**
  - `getReservations(params)`: Lista reservas con filtros opcionales de estado y fecha.
  - `getReservationById(id)`: Obtiene detalle de reserva.
  - `createReservation(payload)`: Creación de reserva (`guestEmail`, `unitId`, `checkInDate`, `checkOutDate`).
  - `updateReservationStatus(id, status)`: Actualiza estado (`CREADA` $\rightarrow$ `CONFIRMADA` $\rightarrow$ `CHECKIN_PENDIENTE` $\rightarrow$ `EN_ESTADÍA` $\rightarrow$ `CHECKOUT`).
- **`catalogService.ts`:**
  - `getUnits()`: Listado completo de habitaciones, cabañas y lodges.
  - `getUnitById(id)`: Consulta por identificador.
  - `createUnit(unit)`: Registro de nuevas unidades (Admin).
  - `updateUnit(id, unit)`: Edición de tarifas por noche y disponibilidad (`available`).
- **`reportService.ts`:**
  - `getKpis(range)`: Indicadores en tiempo real (ocupación activa, tiempo de ciclo, reservas).
  - `getTopUnits(range)`: Unidades más demandadas y desglose de ingresos.
- **`auditService.ts`:**
  - `getAuditLogs()`: Línea de tiempo inmutable de eventos.
  - `getAuditLogsByActor(actor)`: Filtro por usuario/actor.

### B. Conexión de Pantallas a Endpoints Reales (`src/pages/`)
- **`ReservationsPage.tsx`:** Reemplazó los datos mock por consumo vivo de `reservationsService` y `catalogService`. Implementa selector de unidad dinámico, modal de creación y validación de regla de negocio ("No se puede hacer check-in sin CONFIRMAR").
- **`CatalogPage.tsx`:** Conectado a la API para crear unidades, alternar disponibilidad y actualizar tarifas por noche con sincronización directa en Supabase.
- **`ReportsPage.tsx`:** Métricas reactivas al selector temporal (`last24h`, `last7d`, `last30d`) con datos de KPIs y ranking de unidades.
- **`AuditPage.tsx`:** Timeline inmutable con buscador por usuario, reserva y tipo de evento.
- **`DashboardPage.tsx`:** Panel adaptativo por rol (Admin, Recepcionista, Huésped, Auditor) con consola interactiva de prueba de peticiones seguras.

### C. Resolución de Errores de Autenticación en Frontend
- **Solución al error `timed_out` de MSAL:**
  - *Causa:* Al iniciar sesión solo se solicitaba `User.Read`. Al llamar a la API se intentaba pedir el scope `OT.Create` mediante un `<iframe>` oculto, el cual era bloqueado por las políticas de cookies de terceros del navegador.
  - *Solución:* Se unificó `loginRequest` en `authConfig.ts` para incluir tanto `User.Read` como los scopes del API (`OT.Create`). Así, el usuario otorga consentimiento en el flujo interactivo de login y el token queda guardado de inmediato en `localStorage`.
  - Se agregó además el botón de consentimiento interactivo en `DashboardPage.tsx`.

---

## 3. Cambios Realizados en el BFF (`Backend-api/ms-andesstay-bff/`)

### A. Controladores Nuevos y Actualizados
- **`BffCatalogController.java`:** Homogeneizado a `/api/catalog/units` con métodos reactivos `GET`, `POST` y `PUT /{id}`, propagando la cabecera `Authorization: Bearer <token>`.
- **`BffReportController.java`:** Creado para exponer `GET /api/report/kpis` y `GET /api/report/top-units`, delegando a `ms-andesstay-report` (puerto 8084).
- **`BffAuditController.java`:** Creado para exponer `GET /api/audit` y `GET /api/audit/actor/{actor}`, delegando a `ms-andesstay-audit` (puerto 8085).

### B. Seguridad, Validación JWT y Resolución de Errores
- **Inyección de Dependencias:** Se agregó `@Component` a `JwtRoleConverter.java` para resolver el error de bean no encontrado en Spring Boot.
- **Solución al error `401 Unauthorized`:**
  - *Emisor (Issuer):* Azure Entra ID emite tokens v1 (`sts.windows.net`) por defecto para aplicaciones registradas. Se implementó `TenantIssuerValidator.java` para aceptar tanto `https://login.microsoftonline.com/{tenant}/v2.0` como `https://sts.windows.net/{tenant}/`.
  - *Audiencia (Audience):* Se actualizó `AudienceValidator.java` para aceptar el identificador con y sin prefijo `api://`.
- **Manejador Global de Conectividad (`BffExceptionHandler.java`):**
  - Intercepta fallos de conexión cuando un microservicio de dominio no está corriendo y devuelve un error descriptivo `502 Bad Gateway` en lugar de un `500` genérico.

---

## 4. Optimización de Base de Datos y Supabase Pooler

### Error Diagnosticado:
```text
org.postgresql.util.PSQLException: FATAL: (EMAXCONNSESSION) max clients reached in session mode - max clients are limited to pool_size: 15
```
- **Causa:** El puerto `5432` en el pooler de Supabase corresponde al modo sesión (*Session Mode*), limitado a 15 conexiones totales para todo el proyecto. Con varios microservices abriendo 10 conexiones cada uno, el límite colapsaba de inmediato.
- **Solución Aplicada:**
  - Se migró la cadena de conexión de los microservicios (`catalog`, `reservations`, `report`, `audit`) al puerto **`6543`** (*Transaction Mode* con `prepareThreshold=0`):
    ```properties
    spring.datasource.url=jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
    ```
  - Se ajustó el pool de conexiones de HikariCP para desarrollo eficiente:
    ```properties
    spring.datasource.hikari.maximum-pool-size=3
    spring.datasource.hikari.minimum-idle=1
    spring.datasource.hikari.connection-timeout=20000
    ```

---

## 5. Guía de Ejecución Local

1. **Frontend:**
   ```bash
   cd Frontend
   npm install
   npm run dev
   ```
   *(Acceso en: `http://localhost:5173`)*

2. **BFF:**
   ```bash
   cd Backend-api/ms-andesstay-bff
   .\mvnw.cmd spring-boot:run
   ```
   *(Puerto: `8080`)*

3. **Microservicios de Dominio:**
   - Catálogo: `cd Backend-api/ms-andesstay-catalog && .\mvnw.cmd spring-boot:run` (Puerto `8082`)
   - Reservas: `cd Backend-api/ms-andesstay-reservations && .\mvnw.cmd spring-boot:run` (Puerto `8081`)
   - Reportes: `cd Backend-api/ms-andesstay-report && .\mvnw.cmd spring-boot:run` (Puerto `8084`)
   - Auditoría: `cd Backend-api/ms-andesstay-audit && .\mvnw.cmd spring-boot:run` (Puerto `8085`)
