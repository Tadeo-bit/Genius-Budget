# Checklist de Pruebas — Genius-Budget

> **Sprint 1** · Pruebas funcionales y validación de persistencia  
> **Repositorio:** Genius-Budget (Budget Manager API)  
> **Stack:** Java 17 · Spring Boot 3.2.3 · Maven · In-memory  
> **Puerto:** `8080` · **Swagger:** `/swagger-ui.html`

---

## 0. Preparación del entorno

- [✓] 0.1  Clonar/actualizar repositorio (`git pull`)
- [✓] 0.2  Verificar Java 17+ y Maven 3.8+ instalados
- [✓] 0.3  Ejecutar `mvn test` — **18 tests OK** (17 CampaignController + 1 ApplicationContext)
- [✓] 0.4  Iniciar servidor con `mvn spring-boot:run`
- [✓] 0.5  Abrir `http://localhost:8080/swagger-ui.html` — Swagger carga sin errores
- [✓] 0.6  `GET /` → redirección **302** a Swagger (no 404)

---

## 1. Crear campaña — `POST /api/campaigns`

### 1.1 Casos exitosos

- [✓] 1.1.1  Enviar todos los campos → `201 Created` + `id` autoasignado
- [x] 1.1.2 Enviar solo `name` + `client` → `201` + defaults: `status=draft`, `spent=0.0`, `currency=ARS`
      Resultado obtenido: `400 Bad Request`
      Mensaje: `"Campaign budget must be >= 0"`
      Observación: la implementación actual requiere el campo `budget`.
- [✓] 1.1.3  Crear 2 campañas seguidas → IDs consecutivos (7, 8, …)
- [✓] 1.1.4  `budget=0` → `201` (válido según negocio)
- [✓] 1.1.5  `status=active` → `201` con status preservado

### 1.2 Casos de error (deben devolver 400)

- [✓] 1.2.1  Body `{}` → `400` + `"Campaign payload is required"`
- [✓] 1.2.2  Sin campo `name` → `400` + `"Campaign name is required"`
- [✓] 1.2.3  `name=""` → `400`
- [✓] 1.2.4  Sin campo `client` → `400` + `"Campaign client is required"`
- [✓] 1.2.5  `client=""` → `400`
- [✓] 1.2.6  `budget=-100` → `400` + `"Campaign budget must be >= 0"`
- [✓] 1.2.7  `budget=null` → `400`
- [✓] 1.2.8  JSON mal formado → `400`

---

## 2. Listar campañas — `GET /api/campaigns`

### 2.1 Sin filtros

- [✓] 2.1.1  Listado completo → `200` + **6 campañas** semilla

### 2.2 Filtro por `status`

- [✓] 2.2.1  `?status=active` → 3 campañas (IDs 2, 3, 5)
- [✓] 2.2.2  `?status=paused` → 1 campaña (ID 4)
- [✓] 2.2.3  `?status=closed` → 1 campaña (ID 1)
- [✓] 2.2.4  `?status=draft` → 1 campaña (ID 6)
- [✓] 2.2.5  `?status=ACTIVE` → 3 campañas (normalización mayúsculas)
- [✓] 2.2.6  `?status=inexistente` → `200` + `[]`

### 2.3 Filtro por `client`

- [✓] 2.3.1  `?client=TechStore` → 2 campañas (IDs 5, 6)
- [✓] 2.3.2  `?client=SueñoSimple` (con tilde) → 4 campañas
- [✓] 2.3.3  `?client=suenosimple` (minúscula, sin tilde) → 4 campañas (normalización)
- [✓] 2.3.4  `?client=Inexistente` → `200` + `[]`

### 2.4 Filtros combinados

- [✓] 2.4.1  `?status=active&client=SueñoSimple` → 2 campañas (IDs 2, 3)

### 2.5 Integridad de respuesta

- [✓] 2.5.1  Cada campaña devuelve: `id`, `name`, `client`, `type`, `status`, `budget`, `spent`, `currency`, `startDate`, `endDate`

---

## 3. Obtener campaña por ID — `GET /api/campaigns/{id}`

- [✓] 3.1  ID existente (1–6) → `200` + datos coincidentes
- [✓] 3.2  ID de campaña recién creada → `200`
- [✓] 3.3  ID inexistente (999) → `404` + `{"error": "Campaign not found: 999"}`
- [✓] 3.4  ID tipo inválido (`"abc"`) → inválido

---

## 4. Resumen global — `GET /api/campaigns/summary`

- [✓] 4.1  Estructura: `activeCampaigns`, `totalBudget`, `totalSpent`, `totalAvailable`, `consumptionPercentage`
- [✓] 4.2  Solo campañas **active** → `activeCampaigns=x` (IDs 2, 3, 5 por ejemplo si fueran 3)
- [✓] 4.3 Los cálculos son consistentes:
      - totalAvailable = totalBudget − totalSpent
      - consumptionPercentage = (totalSpent / totalBudget) × 100
- [✓] 4.4  Crear campaña activa nueva → el resumen se actualiza
- [x] 4.5  Sin campañas activas → `activeCampaigns=0`, totales en 0
      - no se puede cambiar el estado de las campañas activas para verificar `activeCampaigns=0`

---

## 5. Resumen por campaña — `GET /api/campaigns/{id}/summary`

- [✓] 5.1  Campaña existente → `200` con: `campaignId`, `campaignName`, `client`, `totalBudget`, `spent`, `remaining`, `percentageUsed`
- [✓] 5.2  Campaña 3: `budget=120000`, `spent=67800` → `remaining=52200`, `percentageUsed=56.5`
- [✓] 5.3  Campaña 6: `spent=0` → `remaining=50000`, `percentageUsed=0`
- [✓] 5.4  Campaña sin gastos → `remaining=budget`, `percentageUsed=0`
- [✓] 5.5  `budget=0`, `spent=0` → `percentageUsed=0`
- [✓] 5.6  ID inexistente → `404`

---

## 6. Listar gastos — `GET /api/campaigns/{id}/expenses`

- [✓] 6.1  Campaña con gastos (ID 1) → `200` + **3 gastos**
- [✓] 6.2  Campaña sin gastos (ID 6) → `200` + `[]`
- [✓] 6.3  ID inexistente → `404`
- [✓] 6.4  Cada gasto tiene: `id`, `campaignId`, `description`, `amount`, `category`, `date`

---

## 7. Registrar gasto — `POST /api/campaigns/{id}/expenses`

### 7.1 Casos exitosos

- [✓] 7.1.1  Gasto válido → `201 Created` + `id` autoasignado + `campaignId` correcto
- [✓] 7.1.2  El campo `spent` de la campaña se incrementa en `amount`
- [✓] 7.1.3  Registrar 2 gastos seguidos → `spent` acumula ambos montos
- [✓] 7.1.4  Gasto con `amount=0` → `201` (se acepta, `spent` no cambia)

### 7.2 Casos de error / borde

- [✓] 7.2.1  Campaña inexistente → `404`
- [✓] 7.2.2  `amount` negativo → **se acepta** (sin validación) — `spent` **disminuye**
- [✓] 7.2.3  `category` inválido (`"otro"`) → **se acepta** (sin validación)
- [✓] 7.2.4  `amount` extremadamente grande → se acepta (sin límite)

---

## 8. Actualizar presupuesto — `PUT /api/campaigns/{id}/budget`

- [✓] 8.1  Budget válido (ID 6: 50 000 → 75 000) → `200` + `budget=75000`
- [✓] 8.2  **`spent` se resetea a 0** — comportamiento documentado, verificar
- [✓] 8.3  Campaña con gastos previos → spent pasa a 0 (pérdida de gastos)
- [✓] 8.4  `budget=0` → `200`
- [✓] 8.5  `budget` negativo → **se acepta** (sin validación)
- [✓] 8.6  `budget=null` → **se acepta** (sin validación)
- [✓] 8.7  ID inexistente → `404`
- [✓] 8.8  Resumen global se actualiza tras cambiar budget de campaña activa

---

## 9. Validación de persistencia (en memoria)

> La persistencia es **en memoria**. Los datos se mantienen mientras el servidor esté corriendo. Se validan contra los endpoints de lectura.

### 9.1 Ciclo de vida

- [✓] 9.1.1  Crear campaña → `GET /api/campaigns` → aparece en el listado
- [✓] 9.1.2  Crear campaña → `GET /api/campaigns/{id}` → existe con datos correctos
- [✓] 9.1.3  Actualizar budget → `GET /api/campaigns/{id}` → refleja el cambio
- [✓] 9.1.4  Registrar gasto → `GET /api/campaigns/{id}/expenses` → aparece el gasto
- [✓] 9.1.5  Registrar gasto → `GET /api/campaigns/{id}` → `spent` se incrementó
- [✓] 9.1.6  Múltiples gastos → resumen global refleja la suma

### 9.2 Relaciones

- [✓] 9.2.1  Gasto con `campaignId=X` aparece solo en `GET /api/campaigns/X/expenses`
- [✓] 9.2.2  Gasto en campaña A → no aparece en `GET /api/campaigns/B/expenses`

---

## 10. Gaps funcionales detectados

> Endpoints **no implementados** que deberían existir para un CRUD completo.

- [ ] 10.1  **No existe** `PUT /api/campaigns/{id}` para editar nombre, cliente, tipo, estado o fechas
- [ ] 10.2  **No existe** `DELETE /api/campaigns/{id}` para eliminar campañas
- [ ] 10.3  **No existe** `PUT /api/campaigns/{id}/expenses/{expenseId}` para editar gastos
- [ ] 10.4  **No existe** `DELETE /api/campaigns/{id}/expenses/{expenseId}` para eliminar gastos

---

## 11. Integración cross-system

### 11.1 CORS

- [✓] 11.1.1  `Origin: http://localhost:8000` → `Access-Control-Allow-Origin: http://localhost:8000` ✅
- [✓] 11.1.2  `Origin: http://localhost:5173` → `Access-Control-Allow-Origin: http://localhost:5173` ✅
- [✓] 11.1.3  `Origin: http://evil.com` → sin header CORS (bloqueado) ✅

### 11.2 Reporting (Python)

- [✓] 11.2.1  Ejecutar `python reporting/extract.py` → `reporting/report.xlsx` generado ✅
- [✓] 11.2.2  Hoja "Campañas": 10 campañas con columnas correctas ✅
- [✓] 11.2.3  Hoja "Resumen": KPIs coinciden con API (`activeCampaigns=5`, `totalBudget=351000`, etc.) ✅

---

## 12. Regresión — tests automatizados

- [✓] 12.1  Ejecutar `mvn test` → **18 tests OK**

### 12.2 Casos no cubiertos por tests actuales

- [✓] 12.2.1  Crear campaña con `name` vacío → 400
- [✓] 12.2.2  Crear campaña con `budget` negativo → 400
- [✓] 12.2.3  Filtrar por `status` + `client` combinados
- [✓] 12.2.4  Listar tras crear campaña nueva (verificar que aparece)
- [✓] 12.2.5  Resumen global tras alta/actualización de campañas
- [✓] 12.2.6  Gasto con `amount` negativo (se acepta — posible bug)
- [✓] 12.2.7  `updateBudget` con valor negativo o nulo
- [✓] 12.2.8  `updateBudget` resetea `spent` a 0
- [✓] 12.2.9  Registrar gasto incrementa `spent`
- [✓] 12.2.10  `GET /` → 302 redirect a Swagger

---

## 13. Registro de bugs / observaciones

| ID  | Tipo        | Descripción | Evidencia |
|-----|-------------|-------------|-----------|
| B01 | Bug         | `PUT /api/campaigns/{id}/budget` resetea `spent=0` sin advertencia → pérdida de gastos históricos | |
| B02 | Bug         | No hay validación en `amount` de gastos → se aceptan negativos | |
| B03 | Bug         | No hay validación en `budget` de `updateBudget` → se aceptan `null` y negativos | |
| B04 | Bug         | No hay validación en `category` de gastos → se acepta cualquier string | |
| B05 | Gap         | Falta endpoint `PUT /api/campaigns/{id}` para editar campaña | |
| B06 | Gap         | Falta endpoint `DELETE /api/campaigns/{id}` para eliminar campaña | |
| B07 | Gap         | Falta `PUT`/`DELETE` para gastos individuales | |
| B08 | Observación | `amount=0` en gasto se acepta — validar si tiene sentido de negocio | |
| B09 | Observación | Persistencia en memoria → todos los datos se pierden al reiniciar el servidor | |
| B10 | Observación | No hay método `PUT` para modificar el estado de una campaña | |

---

## Resumen de la sesión

| Sección | Items | Completados |
|---------|-------|-------------|
| 0. Preparación del entorno | 6 | 6 / 6 ✅ |
| 1. Crear campaña | 13 | 12 / 13 (Problema con 1.1.2) |
| 2. Listar / filtrar | 12 | 12 / 12 ✅ |
| 3. Obtener campaña por ID | 4 | 4 / 4 ✅ |
| 4. Resumen global | 5 | 4 / 5 (4.5 no testeable sin endpoint para cambiar estado) |
| 5. Resumen por campaña | 6 | 6 / 6 ✅ |
| 6. Listar gastos | 4 | 4 / 4 ✅ |
| 7. Registrar gasto | 6 | 6 / 6 ✅ |
| 8. Actualizar presupuesto | 8 | 8 / 8 ✅ |
| 9. Persistencia | 8 | 8 / 8 ✅ |
| 10. Gaps funcionales | 4 | — (diagnóstico) |
| 11. Integración cross-system | 5 | 5 / 5 ✅ |
| 12. Regresión | 10 | 10 / 10 ✅ |
| 13. Bugs / observaciones | 10 | — (diagnóstico) |
| **Total checks funcionales** | **~91** | **86 / 91 ✅ (94.5%)** |
| **Bugs encontrados** | **4** | B01–B04 |
| **Gaps funcionales** | **4** | B05–B07, B10 |
