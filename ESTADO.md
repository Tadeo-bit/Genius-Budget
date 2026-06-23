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
| Rama actual | `main` |
| Rama por defecto | `main` |
| Cambios sin commitear | Ninguno (working tree limpio) |
| Sincronización | Al día con `origin/main` |
| Último commit | `c808bf7 — Add files via upload` |

## Estado funcional

**Implementado y operativo:**

- Aplicación Spring Boot arrancable con `mvn spring-boot:run`.
- Endpoints de campañas: listado, filtro por estado, detalle, resumen, gastos y actualización de presupuesto.
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
