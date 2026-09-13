# Inventory Management API — Frontend Integration Guide

Base paths: `/v1/seller/inventory` (stock mutations) · `/v1/seller/inventory-locations` (location mutations) · `/v1/seller/inventory-transfers` (transfer mutations) · `POST /graphql` (all reads)  
All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**  
> Use REST only to create, update, or transition inventory state.  
> Use GraphQL for all reads (stock levels, availability, movement history, locations, transfers).  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [Inventory Concepts](#3-inventory-concepts)
4. [Inventory Locations (REST)](#4-inventory-locations-rest)
5. [Create an Inventory Record (REST)](#5-create-an-inventory-record-rest)
6. [Update Inventory Settings (REST)](#6-update-inventory-settings-rest)
7. [Adjust Stock (REST)](#7-adjust-stock-rest)
8. [Reserve Stock (REST)](#8-reserve-stock-rest)
9. [Release a Reservation (REST)](#9-release-a-reservation-rest)
10. [Confirm a Reservation (REST)](#10-confirm-a-reservation-rest)
11. [Stock Transfers (REST)](#11-stock-transfers-rest)
12. [Read Inventory (GraphQL)](#12-read-inventory-graphql)
13. [Availability Query (GraphQL)](#13-availability-query-graphql)
14. [Stock Movement History (GraphQL)](#14-stock-movement-history-graphql)
15. [Concurrency & Overselling Protection](#15-concurrency--overselling-protection)
16. [Error Reference](#16-error-reference)
17. [Quick Reference](#17-quick-reference)

---

## 1. Standard Response Shapes

### REST envelope

```json
{
  "success": true,
  "message": "Human-readable status",
  "data": { ... }
}
```

On error:

```json
{
  "success": false,
  "message": "What went wrong",
  "data": null
}
```

`data` is `null` for void endpoints. `message` is always safe to display.

### GraphQL envelope

```json
{
  "data": {
    "inventory": { ... }
  }
}
```

On error:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Inventory not found",
      "extensions": { "status": 404 }
    }
  ]
}
```

Check `errors[0].message` and `errors[0].extensions.status` for GraphQL error handling.

---

## 2. Reference Types

### `InventoryStatus`

| Value | Meaning | UI guidance |
|---|---|---|
| `IN_STOCK` | Available quantity > `lowStockThreshold` | Show "In Stock" badge |
| `LOW_STOCK` | Available quantity ≤ `lowStockThreshold` (and > 0) | Show "Low Stock" warning |
| `OUT_OF_STOCK` | Available quantity = 0 and `allowBackorder = false` | Disable "Add to cart"; show "Out of Stock" |
| `DISABLED` | Inventory record is deactivated (`isActive = false`) | Hide from buyer-facing UI |

> **`availableQuantity` = `onHandQuantity` − `reservedQuantity`**. Always display `availableQuantity` to buyers — never `onHandQuantity`.

### `MovementType`

| Value | Triggered by |
|---|---|
| `RECEIPT` | Stock received (purchase order, initial import) |
| `PURCHASE` | Purchase order placed (incoming) |
| `SALE` | Reservation confirmed on payment |
| `RESERVATION` | Stock reserved for an order |
| `RELEASE` | Reservation released (cart abandoned, order cancelled) |
| `RETURN` | Customer return |
| `ADJUSTMENT` | Manual stock correction |
| `DAMAGE` | Damaged goods removed |
| `LOSS` | Shrinkage / theft |
| `TRANSFER_IN` | Stock received from another location |
| `TRANSFER_OUT` | Stock sent to another location |

### `LocationType`

| Value | Description |
|---|---|
| `WAREHOUSE` | Main storage warehouse |
| `STORE` | Physical retail location |
| `FULFILLMENT_CENTER` | Third-party fulfilment hub |
| `DROPSHIPPER` | Supplier ships directly to buyer |
| `SUPPLIER` | Stock held at supplier |
| `VIRTUAL` | Virtual / digital goods location |

### `TransferStatus`

| Value | Meaning |
|---|---|
| `REQUESTED` | Transfer created; awaiting approval |
| `APPROVED` | Approved; ready to ship |
| `IN_TRANSIT` | Stock in transit |
| `RECEIVED` | Transfer completed; stock levels updated |
| `CANCELLED` | Transfer cancelled; no stock moved |

### TypeScript interfaces

```typescript
interface InventoryRecord {
  publicId:          string;        // UUID — use for all subsequent calls
  productId:         string;        // UUID of the product
  variantId:         string | null; // UUID of the variant, null for product-level
  sellerId:          string;
  locationId:        string;
  onHandQuantity:    number;        // physical stock on shelf
  reservedQuantity:  number;        // held for pending orders
  availableQuantity: number;        // onHand − reserved — show this to buyers
  incomingQuantity:  number;        // expected from purchase orders
  damagedQuantity:   number;        // damaged / unsaleable
  lowStockThreshold: number;        // triggers LOW_STOCK status
  allowBackorder:    boolean;       // accept orders when out of stock
  status:            InventoryStatus;
  isActive:          boolean;
  createdAt:         string;        // ISO-8601 UTC
  updatedAt:         string;
}

interface AvailabilityResult {
  productId:         string;
  variantId:         string | null;
  availableQuantity: number;
  status:            InventoryStatus;
  allowBackorder:    boolean;
}

interface InventoryLocation {
  publicId:     string;
  sellerId:     string;
  storeId:      string | null;
  name:         string;
  code:         string;
  locationType: LocationType;
  status:       string;             // "ACTIVE" | "INACTIVE" | "CLOSED"
  isActive:     boolean;
  createdAt:    string;
}

interface StockMovement {
  id:               string;
  inventoryId:      string;
  movementType:     MovementType;
  quantity:         number;
  previousQuantity: number;
  newQuantity:      number;
  referenceType:    string | null;  // e.g. "RESERVATION", "TRANSFER"
  referenceId:      string | null;  // UUID of the referenced entity
  reason:           string | null;
  performedBy:      string | null;  // UUID of the user who made the change
  createdAt:        string;
}

interface InventoryReservation {
  publicId:    string;
  inventoryId: string;
  orderId:     string | null;
  quantity:    number;
  status:      "ACTIVE" | "CONFIRMED" | "RELEASED" | "EXPIRED";
  reservedAt:  string;
  expiresAt:   string | null;
  releasedAt:  string | null;
}

interface InventoryTransfer {
  publicId:               string;
  sourceLocationId:       string;
  destinationLocationId:  string;
  status:                 TransferStatus;
  requestedBy:            string;
  approvedBy:             string | null;
  completedAt:            string | null;
  createdAt:              string;
  updatedAt:              string;
}
```

---

## 3. Inventory Concepts

### One record per (product, variant, location)

Each `InventoryRecord` tracks stock for a single product (or variant) at a single location. If a seller has 3 warehouses, a product will have up to 3 inventory records. The `availability` GraphQL query aggregates across all locations automatically.

```
Product "Blue Linen Shirt"
  ├── Inventory @ Accra Warehouse     → availableQty: 40
  ├── Inventory @ Kumasi Warehouse    → availableQty: 15
  └── Inventory @ Takoradi Warehouse  → availableQty: 5
                                        ─────────────────
                                        Total available: 60
```

### Reservation lifecycle

```
Buyer adds to cart
        │
        ▼
POST /inventory/{id}/reserve   → reservedQuantity ↑, availableQuantity ↓
        │
   ┌────┴────┐
   │         │
Payment    Cart abandoned /
succeeds   order cancelled
   │         │
   ▼         ▼
POST …/confirm   POST …/release
onHandQuantity ↓    reservedQuantity ↓
reservedQuantity ↓  availableQuantity ↑
```

Reservations with an `expiresAt` are auto-released by the server every 60 seconds. You do not need to poll or release manually on expiry — the scheduler handles it.

### Stock movements are immutable

`StockMovement` records are append-only. They are never updated or deleted. Use them as an audit trail when investigating stock discrepancies.

---

## 4. Inventory Locations (REST)

A location must be created before inventory can be tracked at it. Sellers manage their own locations; each location `code` must be unique per seller.

### Create a location

```
POST /v1/seller/inventory-locations
Authorization: Bearer <accessToken>   ← requires inventory.location.create
Content-Type: application/json
```

**Request body**

```json
{
  "name":         "Accra Main Warehouse",
  "code":         "ACC-WH-01",
  "locationType": "WAREHOUSE",
  "storeId":      null
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `name` | Yes | max 150 characters | Human-readable display name |
| `code` | Yes | max 50 characters | Unique per seller; used as a short identifier |
| `locationType` | Yes | see `LocationType` enum | |
| `storeId` | No | UUID | Link to a physical store; omit for standalone warehouses |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Location created",
  "data": {
    "publicId":     "a1b2c3d4-...",
    "sellerId":     "11223344",
    "storeId":      null,
    "name":         "Accra Main Warehouse",
    "code":         "ACC-WH-01",
    "locationType": "WAREHOUSE",
    "status":       "ACTIVE",
    "isActive":     true,
    "createdAt":    "2026-09-13T08:00:00Z"
  }
}
```

Store the `publicId` — you need it when creating inventory records.

---

### Update a location

All fields are optional — send only what changed. The `code` field cannot be changed after creation.

```
PATCH /v1/seller/inventory-locations/{publicId}
Authorization: Bearer <accessToken>   ← requires inventory.location.update
Content-Type: application/json
```

**Request body**

```json
{
  "name":         "Accra Main Warehouse (Expanded)",
  "locationType": "FULFILLMENT_CENTER"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Location updated",
  "data": { ...InventoryLocation }
}
```

---

### Deactivate a location

Soft-deactivates the location (sets `isActive = false`, `status = INACTIVE`). Existing inventory records at this location are not deleted.

```
DELETE /v1/seller/inventory-locations/{publicId}
Authorization: Bearer <accessToken>   ← requires inventory.location.delete
```

**Response `200`**

```json
{
  "success": true,
  "message": "Location deactivated",
  "data": null
}
```

---

## 5. Create an Inventory Record (REST)

Creates a stock tracking record for a product (or variant) at a specific location. Only one record is allowed per `(productId, variantId, locationId)` combination — a `400` is returned if a duplicate is attempted.

```
POST /v1/seller/inventory
Authorization: Bearer <accessToken>   ← requires inventory.create
Content-Type: application/json
```

**Request body**

```json
{
  "productId":        "9a3c1d2e-...",
  "variantId":        null,
  "locationId":       "a1b2c3d4-...",
  "initialQuantity":  100,
  "lowStockThreshold": 10,
  "allowBackorder":   false,
  "storeId":          null
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `productId` | Yes | UUID | The product's public UUID |
| `variantId` | No | UUID | Omit or set `null` for a product-level (no-variant) record |
| `locationId` | Yes | UUID | Must be a location owned by this seller |
| `initialQuantity` | No | ≥ 0; default `0` | Creates a `RECEIPT` stock movement if > 0 |
| `lowStockThreshold` | No | ≥ 0; default `5` | Status becomes `LOW_STOCK` when `availableQuantity ≤ threshold` |
| `allowBackorder` | No | boolean; default `false` | Accept orders even when `OUT_OF_STOCK` |
| `storeId` | No | UUID | Optional store association |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Inventory created",
  "data": {
    "publicId":          "f1e2d3c4-...",
    "productId":         "9a3c1d2e-...",
    "variantId":         null,
    "sellerId":          "11223344",
    "locationId":        "a1b2c3d4-...",
    "onHandQuantity":    100,
    "reservedQuantity":  0,
    "availableQuantity": 100,
    "incomingQuantity":  0,
    "damagedQuantity":   0,
    "lowStockThreshold": 10,
    "allowBackorder":    false,
    "status":            "IN_STOCK",
    "isActive":          true,
    "createdAt":         "2026-09-13T08:10:00Z",
    "updatedAt":         "2026-09-13T08:10:00Z"
  }
}
```

Store the `publicId` — it is the handle for all subsequent stock operations on this record.

---

## 6. Update Inventory Settings (REST)

Updates configuration fields (thresholds, backorder flag, active state). Does not move stock — use [§7 Adjust Stock](#7-adjust-stock-rest) for quantity changes.

```
PATCH /v1/seller/inventory/{publicId}
Authorization: Bearer <accessToken>   ← requires inventory.update
Content-Type: application/json
```

**Request body** — all fields optional

```json
{
  "lowStockThreshold": 20,
  "allowBackorder":    true,
  "isActive":          true
}
```

| Field | Constraints | Notes |
|---|---|---|
| `lowStockThreshold` | ≥ 0 | Changes the threshold that triggers `LOW_STOCK` |
| `allowBackorder` | boolean | When `true`, buyers can order even at `OUT_OF_STOCK` |
| `isActive` | boolean | Setting `false` disables the record → `DISABLED` status |

**Response `200`**

```json
{
  "success": true,
  "message": "Inventory updated",
  "data": { ...InventoryRecord }
}
```

---

## 7. Adjust Stock (REST)

Manually adjusts `onHandQuantity` by a signed delta. Creates an immutable `StockMovement` record. Use this for receipts, returns, damages, and corrections — not for order reservations (use [§8](#8-reserve-stock-rest) for those).

```
POST /v1/seller/inventory/{publicId}/adjust
Authorization: Bearer <accessToken>   ← requires inventory.adjust
Content-Type: application/json
```

**Request body**

```json
{
  "delta":        50,
  "movementType": "RECEIPT",
  "reason":       "Purchase order PO-2026-0041 received"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `delta` | Yes | Non-zero integer | Positive = add stock; negative = remove stock |
| `movementType` | Yes | see `MovementType` enum | Must be semantically appropriate (e.g. don't use `RECEIPT` for removals) |
| `reason` | No | free text | Displayed in stock movement history |

**Common delta/type combinations**

| Scenario | `delta` | `movementType` |
|---|---|---|
| Stock received from supplier | `+N` | `RECEIPT` |
| Customer return | `+N` | `RETURN` |
| Damaged goods removed | `-N` | `DAMAGE` |
| Shrinkage / theft | `-N` | `LOSS` |
| Manual correction (increase) | `+N` | `ADJUSTMENT` |
| Manual correction (decrease) | `-N` | `ADJUSTMENT` |

**Response `200`**

```json
{
  "success": true,
  "message": "Stock adjusted",
  "data": {
    "id":               "1001",
    "inventoryId":      "f1e2d3c4-...",
    "movementType":     "RECEIPT",
    "quantity":         50,
    "previousQuantity": 100,
    "newQuantity":      150,
    "referenceType":    null,
    "referenceId":      null,
    "reason":           "Purchase order PO-2026-0041 received",
    "performedBy":      "user-uuid-...",
    "createdAt":        "2026-09-13T09:00:00Z"
  }
}
```

**`400`** if the adjustment would result in a negative `onHandQuantity` (e.g. `delta = -200` when only 150 on hand).

---

## 8. Reserve Stock (REST)

Atomically reserves stock for an order. On success, `reservedQuantity` increases and `availableQuantity` decreases by the requested `quantity`. The operation is all-or-nothing — partial reservations are never created.

```
POST /v1/seller/inventory/{publicId}/reserve
Authorization: Bearer <accessToken>   ← requires inventory.reserve
Content-Type: application/json
```

**Request body**

```json
{
  "quantity":  3,
  "orderId":   "order-uuid-...",
  "expiresAt": "2026-09-13T10:00:00Z"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `quantity` | Yes | ≥ 1 | Units to reserve |
| `orderId` | No | UUID | Link to the order — used for audit and event correlation |
| `expiresAt` | No | ISO-8601 UTC | If set, the server auto-releases this reservation after this time |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Stock reserved",
  "data": {
    "publicId":    "r1s2t3u4-...",
    "inventoryId": "f1e2d3c4-...",
    "orderId":     "order-uuid-...",
    "quantity":    3,
    "status":      "ACTIVE",
    "reservedAt":  "2026-09-13T09:05:00Z",
    "expiresAt":   "2026-09-13T10:00:00Z",
    "releasedAt":  null
  }
}
```

Store the reservation `publicId` — you need it to release or confirm the reservation.

**`400 Insufficient stock`** when `availableQuantity < quantity` and `allowBackorder = false`. Show "Sorry, only X units are available" to the buyer.

> **Overselling guarantee**: the reservation uses an atomic database `UPDATE ... WHERE available >= qty`. Even under concurrent load, only one request wins when stock runs out — the others receive an error.

---

## 9. Release a Reservation (REST)

Releases an `ACTIVE` or `CONFIRMED` reservation — returns the reserved units back to `availableQuantity`. Call this when an order is cancelled, a cart is abandoned, or a payment fails.

```
POST /v1/seller/inventory/reservations/{reservationPublicId}/release
Authorization: Bearer <accessToken>   ← requires inventory.release
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Reservation released",
  "data": {
    "publicId":   "r1s2t3u4-...",
    "quantity":   3,
    "status":     "RELEASED",
    "releasedAt": "2026-09-13T09:30:00Z"
  }
}
```

**`400`** if the reservation is already `RELEASED` or `EXPIRED`.

> Expired reservations are auto-released by the server every 60 seconds. You only need to call this endpoint explicitly for immediate cancellation (e.g. user clicks "Cancel order").

---

## 10. Confirm a Reservation (REST)

Confirms an `ACTIVE` reservation on payment success. This commits the stock: `onHandQuantity` and `reservedQuantity` both decrease by the reservation quantity. After confirmation, the stock is permanently deducted — releasing a confirmed reservation does not restore `onHandQuantity`.

```
POST /v1/seller/inventory/reservations/{reservationPublicId}/confirm
Authorization: Bearer <accessToken>   ← requires inventory.reserve
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Reservation confirmed",
  "data": {
    "publicId": "r1s2t3u4-...",
    "quantity": 3,
    "status":   "CONFIRMED"
  }
}
```

**Integration flow with payments**:

```
1. POST /inventory/{id}/reserve          → reservation ACTIVE
2. POST /payments/initiate               → redirect buyer to payment gateway
3. POST /payments/webhook (server-side)  → on PAYMENT_SUCCESS:
   POST /inventory/reservations/{id}/confirm
4. If payment fails:
   POST /inventory/reservations/{id}/release
```

---

## 11. Stock Transfers (REST)

Transfers move stock between two of the seller's locations. The transfer lifecycle is: `REQUESTED → APPROVED → RECEIVED`.

### Request a transfer

```
POST /v1/seller/inventory-transfers
Authorization: Bearer <accessToken>   ← requires inventory.transfer.create
Content-Type: application/json
```

**Request body**

```json
{
  "sourceLocationId":      "a1b2c3d4-...",
  "destinationLocationId": "e5f6a7b8-...",
  "items": [
    {
      "inventoryPublicId": "f1e2d3c4-...",
      "quantity":          20
    },
    {
      "inventoryPublicId": "g9h0i1j2-...",
      "quantity":          5
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `sourceLocationId` | Yes | UUID of the source location |
| `destinationLocationId` | Yes | UUID of the destination location; must differ from source |
| `items` | Yes | At least one item |
| `items[].inventoryPublicId` | Yes | UUID of the inventory record to transfer |
| `items[].quantity` | Yes | ≥ 1 |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Transfer requested",
  "data": {
    "publicId":               "t1r2a3n4-...",
    "sourceLocationId":       "a1b2c3d4-...",
    "destinationLocationId":  "e5f6a7b8-...",
    "status":                 "REQUESTED",
    "requestedBy":            "user-uuid-...",
    "approvedBy":             null,
    "completedAt":            null,
    "createdAt":              "2026-09-13T10:00:00Z",
    "updatedAt":              "2026-09-13T10:00:00Z"
  }
}
```

---

### Approve a transfer

Transitions: `REQUESTED → APPROVED`. Required before stock can be received.

```
POST /v1/seller/inventory-transfers/{publicId}/approve
Authorization: Bearer <accessToken>   ← requires inventory.transfer.approve
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Transfer approved",
  "data": { ...InventoryTransfer (status: "APPROVED") }
}
```

**`400`** if the transfer is not in `REQUESTED` status.

---

### Receive a transfer

Transitions: `APPROVED → RECEIVED`. Updates stock at both the source (deduct) and destination (add) locations. You specify how many units were actually received per item (may differ from the requested quantity due to partial shipments).

```
POST /v1/seller/inventory-transfers/{publicId}/receive
Authorization: Bearer <accessToken>   ← requires inventory.transfer.receive
Content-Type: application/json
```

**Request body**

```json
{
  "items": [
    {
      "transferItemId":   1001,
      "receivedQuantity": 18
    },
    {
      "transferItemId":   1002,
      "receivedQuantity": 5
    }
  ]
}
```

| Field | Notes |
|---|---|
| `transferItemId` | Internal ID of the transfer line item (returned in the transfer items query) |
| `receivedQuantity` | Actual units received; may be less than or equal to the requested `quantity` |

**Response `200`**

```json
{
  "success": true,
  "message": "Transfer received",
  "data": { ...InventoryTransfer (status: "RECEIVED") }
}
```

Stock is updated atomically per item. `TRANSFER_OUT` movements appear at the source; `TRANSFER_IN` movements appear at the destination.

---

### Cancel a transfer

Cancels a `REQUESTED` or `APPROVED` transfer. No stock is moved.

```
POST /v1/seller/inventory-transfers/{publicId}/cancel
Authorization: Bearer <accessToken>   ← requires inventory.transfer.cancel
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Transfer cancelled",
  "data": { ...InventoryTransfer (status: "CANCELLED") }
}
```

**`400`** if the transfer is already `RECEIVED` or `CANCELLED`.

---

## 12. Read Inventory (GraphQL)

All inventory reads go through `POST /graphql`. Requires authentication.

### Get a single inventory record

```graphql
query GetInventory($publicId: ID!) {
  inventory(publicId: $publicId) {
    publicId
    productId
    variantId
    locationId
    onHandQuantity
    reservedQuantity
    availableQuantity
    incomingQuantity
    damagedQuantity
    lowStockThreshold
    allowBackorder
    status
    isActive
    createdAt
    updatedAt
  }
}
```

**Variables**

```json
{ "publicId": "f1e2d3c4-..." }
```

Returns `null` if not found or not owned by the authenticated seller.

---

### List the seller's inventory

Returns all active inventory records for the authenticated seller, paginated, newest first.

```graphql
query MyInventory($page: Int, $size: Int) {
  myInventory(page: $page, size: $size) {
    publicId
    productId
    variantId
    locationId
    onHandQuantity
    availableQuantity
    status
    lowStockThreshold
    allowBackorder
    updatedAt
  }
}
```

**Variables**

```json
{ "page": 0, "size": 20 }
```

---

### List inventory locations

Returns all active locations for the authenticated seller.

```graphql
query MyLocations {
  inventoryLocations {
    publicId
    name
    code
    locationType
    status
    isActive
    createdAt
  }
}
```

---

### List stock transfers

```graphql
query MyTransfers($page: Int, $size: Int) {
  inventoryTransfers(page: $page, size: $size) {
    content {
      publicId
      sourceLocationId
      destinationLocationId
      status
      requestedBy
      approvedBy
      completedAt
      createdAt
    }
    totalElements
    totalPages
    currentPage
    hasNextPage
  }
}
```

**Variables**

```json
{ "page": 0, "size": 20 }
```

---

## 13. Availability Query (GraphQL)

The `availability` query is the primary source of truth for buyer-facing stock display. It aggregates `availableQuantity` across **all** of the seller's active locations for a given product (or variant). It is cached server-side for 5 minutes.

```graphql
query CheckAvailability($productId: ID!, $variantId: ID) {
  availability(productId: $productId, variantId: $variantId) {
    productId
    variantId
    availableQuantity
    status
    allowBackorder
  }
}
```

**Variables — product-level (no variant)**

```json
{ "productId": "9a3c1d2e-..." }
```

**Variables — variant-level**

```json
{
  "productId": "9a3c1d2e-...",
  "variantId": "c4d5e6f7-..."
}
```

**Response**

```json
{
  "data": {
    "availability": {
      "productId":         "9a3c1d2e-...",
      "variantId":         "c4d5e6f7-...",
      "availableQuantity": 57,
      "status":            "IN_STOCK",
      "allowBackorder":    false
    }
  }
}
```

**UI guidance by status**

| `status` | `allowBackorder` | Show to buyer |
|---|---|---|
| `IN_STOCK` | any | "In Stock" + quantity badge |
| `LOW_STOCK` | any | "Only X left!" warning |
| `OUT_OF_STOCK` | `false` | "Out of Stock" — disable Add to Cart |
| `OUT_OF_STOCK` | `true` | "Available for backorder" — allow Add to Cart |
| `DISABLED` | any | Hide product from listing |

> Always call `availability` at checkout (via `POST /cart/validate`) to confirm stock before payment — the cache may be up to 5 minutes stale. Do not trust a stale availability response for final purchase confirmation.

---

## 14. Stock Movement History (GraphQL)

Returns a paginated, reverse-chronological log of all stock movements for an inventory record. Use this to populate the movement history view in the seller dashboard.

```graphql
query StockHistory($inventoryId: ID!, $page: Int, $size: Int) {
  inventoryMovements(inventoryId: $inventoryId, page: $page, size: $size) {
    content {
      id
      movementType
      quantity
      previousQuantity
      newQuantity
      referenceType
      referenceId
      reason
      performedBy
      createdAt
    }
    totalElements
    totalPages
    currentPage
    hasNextPage
  }
}
```

**Variables**

```json
{ "inventoryId": "f1e2d3c4-...", "page": 0, "size": 50 }
```

Note: `inventoryId` here is the **internal numeric ID** of the inventory record (returned as `id` in the inventory entity, not the `publicId` UUID). If you only have the `publicId`, first fetch the inventory record and extract its internal ID from the `StockMovement` records.

> Stock movement records are **immutable** — they are never updated or deleted. The history is always complete and accurate.

---

## 15. Concurrency & Overselling Protection

The inventory module uses an atomic SQL `UPDATE` for reservations:

```sql
UPDATE inventory
SET reserved_quantity = reserved_quantity + :qty
WHERE id = :id
  AND (on_hand_quantity - reserved_quantity) >= :qty
  AND is_active = TRUE
```

If this update affects **0 rows**, the request fails immediately with a `400 Insufficient Stock` error — no reservation is created. This guarantees that even with 1,000 simultaneous checkout requests competing for the last 5 units, at most 5 succeed.

**What this means for your UI:**
- On `400 Insufficient Stock`, refresh the availability query and show the buyer the current stock count
- Do not retry automatically — let the buyer choose a lower quantity
- The error `message` field is safe to display: `"Insufficient stock for inventory {id}. Requested: {qty}"`

**`@Version` optimistic locking** on the `Inventory` entity additionally prevents lost updates in any ORM-level operations (settings updates, threshold changes).

---

## 16. Error Reference

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure, negative stock would result, duplicate inventory record |
| `400 Insufficient Stock` | `availableQuantity < requested quantity` on reserve |
| `401 Unauthorized` | Missing or expired access token — refresh and retry |
| `403 Forbidden` | Missing permission (e.g. `inventory.adjust`) or seller does not own this inventory/location |
| `404 Not Found` | Inventory record, location, reservation, or transfer UUID does not exist |
| `400 Invalid Operation` | Status transition not permitted (e.g. releasing an already-released reservation, approving a cancelled transfer) |

For `400` validation errors, `data` may contain a field-error map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "quantity": "must be greater than 0"
  }
}
```

**On `401`**: call `POST /v1/auth/refresh-token`. On failure, redirect to login.  
**On `403`**: the seller does not have the required permission or does not own the resource. This is a client bug, not an edge case to recover from.  
**On `400 Insufficient Stock`**: refresh availability and update the UI before letting the buyer retry.

---

## 17. Quick Reference

```
Inventory Location mutations — REST  (requires SELLER role)
─────────────────────────────────────────────────────────────────────────────
POST   /v1/seller/inventory-locations                🔒  Create location
PATCH  /v1/seller/inventory-locations/{publicId}     🔒  Update location
DELETE /v1/seller/inventory-locations/{publicId}     🔒  Deactivate location

Inventory mutations — REST  (requires SELLER role)
─────────────────────────────────────────────────────────────────────────────
POST   /v1/seller/inventory                          🔒  Create inventory record
PATCH  /v1/seller/inventory/{publicId}               🔒  Update settings (threshold, backorder)
POST   /v1/seller/inventory/{publicId}/adjust        🔒  Adjust stock quantity
POST   /v1/seller/inventory/{publicId}/reserve       🔒  Reserve stock for order
POST   /v1/seller/inventory/reservations/{id}/release   🔒  Release a reservation
POST   /v1/seller/inventory/reservations/{id}/confirm   🔒  Confirm on payment success

Transfer mutations — REST  (requires SELLER role)
─────────────────────────────────────────────────────────────────────────────
POST   /v1/seller/inventory-transfers                🔒  Request a transfer
POST   /v1/seller/inventory-transfers/{id}/approve   🔒  Approve → APPROVED
POST   /v1/seller/inventory-transfers/{id}/receive   🔒  Receive → RECEIVED + stock updated
POST   /v1/seller/inventory-transfers/{id}/cancel    🔒  Cancel transfer

Reads — GraphQL  (all require authentication)
─────────────────────────────────────────────────────────────────────────────
query inventory(publicId)                       🔒  Single inventory record
query myInventory(page, size)                   🔒  Seller's inventory list
query availability(productId, variantId)        🔒  Aggregated availability (cached 5 min)
query inventoryMovements(inventoryId, page, size)  🔒  Movement history (immutable ledger)
query inventoryLocations                        🔒  Seller's active locations
query inventoryTransfers(page, size)            🔒  Transfer list

🔒 = requires Authorization: Bearer <accessToken>

Permission codes required per operation
─────────────────────────────────────────────────────────────────────────────
inventory.read              — read inventory records (GraphQL)
inventory.create            — create inventory records
inventory.update            — update settings
inventory.adjust            — adjust stock quantities
inventory.reserve           — reserve / confirm stock
inventory.release           — release reservations
inventory.history.read      — view stock movement history
inventory.location.read     — view locations
inventory.location.create   — create locations
inventory.location.update   — update locations
inventory.location.delete   — deactivate locations
inventory.transfer.create   — request transfers
inventory.transfer.approve  — approve transfers
inventory.transfer.receive  — receive transfers
inventory.transfer.cancel   — cancel transfers
```
