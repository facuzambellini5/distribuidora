# Documentación de API — Distribuidora

## Convenciones generales

- **Base URL:** `/api`
- **Autenticación:** Bearer JWT (`Authorization: Bearer <token>`), excepto `POST /api/auth/login` y Swagger (`/swagger-ui/**`, `/v3/api-docs/**`).
- **Formato de fechas:**
  - `LocalDate`: ISO-8601 `YYYY-MM-DD` (ej. `2026-11-19`)
  - `LocalDateTime`: ISO-8601 `YYYY-MM-DDTHH:mm:ss`
- **Modelo de inventario:** El stock se gestiona **exclusivamente por camión** (`delivery_stock` y `truck_stock_movements`). No existe inventario central ni alertas de bajo stock.
- **Formato de errores (`ApiErrorResponse`):**
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "Descripción del error",
    "path": "/api/...",
    "timestamp": "2026-11-19T23:30:00"
  }
  ```
- **Códigos HTTP comunes:**
  - `200 OK` / `201 Created` / `204 No Content`
  - `400 Bad Request`: error de validación (`@Valid`) o regla de negocio (`BusinessRuleException`)
  - `401 Unauthorized`: token faltante, inválido o expirado
  - `403 Forbidden`: usuario autenticado sin el permiso requerido (`@PreAuthorize`) o sin acceso al camión/reparto
  - `404 Not Found`: entidad inexistente (`EntityNotFoundException`)
  - `409 Conflict`: recurso duplicado (`DuplicateResourceException` / `DataIntegrityViolationException`)

---

## 1. Autenticación

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Autentica usuario y devuelve `{ token, tokenType, expiresIn, username }`. |
| `POST` | `/api/auth/logout` | Autenticado | Cierra sesión (204). |
| `GET` | `/api/auth/me` | Autenticado | Devuelve perfil actual `{ id, fullName, email, roleId, roleName, permissions[], truckId, truckLicensePlate }`. |

---

## 2. Roles y Permisos

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/api/roles` | `users:manage` | Lista los roles configurados por sistema (`OWNER`, `ADMIN`, `VENDEDOR`, `REPARTIDOR`, `DEPOSITO`) con `id`, `name`, `description` y `permissions: [{ code, name, description }]`. |

### Permisos asignados a `DEPOSITO`
- `products:read`, `products:write` (crear/editar productos, categorías y precios)
- `trucks:read`
- `stock:read`, `stock:write` (gestionar stock y devoluciones en camiones)

---

## 3. Usuarios

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/api/users` | `users:manage` | Crea usuario (`201`). |
| `GET` | `/api/users` | `users:manage` | Lista usuarios (`200`). |
| `GET` | `/api/users/{id}` | `users:manage` | Detalle de usuario (`200`). |
| `PUT` | `/api/users/{id}` | `users:manage` | Actualiza usuario y rol (`200`). |
| `PATCH` | `/api/users/{id}/activate` | `users:manage` | Activa usuario (`204`). |
| `DELETE` | `/api/users/{id}` | `users:manage` | Desactiva usuario (`204`). |

---

## 4. Productos y Categorías

| Método | Ruta | Permiso | Parámetros |
|---|---|---|---|
| `POST` | `/api/products` | `products:write` | Body `ProductRequest` (`name`, `unitPrice`, `unit`, `categoryId`, `sku`, `description`) |
| `GET` | `/api/products` | `products:read` | `active` (boolean, opc.), `categoryId` (Long, opc.) |
| `GET` | `/api/products/{id}` | `products:read` | `id` |
| `PUT` | `/api/products/{id}` | `products:write` | `id`, Body `ProductRequest` |
| `PATCH` | `/api/products/{id}/activate` | `products:write` | `id` |
| `DELETE` | `/api/products/{id}` | `products:write` | `id` |
| `POST` | `/api/categories` | `products:write` | Body `CategoryRequest` |
| `GET` | `/api/categories` | `products:read` | `active` (boolean, opc.) |
| `GET` | `/api/categories/{id}` | `products:read` | `id` |
| `PUT` | `/api/categories/{id}` | `products:write` | `id`, Body `CategoryRequest` |
| `PATCH` | `/api/categories/{id}/activate` | `products:write` | `id` |
| `DELETE` | `/api/categories/{id}` | `products:write` | `id` |

---

## 5. Clientes, Direcciones e Historial Consolidado

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/api/customers` | `customers:write` | Crea cliente y direcciones opcionales (`201`). |
| `GET` | `/api/customers` | `customers:read` | Lista clientes (`active` opcional). |
| `GET` | `/api/customers/{id}` | `customers:read` | Detalle de cliente. |
| `GET` | `/api/customers/{id}/history` | `customers:read` o `ledger:read` | **Historial consolidado del cliente:** reúne compras (`ORDER`), entregas (`DELIVERY`), pagos (`PAYMENT`), ajustes (`ADJUSTMENT`), deuda actual (`currentDebt`) y devoluciones (`RETURN`, indicando `truckId` y `truckLicensePlate`). |
| `PUT` | `/api/customers/{id}` | `customers:write` | Actualiza cliente. |
| `PATCH` | `/api/customers/{id}/activate` | `customers:write` | Activa cliente (`204`). |
| `DELETE` | `/api/customers/{id}` | `customers:write` | Desactiva cliente (`204`). |
| `POST` / `GET` / `PUT` / `DELETE` | `/api/customers/{customerId}/addresses[/{addressId}]` | `customers:read` / `customers:write` | CRUD de direcciones de entrega. |

---

## 6. Cuenta Corriente y Pagos

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/api/customers/{customerId}/ledger` | `ledger:read` | Lista movimientos inmutables de cuenta corriente (`CHARGE`, `PAYMENT`, `ADJUSTMENT`). |
| `POST` | `/api/customers/{customerId}/ledger/payments` | `ledger:write` | Registra un pago inmutable (`201`). |
| `POST` | `/api/customers/{customerId}/ledger/adjustments` | `ledger:write` | Registra un ajuste inmutable positivo o negativo (`201`). |

### Request de pago (`LedgerEntryRequest`)
```json
{
  "amount": 15000.00,
  "paymentMethod": "MIXED",
  "cashAmount": 5000.00,
  "transferAmount": 10000.00,
  "orderId": 42,
  "notes": "Cobro parcial en local",
  "idempotencyKey": "pay-42-20261119-01"
}
```
- **Medios permitidos en pago:** `CASH`, `TRANSFER`, `MIXED`.
- **Validaciones:** `amount > 0`. En `MIXED`, `cashAmount > 0`, `transferAmount > 0` y `cashAmount + transferAmount == amount`. Si se repite `idempotencyKey`, devuelve el movimiento existente sin duplicar.
- **Respuesta (`LedgerEntryResponse`):** incluye `id`, `customerId`, `deliveryOrderId`, `orderCode`, `entryType`, `paymentMethod`, `cashAmount`, `transferAmount`, `amount`, `runningBalance`, `createdById`, `createdByName`, `notes`, `createdAt`.

---

## 7. Camiones, Stock y Movimientos por Camión

| Método | Ruta | Permiso | Parámetros / Descripción |
|---|---|---|---|
| `POST` / `GET` / `PUT` / `PATCH` / `DELETE` | `/api/trucks[/{id}[/activate]]` | `trucks:read` / `trucks:write` | CRUD de camiones (`active` opcional en GET). |
| `GET` | `/api/trucks/{truckId}/orders` | `orders:read` | Pedidos asignados al camión. |
| `POST` | `/api/trucks/{truckId}/stock` | `stock:write` | Carga stock en camión y registra movimiento `LOAD` (`201`). |
| `GET` | `/api/trucks/{truckId}/stock` | `stock:read` | Stock cargado del camión para `date` (`YYYY-MM-DD`, default hoy). |
| `GET` | `/api/trucks/{truckId}/stock/history` | `stock:read` | **Historial y saldos del camión:** filtra por `productId`, `fromDate`, `toDate`, `userId`, `movementType` (`LOAD`, `DELIVERY`, `RETURN`). Devuelve `balances` (`quantityLoaded`, `quantityDelivered`, `quantityReturned`, `resultingBalance`) y `movements`. |
| `GET` | `/api/trucks/stock/movements` | `stock:read` | Consulta global de movimientos por camión filtrable por `truckId`, `productId`, `fromDate`, `toDate`, `userId`, `movementType`. |
| `PUT` / `DELETE` | `/api/trucks/{truckId}/stock/{stockId}` | `stock:write` | Actualiza/elimina registro de stock (respetando reservas de pedidos). |

---

## 8. Pedidos, Entregas, Devoluciones, Comprobante PDF e Historiales

### Estados posibles de un pedido (`OrderStatus`)
`PENDING` -> `IN_ROUTE` -> `DELIVERED` (o `CANCELLED` antes de entregar).

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/api/orders` | `orders:write` | Crea pedido (`201`). Si incluye `truckId`, carga sus productos al camión. |
| `GET` | `/api/orders` | `orders:read` | Lista pedidos (`customerId`, `truckId`, `status`). `REPARTIDOR` solo ve su camión. |
| `GET` | `/api/orders/{id}` | `orders:read` | Detalle de pedido con `orderCode`, `amountPaid` y `pendingBalance`. |
| `PUT` | `/api/orders/{id}` | `orders:write` | Edita pedido `PENDING`. |
| `PATCH` | `/api/orders/{id}/in-route` | `orders:deliver` | Cambia estado `PENDING` -> `IN_ROUTE`. |
| `PATCH` | `/api/orders/{id}/delivered` | `orders:deliver` | Registra entrega y pago (total, parcial o cero en cuenta corriente). |
| `POST` | `/api/orders/{id}/returns` | `stock:write` o `orders:deliver` | Registra devolución de mercadería no entregada al camión del pedido (`201`). |
| `GET` | `/api/orders/{id}/receipt/pdf` | `orders:read` | Descarga comprobante PDF (`application/pdf`) del pedido entregado sin volver a cobrar. |
| `GET` | `/api/orders/sales-history` | `orders:read` o `reports:read` | Historial de ventas filtrable por `fromDate`, `toDate`, `dateType` (`CREATED`, `DELIVERED`, `PAID`), `customerId`, `userId`. |
| `GET` | `/api/orders/deliveries` | `orders:read` | Historial de repartos filtrable por `fromDate`, `toDate`, `customerId`, `truckId`, `driverId`, `status`. |
| `PATCH` | `/api/orders/{id}/cancelled` | `orders:write` | Cancela pedido no entregado y revierte carga en camión. |
| `DELETE` | `/api/orders/{id}` | `orders:write` | Cancela pedido (`204`). |

### Entrega y pago (`PATCH /api/orders/{id}/delivered`)
```json
{
  "paymentMethod": "MIXED",
  "cashAmount": 5000.00,
  "transferAmount": 3000.00,
  "amountPaid": 8000.00,
  "idempotencyKey": "deliv-42-attempt-1"
}
```
- **Pago total:** omitir `amountPaid` (en `CASH`/`TRANSFER` cobra el total; en `MIXED`, `cashAmount + transferAmount == totalAmount`).
- **Pago parcial:** indicar `amountPaid < totalAmount` (o `cashAmount`/`transferAmount` menor al total en `CASH`/`TRANSFER`). La diferencia queda automáticamente como saldo en cuenta corriente.
- **Cuenta corriente (importe cero):** `paymentMethod: "CURRENT_ACCOUNT"` sin montos.
- **Idempotencia:** si el pedido ya está `DELIVERED` o se repite `idempotencyKey`, devuelve el resultado existente sin duplicar cargos ni pagos.

### Devolución por camión (`POST /api/orders/{id}/returns`)
```json
{
  "productId": 10,
  "quantity": 3,
  "notes": "Cliente cerró antes de la entrega",
  "idempotencyKey": "ret-42-prod10-1"
}
```
- **Reglas de validación (`400 Bad Request`):**
  - El pedido **debe tener camión asignado** (si `truckId == null`, rechaza con error claro porque no existe stock central).
  - El pedido no puede estar `CANCELLED` ni `DELIVERED`.
  - `quantity > 0` y `quantity <= cantidad pendiente` (`cantidad pedida - devoluciones acumuladas`).
  - No se permite superar lo cargado en el camión ni duplicar devoluciones (`idempotencyKey`).
  - Los movimientos son históricos e inmutables (nunca se sobrescriben ni borran).

---

## 9. Reportes

| Método | Ruta | Permiso | Parámetros |
|---|---|---|---|
| `GET` | `/api/reports/sales` | `reports:read` | `fromDate`, `toDate` (`YYYY-MM-DD`), `groupBy` (`DAY`, `WEEK`, `MONTH`, `YEAR`, `CUSTOM`) |
| `GET` | `/api/reports/products/top-selling` | `reports:read` | `fromDate`, `toDate`, `limit` (1–100, default 10) |
| `GET` | `/api/reports/employees/performance` | `reports:read` | `fromDate`, `toDate`, `role` (opcional) |
| `GET` | `/api/reports/sales/pdf` | `reports:read` | Mismos filtros; devuelve `application/pdf` |
| `GET` | `/api/reports/products/top-selling/pdf` | `reports:read` | Mismos filtros; devuelve `application/pdf` |
| `GET` | `/api/reports/employees/performance/pdf` | `reports:read` | Mismos filtros; devuelve `application/pdf` |
