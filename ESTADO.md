# Estado del repositorio — Genius-Budget

> Reporte de estado generado el 2026-06-21. Para la documentación funcional y de uso, ver [README.md](README.md).

## Resumen

API REST (Budget Manager) para la gestión de presupuestos de campañas de marketing. Backend del ecosistema Genius que expone los datos de campañas y gastos consumidos por el Dashboard.

| Campo | Valor |
|---|---|
| Repositorio | `Tadeo-bit/Genius-Budget` |
| Stack | Java 17 · Spring Boot 3.2.3 · Maven |
| Documentación API | Swagger UI (`/swagger-ui.html`) |
| Persistencia | En memoria (sin base de datos) |
| Puerto | `8080` |

## Estado de Git

| Campo | Valor |
|---|---|
| Rama actual | `dev` |
| Rama por defecto | `main` |
| Cambios sin commitear | Ninguno (working tree limpio) |
| Sincronización | Al día con `origin/dev` |
| Último commit | `c12e2e7 — feat(budget): agrega endpoints PUT/{id} y PATCH/{id}/status para editar campañas` |

## Estado funcional

**Implementado y operativo:**

- Aplicación Spring Boot arrancable con `mvn spring-boot:run`.
- Endpoints de campañas: listado, filtro por estado/cliente, detalle, resumen, gastos, alta, **edición completa** y **cambio de estado**.
- Capa completa: `controller` · `service` · `repository` · `model` · `exception`.
- Manejo global de errores (`GlobalExceptionHandler`).
- Documentación interactiva con Swagger / springdoc-openapi.
- Pruebas presentes: `CampaignControllerTest`, `BudgetManagerApplicationTests`.

**Consideraciones / pendientes:**

- Los datos se cargan en memoria y se reinician en cada arranque; no hay persistencia en base de datos.
- Sin autenticación ni autorización en los endpoints.

## Dependencias con otros repos

- Es consumido por **Genius-Dashboard** (espera esta API en `localhost:8080`).
- También consultado por el panel admin de **Genius-Landings**.

## Cómo ejecutar

```bash
mvn spring-boot:run
# API:     http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

**Requisitos:** Java 17+ y Maven 3.8+.

## Historial de correcciones

### 2026-06-23 — BM-F01/BM-F02

**Problema detectado**

- Camila no podia crear campanas nuevas porque no existia endpoint `POST /api/campaigns`.
- El filtro por estado en `GET /api/campaigns?status=...` usaba el campo incorrecto (`type` en lugar de `status`).
- El filtro por cliente no estaba implementado en Budget Manager, pero era consumido por otros modulos.
- El resumen de presupuesto calculaba mal el campo `remaining`.

**Modificacion realizada**

- Se implemento `POST /api/campaigns` con validaciones basicas y valores por defecto (`status=draft`, `spent=0`, `currency=ARS`).
- Se unifico el listado en un filtro combinado por `status` y `client` (`GET /api/campaigns?status=&client=`).
- Se corrigio la logica de filtro por estado para usar `campaign.status`.
- Se corrigio `remaining = budget - spent` en el resumen.
- Se agregaron pruebas de controlador para filtro por cliente, filtro por estado y alta de campana.

### 2026-06-23 — BM-F03 (Ajuste de DoD)

**Problema detectado**

- El filtro por cliente no contemplaba tildes/acentos, por lo que `SueñoSimple` podia no coincidir con datos cargados como `SuenoSimple`.
- Las validaciones de alta de campana devolvian 404 al usar `RuntimeException`, cuando correspondia 400 para errores de request.

**Modificacion realizada**

- Se normalizaron strings con eliminacion de diacriticos para filtros (`client`/`status`) y asegurar coincidencias como `SueñoSimple`.
- Las validaciones de negocio en creacion de campana ahora lanzan `IllegalArgumentException`.
- Se agrego handler especifico para `IllegalArgumentException` con HTTP 400 en el `GlobalExceptionHandler`.
- Se agregaron pruebas para filtro por cliente con acento y alta invalida con respuesta 400.

### 2026-06-24 — BM-F04 (Acceso raiz sin Whitelabel)

**Problema detectado**

- Al abrir `http://localhost:8080/` se mostraba 404/Whitelabel, generando confusion aunque la API estuviera operativa.

**Modificacion realizada**

- Se agrego `HomeController` para redirigir la ruta raiz `/` hacia `swagger-ui.html`.
- Se verifico que `GET /` retorna `302` y que `GET /api/campaigns` sigue respondiendo `200`.

**Commit y rama**

- Rama: `fix/bm-campaign-create-client-filter`
- Commit: `5225d30` — `fix(budget): redirige la ruta raíz a swagger para evitar 404`

### 2026-06-24 — BM-F05: CORS para Genius-Landings y Genius-Dashboard

**Problema detectado**

- `GET /api/campaigns` desde `http://localhost:8000` (Genius-Landings) y `http://localhost:5173` (Genius-Dashboard) era bloqueado por política CORS: `No 'Access-Control-Allow-Origin' header is present`.

**Cambios realizados**

- Se agregó `@CrossOrigin(origins = {"http://localhost:8000", "http://localhost:5173"})` en `CampaignController.java`.

**Verificaciones**

- `curl -H "Origin: http://localhost:8000"` a `/api/campaigns` devuelve cabecera `Access-Control-Allow-Origin`.
- La página de clientes de Genius-Landings carga campañas sin errores CORS tras reiniciar el servidor.

---

### 2026-06-25 — Fix de datos: cliente `SuenoSimple` → `SueñoSimple`

**Problema detectado**

- Los datos hardcodeados en `CampaignRepository.java` usaban `"SuenoSimple"` (sin ñ) mientras que el CRM usaba `"SueñoSimple"` (con ñ). El Dashboard mostraba ambos clientes como distintos en el selector de filtro.

**Cambios realizados**

- `src/main/java/com/genius/budgetmanager/repository/CampaignRepository.java`: corregidos los 4 registros de campaña de SueñoSimple.
- `src/test/java/com/genius/budgetmanager/controller/CampaignControllerTest.java`: actualizadas las 4 aserciones.

**Verificaciones**

- `GET /api/campaigns` devuelve `"client": "SueñoSimple"` en todas las campañas del cliente.
- El Dashboard muestra un único cliente `SueñoSimple` en el selector.

---

### 2026-06-30 — Edición de campañas: nuevos endpoints `PUT /{id}` y `PATCH /{id}/status`

**Problema detectado**

- No existían endpoints para editar los datos de una campaña ni para cambiar su estado desde el Dashboard.

**Cambios realizados**

- `CampaignRepository.java`: método `updateCampaign(id, patch)` con actualización parcial de campos.
- `CampaignService.java`: métodos `updateCampaign(id, patch)` y `updateStatus(id, status)`.
- `CampaignController.java`:
  - `PUT /api/campaigns/{id}` — actualiza todos los campos de una campaña.
  - `PATCH /api/campaigns/{id}/status` — cambia solo el estado (`{"status": "active"}`).

**Estado funcional**

- Endpoints operativos: listado, filtro, detalle, resumen, gastos, alta, edición completa y cambio de estado.
- Requiere reinicio del servidor para activar los nuevos endpoints.

**Verificaciones**

- `PATCH /api/campaigns/1/status` con `{"status": "paused"}` devuelve 200 con campaña actualizada.
- `PUT /api/campaigns/1` con payload completo devuelve 200.

---

### 2026-07-03 — Fix Swagger PATCH /campaigns/{id}/status (commit `174b5df`)

**Cambio realizado**

- `CampaignController.java`: reemplazado `Map<String, String>` por el record tipado `StatusUpdateRequest(String status)` como `@RequestBody` del endpoint `PATCH /api/campaigns/{id}/status`.
- Eliminado el import `java.util.Map`.

**Motivo**

Swagger generaba `additionalProp1`, `additionalProp2`, `additionalProp3` al usar `Map<String,String>` como body. El record produce el schema correcto con un único campo `status`.

**Estado Git**

- Commit pusheado a `origin/dev` (`174b5df`).
