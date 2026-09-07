# 13 — Order Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Order Management  
**Module ID:** `ORDER-13`  
**Priority:** Critical  
**Status:** Planned  
**Architecture:** Enterprise Modular Monolith / Microservice-Ready  
**Backend:** Java + Spring Boot  
**API:** REST + GraphQL  
**Frontend:** Next.js + TypeScript  

---

# 1. Executive Summary

Order Management is responsible for creating, maintaining, tracking, and managing customer orders after checkout.

An order represents a **commercial transaction request that has been submitted by the customer**.

The Order module is the authoritative source for:

- Order identity
- Order number
- Customer ownership
- Order items
- Historical product snapshots
- Historical pricing snapshots
- Quantities
- Order totals
- Order status
- Order lifecycle
- Seller-specific order segments
- Cancellation state
- Order history
- Order notes
- Order metadata
- Order events

It does **not** own:

- Product catalog
- Current product price
- Inventory
- Payment execution
- Shipping execution
- Refund processing
- Customer authentication

Those responsibilities remain with their respective modules.

---

# 2. Core Principle

> **An Order is an immutable commercial record of what the customer submitted for purchase at a particular point in time.**

This distinction is critical.

```text
Product
= What is currently being sold?

Pricing
= What does it currently cost?

Inventory
= What is currently available?

Cart
= What does the customer intend to buy?

Checkout
= Can the intended purchase become a transaction?

Order
= What did the customer actually submit?

Payment
= How was the order paid?

Fulfillment
= How is the order delivered?
```

---

# 3. Order Position in Commerce Architecture

```text
Category
    ↓
Product
    ├───────────────┐
    ↓               ↓
Pricing         Inventory
    │               │
    └──────┬────────┘
           ↓
          Cart
           ↓
       Checkout
           ↓
      Order Management
           │
     ┌─────┼─────────────┐
     ↓     ↓             ↓
 Payment  Fulfillment   Notification
     │     │
     ↓     ↓
  Payment Shipping
```

The Order module coordinates with these domains but does not absorb their responsibilities.

---

# 4. Problem Statement

Once a customer submits checkout, the system needs a durable record of exactly what was purchased.

Current product data can change:

- Product name can change.
- Product description can change.
- SKU can change.
- Price can change.
- Product can be archived.
- Variant attributes can change.
- Seller/store information can change.

Therefore, an order cannot simply reference the current Product and Pricing records.

Instead:

```text
Current Product
       ↓
Checkout
       ↓
Order Snapshot
       ↓
Immutable Commercial History
```

The order must preserve the relevant historical information.

---

# 5. Goals

## Primary Goals

1. Create orders from successful checkout.
2. Generate unique order numbers.
3. Preserve immutable item snapshots.
4. Track order lifecycle.
5. Support multi-item orders.
6. Support multi-store/seller orders.
7. Track order totals.
8. Track order status history.
9. Support cancellation workflows.
10. Support customer order history.
11. Support seller order views.
12. Integrate with Payment.
13. Integrate with Inventory.
14. Integrate with Shipping/Fulfillment.
15. Support REST and GraphQL.
16. Provide auditability.
17. Provide reliable domain events.
18. Prevent duplicate order creation.

---

# 6. Non-Goals

Order Management does not own:

- Authentication
- Authorization definitions
- Product catalog
- Pricing rules
- Inventory quantities
- Payment gateway integration
- Payment settlement
- Shipping carrier integration
- Notification delivery
- Reviews
- Promotions
- Customer profile management

The Order module may consume information from those modules.

---

# 7. Order Lifecycle

A recommended lifecycle is:

```text
DRAFT
  ↓
PENDING_PAYMENT
  ↓
PAID
  ↓
PROCESSING
  ↓
FULFILLING
  ↓
SHIPPED
  ↓
DELIVERED
  ↓
COMPLETED
```

Alternative terminal states:

```text
CANCELLED
FAILED
EXPIRED
```

A more complete model:

```text
                    ┌──────────────┐
                    │    DRAFT     │
                    └──────┬───────┘
                           ↓
                  ┌─────────────────┐
                  │ PENDING_PAYMENT │
                  └────────┬────────┘
                           ↓
                       ┌───────┐
                       │ PAID  │
                       └───┬───┘
                           ↓
                    ┌────────────┐
                    │ PROCESSING │
                    └─────┬──────┘
                          ↓
                     FULFILLING
                          ↓
                       SHIPPED
                          ↓
                      DELIVERED
                          ↓
                     COMPLETED
```

Cancellation can occur only in permitted states.

---

# 8. Order Status

Recommended statuses:

```text
DRAFT
PENDING_PAYMENT
PAID
PROCESSING
FULFILLING
SHIPPED
DELIVERED
COMPLETED
CANCELLED
FAILED
EXPIRED
```

The exact lifecycle should be implemented through a state machine rather than arbitrary status updates.

---

# 9. Why a State Machine?

Avoid:

```java
order.setStatus(OrderStatus.SHIPPED);
```

without validating the current state.

Instead:

```text
Current State
      ↓
Transition Request
      ↓
State Machine
      ↓
Validate Transition
      ↓
New State
```

Example:

```text
PENDING_PAYMENT → PAID
```

is valid.

But:

```text
PENDING_PAYMENT → DELIVERED
```

is invalid.

---

# 10. Order State Transition Rules

Example:

| From | Allowed |
|---|---|
| DRAFT | PENDING_PAYMENT, CANCELLED |
| PENDING_PAYMENT | PAID, FAILED, EXPIRED, CANCELLED |
| PAID | PROCESSING, CANCELLED |
| PROCESSING | FULFILLING, CANCELLED |
| FULFILLING | SHIPPED, CANCELLED |
| SHIPPED | DELIVERED |
| DELIVERED | COMPLETED |
| COMPLETED | Terminal |
| CANCELLED | Terminal |
| FAILED | Terminal |
| EXPIRED | Terminal |

Business-specific rules may further restrict cancellation.

---

# 11. Order Identity

Each order should have:

```text
id
publicId
orderNumber
```

Example:

```text
Internal ID:
UUID

Public ID:
opaque identifier

Order Number:
FYN-2026-00001234
```

The order number should be human-readable.

Do not expose database sequences as public identifiers.

---

# 12. Order Number Requirements

Order number must be:

- unique
- human-readable
- searchable
- stable
- non-editable
- suitable for customer support

Recommended:

```text
FYN-2026-00001234
```

The generation strategy should work safely under concurrent order creation.

---

# 13. Order Entity

Recommended:

```text
Order
--------------------------------
id
publicId
orderNumber
customerId
currency
status
subtotal
discountAmount
taxAmount
shippingAmount
totalAmount
billingAddressId
shippingAddressId
checkoutId
placedAt
paidAt
cancelledAt
completedAt
createdAt
updatedAt
version
```

---

# 14. Money Handling

Never use:

```java
double
float
```

for monetary values.

Use:

```java
BigDecimal
```

and an explicit currency.

Example:

```text
subtotal = 1500.00
currency = GHS
```

Every monetary value must have a clearly defined currency context.

---

# 15. Order Totals

Recommended fields:

```text
subtotal
discountAmount
taxAmount
shippingAmount
totalAmount
```

Formula:

```text
totalAmount =
    subtotal
    - discountAmount
    + taxAmount
    + shippingAmount
```

Depending on future requirements, additional values may include:

```text
serviceFee
platformFee
sellerFee
giftWrapFee
roundingAdjustment
```

---

# 16. Order Item

Order items are immutable commercial snapshots.

```text
OrderItem
--------------------------------
id
orderId
productId
variantId

productNameSnapshot
skuSnapshot
productImageSnapshot
variantSnapshot

unitPrice
quantity
discountAmount
taxAmount
subtotal
totalAmount

sellerId
storeId

createdAt
```

---

# 17. Why Snapshots Are Required

Suppose the customer buys:

```text
Laptop
Price: GHS 10,000
```

Later:

```text
Product renamed
Laptop Pro X
Price: GHS 12,000
```

The historical order must still display:

```text
Laptop
GHS 10,000
```

Therefore:

```text
OrderItem
├── productId
├── productNameSnapshot
├── skuSnapshot
├── unitPrice
└── selectedVariantSnapshot
```

The Product module remains the current source of truth, while the Order preserves historical truth.

---

# 18. Snapshot vs Reference

Use both.

```text
OrderItem
├── productId
├── variantId
├── sellerId
├── storeId
│
├── productNameSnapshot
├── skuSnapshot
├── variantSnapshot
├── priceSnapshot
└── imageSnapshot
```

References provide traceability.

Snapshots provide historical accuracy.

---

# 19. Quantity

Order quantity must be positive.

Example:

```text
quantity >= 1
```

Once the order is confirmed, the original purchased quantity should not be silently changed.

Adjustments should occur through explicit business workflows such as:

- Cancellation
- Return
- Refund
- Replacement

---

# 20. Seller / Store Ownership

Fynza may support multiple sellers.

Example:

```text
Customer
   ↓
Order
   ├── Seller A
   │     ├── Product 1
   │     └── Product 2
   │
   └── Seller B
         └── Product 3
```

The customer sees:

```text
Order #FYN-2026-000123

Seller A
├── Product 1
└── Product 2

Seller B
└── Product 3
```

---

# 21. Parent Order vs Seller Order

For marketplace architecture, distinguish:

```text
Customer Order
        │
        ├── Seller Order A
        │      ├── Item
        │      └── Item
        │
        └── Seller Order B
               └── Item
```

Recommended concepts:

```text
Order
SellerOrder
OrderItem
```

The customer sees one order.

Operational systems can work with seller-specific orders.

---

# 22. SellerOrder

Recommended:

```text
SellerOrder
--------------------------------
id
publicId
orderId
sellerId
storeId
status
subtotal
discountAmount
taxAmount
shippingAmount
totalAmount
createdAt
updatedAt
```

This allows independent fulfillment.

Example:

```text
Order
└── SellerOrder A → Shipped
└── SellerOrder B → Processing
```

The parent order can therefore represent the customer's purchase while seller orders represent operational fulfillment units.

---

# 23. Seller Order Status

Seller order status may differ from parent order status.

Example:

```text
Parent Order:
PROCESSING

Seller A:
SHIPPED

Seller B:
FULFILLING
```

The parent order status should be derived or orchestrated from child states according to business rules.

Do not assume every seller must fulfill at the same time.

---

# 24. Address Snapshot

Customer addresses can change after an order.

Therefore, order delivery information should be preserved.

Recommended:

```text
OrderAddress
--------------------------------
id
orderId
type
recipientName
phone
addressLine1
addressLine2
city
region
country
postalCode
createdAt
```

Types:

```text
BILLING
SHIPPING
```

Do not rely solely on:

```text
customerAddressId
```

because the customer may later edit that address.

---

# 25. Customer Address vs Order Address

Customer:

```text
CustomerAddress
```

represents the current reusable address.

Order:

```text
OrderAddress
```

represents the historical address used for the purchase.

Therefore:

```text
Customer Address
      ↓
Checkout
      ↓
Order Address Snapshot
```

---

# 26. Order Notes

Optional:

```text
OrderNote
--------------------------------
id
orderId
authorId
type
content
createdAt
```

Types may include:

```text
CUSTOMER_NOTE
SELLER_NOTE
SUPPORT_NOTE
SYSTEM_NOTE
INTERNAL_NOTE
```

Internal notes must never be exposed to customers.

---

# 27. Order Metadata

Use carefully.

```text
OrderMetadata
--------------------------------
id
orderId
key
value
```

Metadata should not become a replacement for proper domain fields.

Use it for:

- integration references
- external IDs
- controlled extensibility

Avoid storing core business data as arbitrary JSON when a proper field is appropriate.

---

# 28. Order History

Every important status transition should be recorded.

```text
OrderStatusHistory
--------------------------------
id
orderId
fromStatus
toStatus
reason
changedBy
createdAt
```

Example:

```text
PENDING_PAYMENT
      ↓
PAID
```

History:

```text
from: PENDING_PAYMENT
to: PAID
changedBy: SYSTEM
```

---

# 29. Order Event Log

Status history and domain events are different.

### Status History

Business audit trail.

### Domain Event

Message for other systems.

For example:

```text
OrderStatusHistory
```

records:

```text
PAID → PROCESSING
```

while:

```text
ORDER_PAID
```

can trigger:

- Fulfillment
- Notification
- Analytics
- Inventory reconciliation

---

# 30. Checkout → Order

The critical flow is:

```text
Cart
  ↓
Checkout
  ↓
Validate
  ↓
Resolve Price
  ↓
Validate Inventory
  ↓
Reserve Inventory
  ↓
Create Order
  ↓
Payment
```

Depending on the payment architecture, payment authorization may happen before or after order creation.

Recommended approach:

```text
Checkout
   ↓
Create Order
   ↓
PENDING_PAYMENT
   ↓
Payment
   ↓
PAID
```

This gives the payment process a durable order reference.

---

# 31. Idempotent Order Creation

This is critical.

A customer may click:

```text
Place Order
```

twice.

Or the network may retry the request.

The system must not create:

```text
Order A
Order B
```

for the same checkout attempt.

Use an idempotency key:

```text
checkoutId + idempotencyKey
```

or a unique checkout transaction reference.

---

# 32. Order Creation Idempotency

Recommended constraint:

```text
UNIQUE(checkoutId)
```

if one checkout can produce only one parent order.

For more advanced architectures:

```text
UNIQUE(orderCreationReference)
```

The service should return the already-created order when the same request is retried.

---

# 33. Inventory Integration

Order creation must coordinate with Inventory.

Typical flow:

```text
Checkout
   ↓
Inventory Reservation
   ↓
Order Creation
   ↓
Payment
```

Payment success:

```text
Reservation
   ↓
Commit Stock
```

Payment failure:

```text
Reservation
   ↓
Release Stock
```

The exact orchestration should be handled through explicit workflows/events rather than unsafe distributed transactions.

---

# 34. Inventory Does Not Belong to Order

Do not put:

```text
onHandQuantity
reservedQuantity
availableQuantity
```

inside Order.

Order only needs relevant references and fulfillment quantities if required.

Inventory remains the source of truth for stock.

---

# 35. Payment Integration

Order should reference payment information, but Payment Management owns payment execution.

Possible:

```text
Order
├── paymentStatus
└── paymentReference
```

However, detailed payment transaction information belongs to Payment.

Example:

```text
Order
   │
   └── paymentReference
           ↓
       Payment Module
           ↓
       PaymentTransaction
```

---

# 36. Payment Status

A separate payment state may be required:

```text
UNPAID
PENDING
AUTHORIZED
PAID
FAILED
REFUNDED
PARTIALLY_REFUNDED
```

Do not confuse:

```text
Order Status
```

with:

```text
Payment Status
```

For example:

```text
Order = PENDING_PAYMENT
Payment = PENDING
```

Later:

```text
Order = PROCESSING
Payment = PAID
```

---

# 37. Cancellation

Cancellation should be an explicit operation.

```http
POST /api/v1/orders/{orderId}/cancel
```

The service must verify:

- Current order state
- Cancellation eligibility
- User ownership
- Payment state
- Fulfillment state
- Seller policies
- Inventory implications

---

# 38. Cancellation States

Do not automatically allow:

```text
SHIPPED → CANCELLED
```

unless the business explicitly supports it.

Instead, shipped orders may require:

```text
Return
Refund
```

Cancellation and return are different processes.

---

# 39. Cancellation Flow

```text
Customer
   ↓
Cancel Order
   ↓
Order Policy
   ↓
Validate State
   ↓
Cancel
   ↓
Release Inventory
   ↓
Payment Refund Request
   ↓
Notification
```

Payment refund execution belongs to Payment.

---

# 40. Returns

Returns should eventually be handled by a dedicated Returns/Refunds module.

Order should provide:

- order reference
- item reference
- purchase history
- fulfillment status

But should not own complete return workflows.

Future:

```text
Order
   ↓
Return Request
   ↓
Returns
   ↓
Refund
```

---

# 41. Order History

Customer API:

```http
GET /api/v1/orders
```

should support:

- Pagination
- Sorting
- Filtering
- Date range
- Status
- Order number

Example:

```http
GET /api/v1/orders?status=DELIVERED&page=0&size=20
```

---

# 42. Order Detail

```http
GET /api/v1/orders/{orderId}
```

Response:

```text
Order
├── orderNumber
├── status
├── items
├── totals
├── addresses
├── paymentSummary
├── fulfillmentSummary
└── history
```

Sensitive/internal information must be filtered based on caller role.

---

# 43. Seller Order APIs

Seller:

```http
GET /api/v1/seller/orders
```

Seller should see only orders associated with stores they are authorized to manage.

Seller can view:

- Order number
- Relevant seller items
- Customer delivery information required for fulfillment
- Payment state relevant to seller
- Fulfillment state
- Cancellation state
- Shipping state

Seller must not automatically receive unrelated seller/order information.

---

# 44. Admin APIs

Administrative APIs may include:

```http
GET /api/v1/admin/orders
GET /api/v1/admin/orders/{id}
POST /api/v1/admin/orders/{id}/cancel
POST /api/v1/admin/orders/{id}/status
```

Administrative status changes must:

- Require elevated permission
- Validate state transition
- Record actor
- Record reason
- Create audit event

---

# 45. GraphQL Queries

```graphql
type Query {
    orders(
        filter: OrderFilterInput
        first: Int
        after: String
    ): OrderConnection!

    order(id: ID!): Order

    orderByNumber(orderNumber: String!): Order

    sellerOrders(
        filter: SellerOrderFilterInput
        first: Int
        after: String
    ): SellerOrderConnection!
}
```

---

# 46. GraphQL Mutations

```graphql
type Mutation {

    createOrder(
        input: CreateOrderInput!
    ): OrderPayload!

    cancelOrder(
        input: CancelOrderInput!
    ): OrderPayload!

    updateOrderStatus(
        input: UpdateOrderStatusInput!
    ): OrderPayload!

    addOrderNote(
        input: AddOrderNoteInput!
    ): OrderNotePayload!
}
```

Order creation should normally be invoked by the Checkout workflow rather than directly by arbitrary clients.

---

# 47. GraphQL Inputs

```text
order/graphql/input/
├── CreateOrderInput
├── CancelOrderInput
├── UpdateOrderStatusInput
├── OrderFilterInput
├── SellerOrderFilterInput
└── AddOrderNoteInput
```

---

# 48. GraphQL Payloads

```text
order/graphql/payload/
├── OrderPayload
├── OrderConnection
├── SellerOrderPayload
├── SellerOrderConnection
├── OrderItemPayload
└── OrderNotePayload
```

---

# 49. REST DTOs

```text
order/dto/
├── request/
│   ├── CreateOrderRequest
│   ├── CancelOrderRequest
│   ├── UpdateOrderStatusRequest
│   └── AddOrderNoteRequest
│
└── response/
    ├── OrderResponse
    ├── OrderItemResponse
    ├── SellerOrderResponse
    └── OrderSummaryResponse
```

Never expose JPA entities directly.

---

# 50. Permissions

Recommended:

```text
order.read
order.create
order.cancel
order.update
order.status.update

order.history.read
order.note.create
order.note.read

order.seller.read
order.seller.manage

order.admin.read
order.admin.manage
```

Ownership policies remain necessary.

---

# 51. Customer Authorization

Customer:

```text
order.read
```

does not mean:

```text
Read every order
```

Policy:

```text
Order.customerId == authenticatedCustomerId
```

must be checked.

---

# 52. Seller Authorization

Seller access should be scoped:

```text
User
 ↓
Seller Membership
 ↓
Store
 ↓
SellerOrder
```

A seller should only access SellerOrders belonging to stores they are authorized to manage.

---

# 53. Admin Authorization

Administrative actions should use stronger permissions.

Example:

```text
order.admin.read
order.admin.manage
```

High-risk actions should be audited.

---

# 54. Order Events

Recommended:

```text
ORDER_CREATED
ORDER_PENDING_PAYMENT
ORDER_PAYMENT_PENDING

ORDER_PAID
ORDER_PAYMENT_FAILED

ORDER_PROCESSING
ORDER_FULFILLING
ORDER_SHIPPED
ORDER_DELIVERED
ORDER_COMPLETED

ORDER_CANCELLED
ORDER_EXPIRED

ORDER_ITEM_CANCELLED
ORDER_STATUS_CHANGED

SELLER_ORDER_CREATED
SELLER_ORDER_STATUS_CHANGED
```

---

# 55. Event Consumers

Potential consumers:

```text
Payment
Inventory
Shipping
Notification
Analytics
Customer
Seller
Reporting
```

Example:

```text
ORDER_PAID
    ↓
Inventory → Commit Reservation
    ↓
Fulfillment → Start Processing
    ↓
Notification → Notify Customer
    ↓
Analytics → Record Purchase
```

---

# 56. Transactional Outbox

Order events should use:

```text
Order Transaction
      ↓
Database
      ↓
Transactional Outbox
      ↓
Event Publisher
      ↓
Message Broker
```

This prevents the failure scenario:

```text
Order committed
      ↓
Event publishing fails
```

without leaving downstream systems unaware.

---

# 57. Order Event Idempotency

Consumers must tolerate duplicate events.

Example:

```text
ORDER_PAID
ORDER_PAID
```

should not result in:

```text
Inventory committed twice
```

Use:

```text
eventId
```

and consumer-side idempotency.

---

# 58. Order Read Models

Complex customer order pages may join:

```text
Order
OrderItem
SellerOrder
Product
Store
Payment
Shipping
```

Do not make the Order entity responsible for all of these relationships.

Create read models.

Example:

```text
OrderDetailsView
```

---

# 59. Database View Strategy

Recommended:

```text
order/
├── query/
│   ├── projection/
│   └── view/
```

Example:

```text
OrderSummaryView
```

could combine:

```text
Order
+
SellerOrder
+
OrderItem
+
Store
+
Product snapshot
```

A broader operational dashboard may combine:

```text
Order
+
Payment
+
Shipping
```

through read-only projections.

These views must not become normal write entities.

---

# 60. Order Module Structure

```text
order/
├── config/
│   └── OrderConfig.java
│
├── controller/
│   └── OrderController.java
│
├── service/
│   ├── OrderService.java
│   ├── OrderCreationService.java
│   ├── OrderCancellationService.java
│   ├── OrderStatusService.java
│   ├── SellerOrderService.java
│   └── OrderHistoryService.java
│
├── repository/
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── SellerOrderRepository.java
│   ├── OrderAddressRepository.java
│   └── OrderStatusHistoryRepository.java
│
├── entity/
│   ├── Order.java
│   ├── OrderItem.java
│   ├── SellerOrder.java
│   ├── OrderAddress.java
│   ├── OrderNote.java
│   └── OrderStatusHistory.java
│
├── dto/
│   ├── request/
│   └── response/
│
├── mapper/
│   └── OrderMapper.java
│
├── validator/
│   ├── OrderValidator.java
│   ├── OrderCancellationValidator.java
│   └── OrderTransitionValidator.java
│
├── exception/
│   ├── OrderNotFoundException.java
│   ├── OrderAccessDeniedException.java
│   ├── InvalidOrderStateException.java
│   ├── OrderCancellationNotAllowedException.java
│   └── DuplicateOrderException.java
│
├── policy/
│   ├── OrderPolicy.java
│   └── OrderCancellationPolicy.java
│
├── state/
│   ├── OrderStateMachine.java
│   └── OrderTransition.java
│
├── event/
│   ├── OrderCreatedEvent.java
│   ├── OrderPaidEvent.java
│   ├── OrderCancelledEvent.java
│   └── ...
│
├── query/
│   ├── projection/
│   └── view/
│
├── graphql/
│   ├── resolver/
│   │   ├── OrderQueryResolver.java
│   │   └── OrderMutationResolver.java
│   │
│   ├── input/
│   │   ├── CreateOrderInput.java
│   │   ├── CancelOrderInput.java
│   │   ├── OrderFilterInput.java
│   │   └── UpdateOrderStatusInput.java
│   │
│   └── payload/
│       ├── OrderPayload.java
│       ├── OrderConnection.java
│       ├── SellerOrderPayload.java
│       └── OrderItemPayload.java
│
└── OrderModule.java
```

---

# 61. Order Test Structure

```text
test/
└── order/
    ├── service/
    ├── repository/
    ├── policy/
    ├── state/
    ├── controller/
    ├── graphql/
    ├── integration/
    ├── security/
    ├── concurrency/
    └── e2e/
```

---

# 62. Unit Tests

Test:

- Order creation
- Order number generation
- Snapshot generation
- Total calculations
- State transitions
- Cancellation rules
- Seller-order creation
- Authorization policies
- Idempotency

---

# 63. State Machine Tests

Test valid:

```text
DRAFT → PENDING_PAYMENT
PENDING_PAYMENT → PAID
PAID → PROCESSING
PROCESSING → FULFILLING
FULFILLING → SHIPPED
SHIPPED → DELIVERED
DELIVERED → COMPLETED
```

Test invalid:

```text
PENDING_PAYMENT → DELIVERED
COMPLETED → PROCESSING
CANCELLED → PAID
```

---

# 64. Snapshot Tests

Scenario:

```text
Product:
Name = Laptop
Price = GHS 10,000
SKU = LAP-001
```

Create order.

Then change Product:

```text
Name = Laptop Pro
Price = GHS 12,000
SKU = LAP-002
```

Order must still show:

```text
Laptop
GHS 10,000
LAP-001
```

---

# 65. Repository Tests

Test:

- Order lookup
- Order number lookup
- Customer orders
- Seller orders
- Status filtering
- Date filtering
- Pagination
- Unique constraints

Use Testcontainers for database integration.

---

# 66. Security Tests

Test:

```text
Customer A → Customer B Order
```

Expected:

```text
403 Forbidden
```

Test seller isolation:

```text
Seller A → Seller B Order
```

Expected:

```text
403 Forbidden
```

Test unauthorized status updates.

---

# 67. Concurrency Tests

Critical scenario:

```text
Client A → Create Order
Client B → Retry Same Checkout
```

Expected:

```text
One Order
```

Also test:

```text
Concurrent status update
```

and:

```text
Concurrent cancellation
```

with optimistic locking.

---

# 68. Integration Tests

Test:

```text
Checkout
   ↓
Order
   ↓
Inventory
   ↓
Payment
   ↓
Fulfillment
```

Scenarios:

### Payment Success

```text
Order = PAID
Inventory = COMMITTED
```

### Payment Failure

```text
Order = FAILED
Inventory = RELEASED
```

### Cancellation

```text
Order = CANCELLED
Inventory = RELEASED
Refund = Requested
```

---

# 69. End-to-End Test

Normal purchase:

```text
Login
 ↓
Browse Product
 ↓
Add to Cart
 ↓
Checkout
 ↓
Reserve Inventory
 ↓
Create Order
 ↓
Payment
 ↓
Order PAID
 ↓
Fulfillment
 ↓
Shipping
 ↓
Delivery
 ↓
Completed
```

---

# 70. Performance Requirements

Target under normal production conditions:

```text
Order creation: < 1 second
Order detail: < 500 ms
Order history: < 500 ms
Seller order list: < 500 ms
```

These are initial engineering targets and should be validated with production-like load testing.

---

# 71. Scalability

The design should support:

```text
Millions of orders
Millions of order items
Thousands of concurrent checkouts
Multiple sellers
Multiple stores
Multiple fulfillment flows
```

Indexes and pagination are mandatory.

---

# 72. Important Indexes

Order:

```text
UNIQUE(public_id)
UNIQUE(order_number)

INDEX(customer_id)
INDEX(status)
INDEX(created_at)
INDEX(checkout_id)
INDEX(payment_reference)
```

SellerOrder:

```text
INDEX(order_id)
INDEX(seller_id)
INDEX(store_id)
INDEX(status)
```

OrderItem:

```text
INDEX(order_id)
INDEX(product_id)
INDEX(variant_id)
INDEX(seller_id)
INDEX(store_id)
```

---

# 73. Optimistic Locking

Use:

```java
@Version
private Long version;
```

where appropriate.

This helps prevent:

```text
Request A
     ↓
Order PROCESSING

Request B
     ↓
Order CANCELLED
```

from silently overwriting each other.

---

# 74. Auditability

The system should preserve:

- Who created the order
- When it was created
- Status changes
- Cancellation actor
- Cancellation reason
- Administrative modifications
- Seller actions
- System transitions

Order history should be append-oriented.

---

# 75. Cancellation Reason

Recommended:

```text
CUSTOMER_REQUEST
PAYMENT_FAILED
INVENTORY_UNAVAILABLE
SELLER_REQUEST
FRAUD
SYSTEM_ERROR
ADMINISTRATIVE
OTHER
```

The reason should be recorded.

---

# 76. Order Search

Customer search:

```text
Order Number
```

Seller search:

```text
Order Number
Customer reference
Product
SKU
```

Admin search may support:

```text
Order number
Customer
Seller
Store
Payment reference
Date
Status
```

Search infrastructure can eventually move to Search/Discovery.

---

# 77. Notifications

Order emits events.

Notification module handles:

```text
Order Created
Payment Confirmed
Order Processing
Order Shipped
Order Delivered
Order Cancelled
```

Flow:

```text
Order
 ↓
Event
 ↓
Notification
 ├── Email
 ├── Push
 └── In-App
```

Order should not directly send emails.

---

# 78. Reporting

Order events are consumed by Reporting/Analytics.

Potential reports:

```text
Revenue
Orders
Average Order Value
Cancellation Rate
Seller Sales
Product Sales
Customer Purchase Frequency
```

Order provides transactional facts.

Reporting owns analytical projections.

---

# 79. Fraud Integration

Future fraud detection can consume:

```text
ORDER_CREATED
ORDER_PAYMENT_PENDING
ORDER_PAID
```

Potential flow:

```text
Order
 ↓
Fraud Detection
 ↓
Risk Assessment
 ↓
Approve / Review / Reject
```

Fraud logic should not be embedded inside Order Management.

---

# 80. Order Number vs Public ID

Use both.

### Public ID

For API/resource identification:

```text
UUID
```

### Order Number

For human interaction:

```text
FYN-2026-000123
```

Customer support should primarily use order number.

API clients should generally use public IDs.

---

# 81. Data Ownership

| Data | Owner |
|---|---|
| Product | Product |
| Current Price | Pricing |
| Inventory | Inventory |
| Cart | Cart |
| Order | Order |
| Payment | Payment |
| Shipment | Shipping/Fulfillment |
| Customer | Customer |
| Seller | Seller |
| Store | Store |
| Notification | Notification |

Order can retain **historical snapshots** without becoming the owner of current source data.

---

# 82. Recommended Order Database

Core tables:

```text
orders
order_items
seller_orders
order_addresses
order_notes
order_status_history
order_metadata
```

Supporting:

```text
outbox_events
audit_logs
```

depending on common infrastructure.

---

# 83. Suggested Database Relationship

```text
Customer
   │
   │ 1:N
   ▼
Order
   │
   ├──────────────┐
   ▼              ▼
SellerOrder    OrderAddress
   │
   ▼
OrderItem
   │
   ├── productId
   ├── variantId
   ├── sellerId
   └── storeId
```

---

# 84. Multi-Seller Order Example

```text
ORDER #FYN-2026-000100

Customer: John

Seller A
──────────────
Laptop        ×1
Mouse         ×2
Subtotal: GHS 10,500

Seller B
──────────────
Keyboard      ×1
Subtotal: GHS 800

Total:
GHS 11,300
```

Internally:

```text
Order
├── SellerOrder A
│    ├── Item Laptop
│    └── Item Mouse
│
└── SellerOrder B
     └── Item Keyboard
```

---

# 85. Parent Order Status Derivation

For multi-seller orders, the parent state may be derived.

Example:

```text
Seller A = DELIVERED
Seller B = SHIPPED
```

Parent:

```text
DELIVERED / PARTIALLY_FULFILLED
```

A richer model may introduce:

```text
PARTIALLY_FULFILLED
PARTIALLY_CANCELLED
```

if required.

Do not force the parent order into an overly simplistic state model if marketplace fulfillment becomes complex.

---

# 86. Recommended Initial State Model

For V1, keep the core order state simple:

```text
PENDING_PAYMENT
PAID
PROCESSING
FULFILLING
SHIPPED
DELIVERED
COMPLETED
CANCELLED
FAILED
EXPIRED
```

Use SellerOrder for seller-specific operational state.

Introduce more complex parent statuses only when required by actual workflows.

---

# 87. Checkout Boundary

Checkout should orchestrate:

```text
Cart
Pricing
Inventory
Customer
Shipping
Promotion
Payment
Order
```

But Checkout should not become another giant domain entity.

A possible future:

```text
checkout/
├── service/
├── orchestration/
├── policy/
└── workflow/
```

The final durable transaction becomes:

```text
Order
```

---

# 88. Recommended Order Creation Contract

Checkout can call:

```text
OrderApplicationService.createFromCheckout(
    checkoutContext
)
```

The context contains:

```text
customer
cart
items
pricing snapshots
addresses
seller grouping
idempotency reference
```

The Order service then creates the durable order.

---

# 89. Order Creation Validation

Before creation:

### Customer

- authenticated
- active
- permitted to purchase

### Cart

- active
- not empty
- valid

### Items

- product still active
- variants valid
- quantities valid

### Pricing

- current
- currency valid
- totals calculated

### Inventory

- sufficient availability
- reservation successful

### Address

- valid
- required fields present

### Seller

- active
- store active

---

# 90. Final Checkout → Order Validation

Never assume:

```text
Cart was valid 5 minutes ago
```

means:

```text
Order is valid now
```

Revalidate critical data during checkout.

Especially:

```text
Price
Inventory
Product availability
Seller/store status
Promotions
Shipping
```

---

# 91. Security Requirements

- Ownership enforcement
- Seller scope enforcement
- Admin permission checks
- No order enumeration
- Opaque IDs
- Rate limiting
- Audit logs
- Sensitive data filtering
- Internal notes protection
- Secure webhook/event processing

---

# 92. Webhook/Event Security

Payment and fulfillment integrations may update order state through events.

Never trust an external callback blindly.

Verify:

- signature
- source
- event ID
- timestamp where applicable
- idempotency
- expected order/payment relationship

Then apply state transition rules.

---

# 93. Error Codes

Recommended:

```text
ORDER_NOT_FOUND
ORDER_ACCESS_DENIED
ORDER_ALREADY_EXISTS
ORDER_ALREADY_CANCELLED

INVALID_ORDER_STATE
INVALID_ORDER_TRANSITION
ORDER_CANCELLATION_NOT_ALLOWED

CHECKOUT_INVALID
CHECKOUT_ALREADY_PROCESSED

INVENTORY_RESERVATION_FAILED
PRICE_CHANGED
PRODUCT_UNAVAILABLE

ORDER_CREATION_FAILED
ORDER_PAYMENT_REQUIRED
```

---

# 94. Error Response

Example:

```json
{
  "code": "INVALID_ORDER_STATE",
  "message": "The order cannot be cancelled in its current state.",
  "field": "status",
  "details": {
    "currentStatus": "SHIPPED"
  }
}
```

Do not expose internal implementation details.

---

# 95. Module Events vs Integration Events

Not every internal event needs to become a public integration event.

Keep internal domain events:

```text
OrderStatusChanged
```

separate from stable integration contracts:

```text
ORDER_PAID
ORDER_SHIPPED
ORDER_COMPLETED
```

This allows internal refactoring without breaking external consumers.

---

# 96. Observability

Metrics:

```text
orders.created
orders.paid
orders.failed
orders.cancelled
orders.completed

orders.creation.latency
orders.payment.failure_rate
orders.cancellation_rate
orders.creation.duplicate_attempts
```

Logs should include:

```text
orderId
orderNumber
checkoutId
customerId
eventId
correlationId
```

Avoid logging sensitive customer data.

---

# 97. Distributed Tracing

A purchase flow should be traceable:

```text
Request
 ↓
Checkout
 ↓
Pricing
 ↓
Inventory
 ↓
Order
 ↓
Payment
 ↓
Fulfillment
```

Use correlation IDs / trace IDs across service boundaries.

---

# 98. Reliability

Order creation is a critical operation.

Recommended:

- Database transactions
- Idempotency
- Optimistic locking
- Transactional outbox
- Retry-safe consumers
- Dead-letter handling
- Reconciliation
- Monitoring

Do not rely on one large distributed transaction.

---

# 99. Reconciliation

A future reconciliation process should detect:

```text
Payment says PAID
Order says PENDING_PAYMENT
```

or:

```text
Inventory says committed
Order says cancelled
```

These inconsistencies should be detected and repaired through explicit workflows.

---

# 100. Milestones

## M1 — Order Foundation

Deliver:

- Order entity
- Order number
- Order repository
- Order service
- Status model
- State machine
- Status history

---

## M2 — Order Items

Deliver:

- OrderItem
- Product snapshots
- Pricing snapshots
- Seller/store references
- Totals
- Address snapshots

---

## M3 — Checkout Integration

Deliver:

- Create order from checkout
- Idempotency
- Cart validation
- Inventory reservation
- Order creation workflow

---

## M4 — Payment Integration

Deliver:

- Payment references
- Payment events
- PAID state
- Failed payment handling
- Inventory commit/release integration

---

## M5 — Seller Orders

Deliver:

- SellerOrder
- Seller-scoped access
- Seller status
- Multi-seller orders
- Seller order APIs

---

## M6 — Fulfillment Integration

Deliver:

- Shipping integration
- Fulfillment state
- Shipment references
- Delivered state
- Completion workflow

---

## M7 — Cancellation & Operational Management

Deliver:

- Customer cancellation
- Seller cancellation
- Admin cancellation
- Cancellation reasons
- Refund integration contract
- Audit

---

## M8 — Analytics & Optimization

Deliver:

- Order events
- Reporting projections
- Read models
- Performance optimization
- Reconciliation
- Advanced search

---

# 101. User Stories

## US-01 — Create Order

**As a customer, I want to submit my cart as an order so that my purchase can be processed.**

Acceptance:

- Checkout is valid.
- Inventory is reserved.
- Order is created.
- Unique order number is generated.
- Items are snapshotted.

---

## US-02 — View Order

**As a customer, I want to view my order details so that I know what I purchased.**

Acceptance:

- Customer sees only their orders.
- Historical item data is displayed.
- Current product changes do not alter historical order details.

---

## US-03 — View Order History

**As a customer, I want to view previous orders.**

Acceptance:

- Orders are paginated.
- Orders can be filtered.
- Order numbers are searchable.

---

## US-04 — Cancel Order

**As a customer, I want to cancel an eligible order.**

Acceptance:

- State is validated.
- Cancellation policy is applied.
- Order is transitioned safely.
- Inventory workflow is triggered.

---

## US-05 — Seller View

**As a seller, I want to see orders containing my products.**

Acceptance:

- Seller sees only authorized SellerOrders.
- Customer information is limited to fulfillment requirements.

---

## US-06 — Preserve History

**As a customer, I want my old order to remain accurate even if product information changes.**

Acceptance:

- Product snapshots are stored.
- Historical price remains unchanged.
- Historical SKU remains unchanged.

---

## US-07 — Prevent Duplicate Orders

**As a customer, I do not want a network retry to create duplicate orders.**

Acceptance:

- Checkout creation is idempotent.
- Repeated requests return the existing order.

---

## US-08 — Track Order

**As a customer, I want to know the status of my order.**

Acceptance:

- Current status is shown.
- Status history is available where appropriate.
- Fulfillment updates propagate correctly.

---

# 102. Definition of Done

### Foundation

- [ ] Order entity
- [ ] Order number
- [ ] Order lifecycle
- [ ] State machine
- [ ] Status history

### Items

- [ ] Order items
- [ ] Product snapshots
- [ ] Variant snapshots
- [ ] Price snapshots
- [ ] Seller/store references
- [ ] Address snapshots

### Checkout

- [ ] Checkout integration
- [ ] Inventory reservation
- [ ] Idempotency
- [ ] Transaction handling

### Payment

- [ ] Payment integration
- [ ] Payment success
- [ ] Payment failure
- [ ] Commit/release inventory workflow

### Seller

- [ ] SellerOrder
- [ ] Seller authorization
- [ ] Multi-seller support

### APIs

- [ ] REST
- [ ] GraphQL
- [ ] DTOs
- [ ] Error model
- [ ] Pagination

### Security

- [ ] Customer ownership
- [ ] Seller scope
- [ ] Admin authorization
- [ ] Audit
- [ ] Secure callbacks

### Reliability

- [ ] Transactional outbox
- [ ] Idempotent events
- [ ] Retry handling
- [ ] Reconciliation

### Testing

- [ ] Unit
- [ ] Repository
- [ ] State machine
- [ ] Integration
- [ ] Security
- [ ] Concurrency
- [ ] GraphQL
- [ ] E2E

---

# 103. Final Architectural Boundary

The final responsibility chain should be:

```text
Product
"What is being sold?"
        ↓
Pricing
"How much?"
        ↓
Inventory
"Is it available?"
        ↓
Cart
"What does the customer want to buy?"
        ↓
Checkout
"Can this purchase be submitted?"
        ↓
Order
"What did the customer submit?"
        ↓
Payment
"Was it paid?"
        ↓
Fulfillment
"Was it prepared and shipped?"
        ↓
Delivery
"Was it delivered?"
```

And the Order module should preserve:

```text
                  ORDER
                    │
        ┌───────────┼────────────┐
        ▼           ▼            ▼
   Order Items   Seller Orders  Addresses
        │
        ├── Product Reference
        ├── Variant Reference
        ├── Product Snapshot
        ├── Price Snapshot
        └── Seller/Store Reference
```

---

# 104. Critical Architectural Rules

1. **Order is the authoritative record of the submitted purchase.**
2. **Cart remains mutable; Order becomes historically stable.**
3. **Never rely on current Product data to reconstruct historical orders.**
4. **Order items must preserve relevant snapshots.**
5. **Current pricing remains owned by Pricing.**
6. **Inventory remains owned by Inventory.**
7. **Payment execution remains owned by Payment.**
8. **Shipping and fulfillment remain outside Order.**
9. **Cancellation is not the same as return.**
10. **Refunds belong to Payment/Refund Management.**
11. **SellerOrder enables independent marketplace fulfillment.**
12. **Customers can only access their own orders.**
13. **Sellers can only access authorized SellerOrders.**
14. **Order state changes must pass through a state machine.**
15. **Order creation must be idempotent.**
16. **Order events should use transactional outbox.**
17. **Event consumers must be idempotent.**
18. **Order addresses should be snapshotted.**
19. **Database views are read models, not domain entities.**
20. **Order should coordinate with other modules without becoming a God Object.**