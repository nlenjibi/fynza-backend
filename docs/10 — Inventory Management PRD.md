# 10 — Inventory Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Inventory Management  
**Module ID:** `INVENTORY-10`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Product Management, Seller Management, Store Management, Pricing Management, Authorization, Common

---

# 1. Overview

Inventory Management controls the availability and quantity of sellable products within Fynza.

Its primary responsibility is answering:

> **How many units are available, where are they located, and can they be sold?**

The module manages:

- Inventory items.
- Stock quantities.
- Available stock.
- Reserved stock.
- On-hand stock.
- Incoming stock.
- Stock adjustments.
- Stock movements.
- Inventory reservations.
- Warehouses/locations.
- Low-stock thresholds.
- Inventory status.
- Stock transfers.
- Stock history.
- Inventory synchronization.
- Stock availability queries.

The architectural separation is:

```text
Product
    │
    │ What is being sold?
    ▼
Pricing
    │
    │ How much?
    ▼
Inventory
    │
    │ How many?
    ▼
Cart / Order
```

---

# 2. Goals

Inventory Management should:

1. Track stock accurately.
2. Prevent overselling.
3. Support product variants.
4. Support multiple inventory locations.
5. Support stock reservations.
6. Track stock movements.
7. Support stock adjustments.
8. Provide real-time availability.
9. Support low-stock alerts.
10. Maintain inventory history.
11. Support stock transfers.
12. Integrate with orders.
13. Support cancellation/restocking.
14. Support future warehouse operations.
15. Provide reliable inventory reporting.

---

# 3. Non-Goals

Inventory Management does not own:

- Product definitions.
- Product descriptions.
- Product categories.
- Product pricing.
- Payments.
- Customers.
- Seller verification.
- Orders.
- Shipping execution.

Inventory may integrate with those modules but should not own their domain data.

---

# 4. Core Inventory Model

```text
Product
   │
   └── Variant
          │
          ▼
      Inventory Item
          │
          ├── Warehouse
          │
          ├── On-Hand Stock
          ├── Reserved Stock
          ├── Available Stock
          └── Incoming Stock
```

---

# 5. Inventory Quantity Concepts

Fynza should distinguish between:

```text
On-Hand
Reserved
Available
Incoming
Damaged
Unavailable
```

Recommended formula:

```text
Available Stock
=
On-Hand Stock
-
Reserved Stock
-
Unavailable Stock
```

For a basic implementation:

```text
Available
=
On-Hand
-
Reserved
```

Example:

```text
On-Hand      = 100
Reserved     = 20
Available    = 80
```

---

# 6. INVENTORY-FR-001 — Inventory Item

An inventory item represents stock for a sellable product or variant.

Example:

```text
Inventory Item
-------------------------
SKU: LAP-001
Warehouse: Accra
On Hand: 100
Reserved: 20
Available: 80
```

Variants should normally be the actual stock-tracked unit.

---

# 7. Variant-Level Inventory

For products with variants:

```text
T-Shirt
│
├── Small / Black
│     └── Stock: 20
│
├── Medium / Black
│     └── Stock: 15
│
└── Large / Black
      └── Stock: 8
```

Inventory should reference:

```text
variantId
```

rather than attempting to infer inventory from product attributes.

---

# 8. Product-Level Inventory

Simple products without variants can reference:

```text
productId
```

Example:

```text
Product
USB Cable
SKU: USB-001

Inventory:
50 units
```

Recommended rule:

```text
Simple Product → product inventory
Variant Product → variant inventory
```

---

# 9. INVENTORY-FR-002 — On-Hand Stock

On-hand stock represents physical or system-confirmed stock currently owned/controlled by the seller.

Example:

```text
Warehouse Stock = 250
```

It should not include:

- Future purchase orders.
- Unconfirmed incoming inventory.
- Cancelled stock.
- Stock that has already been removed.

---

# 10. INVENTORY-FR-003 — Reserved Stock

Reserved stock represents inventory temporarily committed to customers but not yet finalized.

Example:

```text
On-Hand  = 100
Reserved = 15
Available = 85
```

Reservations can occur during:

- Checkout.
- Order creation.
- Payment processing.
- Flash sales.

---

# 11. Inventory Reservation

Recommended lifecycle:

```text
Reservation
    │
    ├──► ACTIVE
    │
    ├──► CONFIRMED
    │
    ├──► RELEASED
    │
    └──► EXPIRED
```

Example:

```text
Customer
   │
   ▼
Checkout
   │
   ▼
Reserve 2 units
   │
   ├── Payment succeeds → CONFIRMED
   │
   ├── Payment fails → RELEASED
   │
   └── Timeout → EXPIRED
```

---

# 12. INVENTORY-FR-004 — Reservation Expiration

Reservations should have expiration times.

```text
reservedAt
expiresAt
```

Example:

```text
Reserved:
10:00

Expires:
10:15
```

Expired reservations must release stock.

However, inventory availability should not depend solely on a scheduled job.

When calculating available inventory:

```text
available =
onHand - activeReservations
```

Expired reservations should be ignored/released transactionally.

---

# 13. Preventing Overselling

This is one of the most important inventory requirements.

Bad:

```text
Read stock
↓
Check stock
↓
Update stock
```

Two requests can read the same quantity.

Example:

```text
Stock = 1

Customer A → sees 1
Customer B → sees 1

A buys
B buys

Result = -1
```

Inventory operations must be atomic.

---

# 14. Atomic Reservation

Recommended logic:

```sql
UPDATE inventory
SET reserved_quantity = reserved_quantity + :quantity
WHERE id = :inventoryId
  AND available_quantity >= :quantity;
```

Then verify:

```text
rowsAffected == 1
```

If zero:

```text
INSUFFICIENT_STOCK
```

Alternative approaches:

- Pessimistic locking.
- Optimistic locking.
- Atomic conditional updates.

For high-concurrency inventory, atomic database operations are strongly recommended.

---

# 15. INVENTORY-FR-005 — Stock Adjustment

Authorized users may adjust stock.

Examples:

```text
+50 received
-5 damaged
-2 lost
+10 correction
```

Every adjustment must create an inventory movement.

Never silently change stock.

---

# 16. Stock Movement

Model:

```text
StockMovement
-------------------------
id
inventoryId
movementType
quantity
previousQuantity
newQuantity
referenceType
referenceId
reason
performedBy
createdAt
```

Movement types:

```text
PURCHASE
RECEIPT
SALE
RESERVATION
RELEASE
RETURN
ADJUSTMENT
DAMAGE
LOSS
TRANSFER_IN
TRANSFER_OUT
```

---

# 17. Inventory Ledger

The stock movement history effectively becomes an inventory ledger.

Example:

```text
Opening Balance       +100
Purchase               +50
Sale                    -10
Return                   +2
Damage                   -3
Adjustment               +5
--------------------------------
Current                 144
```

This provides auditability.

---

# 18. INVENTORY-FR-006 — Warehouses

Fynza should support multiple inventory locations.

Example:

```text
Seller
│
├── Accra Warehouse
├── Kumasi Warehouse
└── Tamale Warehouse
```

Model:

```text
InventoryLocation
-------------------------
id
sellerId
storeId
name
code
type
status
addressId
createdAt
updatedAt
```

---

# 19. Inventory Location Types

Possible types:

```text
WAREHOUSE
STORE
FULFILLMENT_CENTER
DROPSHIPPER
SUPPLIER
VIRTUAL
```

Initially Fynza may only need:

```text
WAREHOUSE
STORE
```

The model should allow future expansion.

---

# 20. INVENTORY-FR-007 — Multi-Location Stock

A product can exist at multiple locations.

```text
Laptop A

Accra:
100

Kumasi:
50

Tamale:
25
```

Total:

```text
175 units
```

But availability should be location-aware.

---

# 21. Location-Aware Availability

Example:

```text
Customer location:
Accra

Nearest warehouse:
Accra

Stock:
100
```

The system may choose:

```text
Accra Warehouse
```

rather than:

```text
Kumasi Warehouse
```

This decision may later be owned by Fulfillment/Shipping.

Inventory should expose location-level stock.

---

# 22. INVENTORY-FR-008 — Stock Transfer

Stock can move between locations.

Example:

```text
Accra
100 units

       ↓ transfer 20

Accra
80 units

Kumasi
70 units
```

A transfer should create two movements:

```text
TRANSFER_OUT
TRANSFER_IN
```

---

# 23. Transfer Lifecycle

```text
REQUESTED
   │
   ▼
APPROVED
   │
   ▼
IN_TRANSIT
   │
   ▼
RECEIVED
   │
   └──► CANCELLED
```

For an initial implementation:

```text
TRANSFERRED
```

may be sufficient.

---

# 24. INVENTORY-FR-009 — Low Stock

Inventory should support thresholds.

Example:

```text
Current Stock: 8
Low Stock Threshold: 10
```

System status:

```text
LOW_STOCK
```

Possible inventory statuses:

```text
IN_STOCK
LOW_STOCK
OUT_OF_STOCK
DISABLED
```

---

# 25. Out-of-Stock

Important architectural distinction:

Product Management should not own stock state.

Instead:

```text
Inventory
   │
   ▼
Availability
```

Product can consume:

```text
availableQuantity
availabilityStatus
```

Therefore:

```text
Product.status ≠ OUT_OF_STOCK
```

Inventory determines availability.

---

# 26. Product Availability

Recommended representation:

```text
Availability
-------------------------
inventoryTracked
availableQuantity
availabilityStatus
allowBackorder
```

Example:

```text
availableQuantity = 0
allowBackorder    = false
```

Result:

```text
OUT_OF_STOCK
```

---

# 27. INVENTORY-FR-010 — Backorders

Future functionality may allow:

```text
allowBackorder = true
```

Example:

```text
Stock = 0

Customer orders = 2

Backorder = 2
```

Backorder management should eventually integrate with Procurement/Fulfillment.

Do not implement unrestricted backorders in the first version.

---

# 28. INVENTORY-FR-011 — Returns

When an order item is returned:

```text
Order
   │
   ▼
Return
   │
   ▼
Inventory
```

Returned stock may become:

```text
AVAILABLE
DAMAGED
INSPECTION_REQUIRED
```

Example:

```text
Returned:
1 unit

Inspection:
PASS

Inventory:
+1 available
```

---

# 29. Damaged Stock

Damaged inventory should be tracked separately.

Example:

```text
On Hand:
100

Available:
90

Damaged:
10
```

Do not simply delete damaged units from history.

Record:

```text
DAMAGE
```

movement.

---

# 30. Inventory Reconciliation

Physical inventory may differ from system inventory.

Example:

```text
System:
100

Physical:
97

Difference:
-3
```

Administrator performs:

```text
Stock Adjustment
-3
Reason:
Physical count reconciliation
```

This creates an auditable adjustment.

---

# 31. Inventory Counting

Future functionality:

```text
Inventory Count
-------------------------
location
countDate
performedBy
status
```

Workflow:

```text
Start Count
    ↓
Count Items
    ↓
Compare
    ↓
Review Differences
    ↓
Approve Adjustments
```

---

# 32. INVENTORY-FR-012 — Inventory History

Users with permission should see:

```text
Current Stock
Stock Changes
Reservations
Transfers
Returns
Adjustments
```

Example:

```text
Date        Type          Qty
--------------------------------
Sep 01      Receipt       +100
Sep 02      Sale           -10
Sep 03      Damage          -2
Sep 04      Return          +1
```

---

# 33. Inventory Data Model

## Inventory

```text
Inventory
-------------------------
id
publicId
productId
variantId
sellerId
storeId
locationId
onHandQuantity
reservedQuantity
incomingQuantity
damagedQuantity
lowStockThreshold
allowBackorder
version
createdAt
updatedAt
```

---

# 34. Inventory Location

```text
InventoryLocation
-------------------------
id
publicId
sellerId
storeId
name
code
type
status
addressId
createdAt
updatedAt
```

---

# 35. Inventory Reservation

```text
InventoryReservation
-------------------------
id
publicId
inventoryId
orderId
quantity
status
reservedAt
expiresAt
releasedAt
createdAt
updatedAt
```

---

# 36. Stock Movement

```text
StockMovement
-------------------------
id
inventoryId
movementType
quantity
previousQuantity
newQuantity
referenceType
referenceId
reason
performedBy
createdAt
```

---

# 37. Inventory Transfer

```text
InventoryTransfer
-------------------------
id
publicId
sourceLocationId
destinationLocationId
status
requestedBy
approvedBy
completedAt
createdAt
updatedAt
```

---

# 38. Inventory Transfer Item

```text
InventoryTransferItem
-------------------------
id
transferId
inventoryId
quantity
receivedQuantity
createdAt
updatedAt
```

---

# 39. Inventory Quantity Formula

Recommended:

```text
availableQuantity =
    onHandQuantity
    - reservedQuantity
    - unavailableQuantity
```

If damaged stock is included in on-hand:

```text
availableQuantity =
    onHandQuantity
    - reservedQuantity
    - damagedQuantity
```

The exact accounting model must be standardized early.

---

# 40. Recommended Quantity Model

Prefer explicit buckets:

```text
onHand
reserved
damaged
available
incoming
```

But avoid unnecessary duplication.

A safer source-of-truth model can be:

```text
onHand
reserved
incoming
```

and derive:

```text
available = onHand - reserved
```

Damaged stock can either:

1. Be excluded from on-hand, or
2. Be explicitly tracked.

Choose one consistent accounting model.

---

# 41. Inventory API — REST

## Public

```text
GET /products/{productId}/availability
GET /products/{productId}/variants/{variantId}/availability
```

## Seller

```text
GET  /seller/inventory
GET  /seller/inventory/{id}

POST /seller/inventory
PATCH /seller/inventory/{id}

POST /seller/inventory/{id}/adjust
POST /seller/inventory/{id}/reserve
POST /seller/inventory/{id}/release

GET /seller/inventory/{id}/movements
```

## Locations

```text
GET    /seller/inventory-locations
POST   /seller/inventory-locations
PATCH  /seller/inventory-locations/{id}
DELETE /seller/inventory-locations/{id}
```

## Transfers

```text
POST /seller/inventory-transfers
GET  /seller/inventory-transfers
GET  /seller/inventory-transfers/{id}

POST /seller/inventory-transfers/{id}/approve
POST /seller/inventory-transfers/{id}/receive
POST /seller/inventory-transfers/{id}/cancel
```

---

# 42. GraphQL Queries

```graphql
inventory(productId: ID!, variantId: ID): Inventory

availability(
    productId: ID!
    variantId: ID
    locationId: ID
): Availability

inventoryMovements(
    inventoryId: ID!
): [StockMovement!]!

inventoryLocations: [InventoryLocation!]!
```

---

# 43. GraphQL Mutations

```graphql
adjustInventory
reserveInventory
releaseInventory

createInventoryLocation
updateInventoryLocation

createInventoryTransfer
approveInventoryTransfer
receiveInventoryTransfer
cancelInventoryTransfer
```

---

# 44. GraphQL Structure

```text
inventory/
└── graphql/
    ├── resolver/
    │   ├── InventoryResolver
    │   ├── AvailabilityResolver
    │   ├── InventoryLocationResolver
    │   └── InventoryTransferResolver
    │
    ├── input/
    │   ├── InventoryFilterInput
    │   ├── AdjustInventoryInput
    │   ├── ReserveInventoryInput
    │   └── CreateInventoryTransferInput
    │
    └── payload/
        ├── InventoryPayload
        ├── AvailabilityPayload
        ├── InventoryMovementPayload
        └── InventoryTransferPayload
```

---

# 45. Authorization

Recommended permissions:

```text
inventory.read
inventory.create
inventory.update
inventory.adjust
inventory.reserve
inventory.release
inventory.history.read

inventory.location.read
inventory.location.create
inventory.location.update
inventory.location.delete

inventory.transfer.create
inventory.transfer.approve
inventory.transfer.receive
inventory.transfer.cancel
```

Seller access must be scoped to:

```text
Seller
  ↓
Store
  ↓
Inventory
```

---

# 46. Domain Events

Recommended events:

```text
INVENTORY_CREATED
INVENTORY_UPDATED

STOCK_RECEIVED
STOCK_ADJUSTED
STOCK_RESERVED
STOCK_RELEASED
STOCK_COMMITTED
STOCK_RETURNED

STOCK_TRANSFER_CREATED
STOCK_TRANSFER_APPROVED
STOCK_TRANSFER_RECEIVED
STOCK_TRANSFER_CANCELLED

LOW_STOCK_DETECTED
OUT_OF_STOCK
STOCK_REPLENISHED
```

---

# 47. Event Flow

Example sale:

```text
Order
 │
 ▼
Inventory Reservation
 │
 ▼
STOCK_RESERVED
 │
 ▼
Payment
 │
 ├── Success
 │     ▼
 │  STOCK_COMMITTED
 │
 └── Failure
       ▼
   STOCK_RELEASED
```

---

# 48. Order Integration

Inventory should integrate with Order without owning the order.

Recommended flow:

```text
Cart
 │
 ▼
Checkout
 │
 ▼
Inventory Reservation
 │
 ▼
Order Created
 │
 ▼
Payment
 │
 ▼
Inventory Commit
```

If payment fails:

```text
Payment Failed
      │
      ▼
Release Reservation
```

---

# 49. Inventory and Cart

The cart should display availability.

Example:

```text
Product:
Laptop

Available:
3
```

Customer attempts:

```text
Quantity:
5
```

Result:

```text
Only 3 units available.
```

The frontend must not be the authority.

---

# 50. Inventory and Checkout

At checkout:

```text
1. Validate product.
2. Validate variant.
3. Resolve price.
4. Check inventory.
5. Reserve inventory.
6. Create order.
7. Process payment.
8. Commit/release inventory.
```

Exact transaction orchestration should be handled by the Checkout/Order architecture.

---

# 51. Inventory and Pricing

Inventory does not need pricing.

```text
Inventory
  = quantity

Pricing
  = monetary value
```

At checkout:

```text
Product
 ├── Pricing → GHS 1,299
 └── Inventory → 5 available
```

---

# 52. Database Views

Inventory dashboard may require:

```text
Inventory
+
Product
+
Variant
+
Store
+
Location
```

Use read models:

```text
inventory/
└── query/
    ├── projection/
    └── view/
```

Example:

```text
InventoryDashboardView
-------------------------
sku
productName
variantName
locationName
onHand
reserved
available
incoming
status
```

Do not turn this view into a write entity.

---

# 53. Caching

Availability may be cached for product browsing.

Example:

```text
inventory:product:{productId}
inventory:variant:{variantId}
availability:{variantId}:{locationId}
```

However:

> **Cache must never be the authoritative source for stock reservation.**

Reservation must use the authoritative inventory store.

---

# 54. Concurrency Strategy

Recommended:

```text
Database
   │
   ├── Atomic updates
   ├── Optimistic locking
   └── Transactions
```

Use:

```java
@Version
private Long version;
```

where appropriate.

For reservation:

```text
available >= requestedQuantity
```

must be checked atomically with the stock update.

---

# 55. Idempotency

Inventory operations must support idempotency.

Example:

```text
Payment event:
PAY-123

Inventory receives:
STOCK_COMMIT
```

If the event is delivered twice:

```text
First → commit 2 units
Second → ignored
```

Use:

```text
eventId
referenceId
idempotencyKey
```

to prevent duplicate stock movements.

---

# 56. Failure Handling

Potential failure:

```text
Payment succeeds
       │
       ▼
Inventory commit fails
```

This must not be ignored.

Fynza should use:

- Transactional outbox.
- Retry mechanisms.
- Idempotent event consumers.
- Reconciliation.
- Dead-letter handling where appropriate.

Do not attempt to solve distributed consistency by blindly wrapping everything in one database transaction.

---

# 57. Reconciliation

Inventory reconciliation jobs should detect:

```text
Order says:
2 units committed

Inventory says:
0 units committed
```

or:

```text
Reservation exists
but order does not.
```

These inconsistencies should be flagged for resolution.

---

# 58. Security Requirements

Inventory is financially sensitive.

Requirements:

- Sellers may only modify their inventory.
- Customers cannot modify inventory.
- Inventory adjustments require authorization.
- Administrative adjustments require audit.
- Stock movements cannot be silently deleted.
- Historical movements are immutable.
- Reservations must be validated server-side.
- Client-supplied quantities must be validated.
- APIs must be protected against replay where appropriate.

---

# 59. Audit

Inventory adjustments should record:

```text
who
what
when
where
why
```

Example:

```text
User:
Admin 123

Action:
STOCK_ADJUSTMENT

SKU:
LAP-001

Change:
-5

Reason:
Damaged during transportation

Time:
2026-09-04 13:30
```

---

# 60. Testing

## Unit Tests

Test:

- Available quantity calculation.
- Reservation.
- Release.
- Commit.
- Stock adjustment.
- Low-stock calculation.
- Status transitions.
- Transfer calculations.
- Validation.

---

# 61. Repository Tests

Test:

- Atomic reservation.
- Concurrent reservations.
- Optimistic locking.
- Inventory queries.
- Stock movement persistence.
- Location queries.
- Reservation expiration.

---

# 62. Concurrency Tests

Critical test:

```text
Stock = 10

100 customers
attempt to purchase
1 unit each
```

Expected:

```text
10 successful reservations
90 failures
```

Never:

```text
Negative inventory
```

---

# 63. Security Tests

Test:

- Seller A accessing Seller B inventory.
- Seller A modifying Seller B stock.
- Customer adjusting inventory.
- Unauthorized reservation.
- Unauthorized transfer.
- Unauthorized stock adjustment.
- GraphQL authorization bypass.
- REST authorization bypass.

---

# 64. Integration Tests

Test:

```text
Product
   ↓
Variant
   ↓
Inventory
   ↓
Reservation
   ↓
Order
   ↓
Payment
   ↓
Commit
```

Also:

```text
Payment failure
      ↓
Release
      ↓
Available stock restored
```

---

# 65. E2E Test

### Successful Purchase

```text
Customer
   │
   ▼
Product Page
   │
   ▼
Add to Cart
   │
   ▼
Checkout
   │
   ▼
Reserve Stock
   │
   ▼
Payment
   │
   ▼
Order Confirmed
   │
   ▼
Commit Stock
```

### Failed Payment

```text
Checkout
   │
   ▼
Reserve
   │
   ▼
Payment Failed
   │
   ▼
Release
   │
   ▼
Stock Available Again
```

---

# 66. Module Structure

```text
inventory/
├── config/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
├── validator/
├── exception/
├── policy/
├── event/
├── query/
│   ├── projection/
│   └── view/
├── graphql/
│   ├── resolver/
│   ├── input/
│   └── payload/
└── InventoryModule.java
```

Tests:

```text
test/
└── inventory/
    ├── service/
    ├── repository/
    ├── policy/
    ├── controller/
    ├── graphql/
    ├── integration/
    ├── security/
    └── concurrency/
```

---

# 67. Recommended Entities

Core:

```text
Inventory
InventoryLocation
InventoryReservation
StockMovement
InventoryTransfer
InventoryTransferItem
```

Future:

```text
InventoryCount
InventoryCountItem
StockBatch
StockLot
SerialNumber
PurchaseOrder
SupplierInventory
```

---

# 68. Future Batch/Lot Tracking

For products such as:

- Food.
- Medicine.
- Cosmetics.
- Industrial products.

Fynza may eventually require:

```text
Batch
Lot
Expiry Date
Manufacturing Date
```

Example:

```text
Product
  │
  ├── Batch A
  │     └── Expires: 2027
  │
  └── Batch B
        └── Expires: 2028
```

This should be added only when the business requires it.

---

# 69. Future Serial Number Tracking

Electronics may require serial numbers.

Example:

```text
Laptop
SKU: LAP-001

Serial:
SN123
SN124
SN125
```

This can later support:

- Warranty.
- Returns.
- Theft prevention.
- Device tracking.

---

# 70. Milestones

## M1 — Inventory Foundation

- [ ] Inventory entity.
- [ ] Product/variant linkage.
- [ ] Quantity tracking.
- [ ] Availability calculation.
- [ ] Basic CRUD.
- [ ] Validation.

## M2 — Reservations

- [ ] Reservation entity.
- [ ] Reserve stock.
- [ ] Release stock.
- [ ] Commit stock.
- [ ] Expiration.
- [ ] Idempotency.

## M3 — Stock Ledger

- [ ] Stock movements.
- [ ] Adjustments.
- [ ] Returns.
- [ ] Damage.
- [ ] Audit history.

## M4 — Multi-Location

- [ ] Warehouses.
- [ ] Store locations.
- [ ] Location stock.
- [ ] Transfers.

## M5 — Commerce Integration

- [ ] Cart integration.
- [ ] Checkout integration.
- [ ] Order integration.
- [ ] Payment integration.
- [ ] Automatic release.
- [ ] Stock commit.

## M6 — Optimization

- [ ] Inventory read models.
- [ ] Availability caching.
- [ ] Low-stock notifications.
- [ ] Reconciliation.
- [ ] Performance optimization.

## M7 — Advanced Inventory

- [ ] Backorders.
- [ ] Batch tracking.
- [ ] Lot tracking.
- [ ] Serial numbers.
- [ ] Inventory counting.
- [ ] Supplier integration.

---

# 71. Definition of Done

- [ ] Inventory entity implemented.
- [ ] Product inventory supported.
- [ ] Variant inventory supported.
- [ ] On-hand stock supported.
- [ ] Reserved stock supported.
- [ ] Available stock supported.
- [ ] Inventory reservations implemented.
- [ ] Reservation expiration implemented.
- [ ] Atomic reservation implemented.
- [ ] Stock movements implemented.
- [ ] Stock adjustments implemented.
- [ ] Inventory history implemented.
- [ ] Low-stock thresholds implemented.
- [ ] Out-of-stock detection implemented.
- [ ] Inventory locations implemented.
- [ ] Stock transfers implemented where required.
- [ ] Returns integrated.
- [ ] Cart integration implemented.
- [ ] Checkout integration implemented.
- [ ] Order integration implemented.
- [ ] Idempotency implemented.
- [ ] Audit implemented.
- [ ] Domain events implemented.
- [ ] REST API implemented.
- [ ] GraphQL API implemented.
- [ ] Read models implemented where required.
- [ ] Caching implemented safely.
- [ ] Concurrency tests implemented.
- [ ] Security tests pass.
- [ ] Integration tests pass.
- [ ] E2E tests pass.
- [ ] Documentation completed.

---

# 72. Architectural Boundaries

Fynza's commerce foundation should now look like:

```text
┌────────────────────────┐
│ Category               │
│ What category?         │
└────────────┬───────────┘
             │
             ▼
┌────────────────────────┐
│ Product                │
│ What is being sold?    │
└────────────┬───────────┘
             │
       ┌─────┴─────┐
       ▼           ▼
┌─────────────┐ ┌─────────────┐
│ Pricing     │ │ Inventory   │
│ How much?   │ │ How many?   │
└──────┬──────┘ └──────┬──────┘
       │               │
       └───────┬───────┘
               ▼
          Cart / Checkout
               │
               ▼
             Order
```

---

# 73. Key Architectural Principle

> **Inventory Management owns stock availability and stock movements. It does not own products, prices, orders, or payments.**

The most important separation is:

```text
Product
    = What is it?

Category
    = Where is it classified?

Pricing
    = How much does it cost?

Inventory
    = How many can be sold?

Order
    = What did the customer purchase?
```

Inventory must remain the **authoritative source of stock availability**.

The frontend, Product module, Cart, and Search may display inventory information, but none of them should be allowed to independently determine whether a unit can actually be sold.

---

# 74. Recommended Commerce Flow

```text
Seller
   │
   ▼
Store
   │
   ▼
Product
   │
   ├──────────► Category
   │
   ├──────────► Pricing
   │
   └──────────► Inventory
                      │
                      ▼
                   Customer
                      │
                      ▼
                     Cart
                      │
                      ▼
                   Checkout
                      │
              ┌───────┴────────┐
              ▼                ▼
           Pricing          Inventory
           Resolve          Reserve
              │                │
              └───────┬────────┘
                      ▼
                    Order
                      │
                      ▼
                   Payment
                      │
             ┌────────┴────────┐
             ▼                 ▼
          Success             Failure
             │                 │
             ▼                 ▼
        Commit Stock       Release Stock
```

This establishes the foundation for the next module:

**`11 — Cart Management PRD`**