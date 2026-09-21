# AndesStay: Plataforma para Reservas de Hostales y Cabañas

Plataforma unificada para la gestión de reservas web, administración de disponibilidad y coordinación de estadías para una red de 20 hostales, cabañas y lodges de turismo de intereses especiales.

## 🏗️ Arquitectura del Sistema

- **Frontend:** SPA en **React 19 + TypeScript + Vite**, autenticación IDaaS con **Azure Entra ID (MSAL)**, control de acceso basado en roles (**Admin**, **Recepcionista**, **Huésped**, **Auditor**), y cliente HTTP **Axios** con inyección automática de Bearer JWT.
- **Backend (BFF):** **Spring Boot + Spring Security** (`ms-andesstay-bff`) ubicado detrás de AWS API Gateway, validando tokens OAuth2/OIDC y delegando a los servicios de dominio.
- **Microservicios de Dominio:**
  - `ms-andesstay-reservations` (Puerto 8081): CRUD de reservas, control de ciclo de vida de estadía y validación de reglas de negocio.
  - `ms-andesstay-catalog` (Puerto 8082): Gestión de unidades, tarifas por noche y disponibilidad.
  - `ms-andesstay-report` (Puerto 8084): Panel de analítica y KPIs en tiempo real (consumidor Kafka).
  - `ms-andesstay-audit` (Puerto 8085): Trazabilidad inmutable y timeline de eventos (consumidor Kafka).
  - `ms-andesstay-notify` (Puerto 8083): Envío de notificaciones y tickets de housekeeping (consumidor RabbitMQ).
- **Base de Datos:** **Supabase PostgreSQL** en AWS US-East-1 (Transaction Mode Pooler en puerto 6543).

---

## 📚 Documentación Técnica

- [Documentación de Integración Frontend - BFF - Microservicios](docs/INTEGRACION_FRONTEND_BACKEND.md): Detalle exhaustivo de contratos de API, resolución de errores de autenticación JWT / MSAL, y optimización de conexiones de base de datos.
- [Implementación MSAL y Axios](Frontend/docs/implementacion_msal_axios.txt): Detalles específicos de la integración con Microsoft Entra ID y flujo de redirección.

---

## 🚀 Inicio Rápido en Entorno Local

### 1. Levantar los Microservicios de Dominio
```bash
# Catálogo (puerto 8082)
cd Backend-api/ms-andesstay-catalog && .\mvnw.cmd spring-boot:run

# Reservas (puerto 8081)
cd Backend-api/ms-andesstay-reservations && .\mvnw.cmd spring-boot:run
```

### 2. Levantar el BFF
```bash
cd Backend-api/ms-andesstay-bff && .\mvnw.cmd spring-boot:run
```
*(Corre en el puerto 8080).*

### 3. Levantar el Frontend
```bash
cd Frontend
npm install
npm run dev
```
*(Disponible en `http://localhost:5173`).*