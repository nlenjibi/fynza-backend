# 11 — Cart Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Cart Management  
**Module ID:** `CART-11`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Authentication, User Management, Customer Management, Product Management, Pricing Management, Inventory Management, Store Management, Authorization, Common

---

# 1. Overview

Cart Management manages the customer's collection of products they intend to purchase before checkout.

The cart represents **purchase intent**, not a completed transaction.

It manages:

- Cart creation.
- Cart ownership.
- Cart items.
- Product/variant references.
- Quantities.
- Item removal.
- Quantity updates.
- Cart merging.
- Guest carts.
- Saved carts.
- Cart expiration.
- Cart validation.
- Price refresh.
- Inventory availability checks.
- Cart totals.
- Cart status.
- Cart events.

The central distinction is:

```text
Product
    │
    │ What is being sold?
    ▼
Pricing
    │
    │ What does it cost?
    ▼
Inventory
    │
    │ Is it available?
    ▼
Cart
    │
    │ What does the customer intend to buy?
    ▼
Checkout
    │
    ▼
Order
```

---

# 2. Goals

Cart Management should:

1. Allow customers to add products.
2. Allow customers to remove products.
3. Allow quantity changes.
4. Support authenticated customers.
5. Support guest customers.
6. Merge guest and authenticated carts.
7. Validate products.
8. Validate variants.
9. Validate availability.
10. Refresh prices.
11. Calculate cart totals.
12. Detect unavailable products.
13. Prevent invalid quantities.
14. Preserve cart state.
15. Support cart expiration.
16. Prepare carts for checkout.
17. Support multi-store carts where marketplace rules permit.
18. Provide a reliable foundation for checkout.

---

# 3. Non-Goals

Cart Management does not own:

- Product definitions.
- Product pricing.
- Inventory quantities.
- Payments.
- Orders.
- Shipping execution.
- Promotions.
- Coupons.
- Customer identity.

It consumes information from those modules.

---

# 4. Cart vs Order

This distinction is fundamental.

```text
Cart
=
Customer intends to purchase something.
```

```text
Order
=
Customer has submitted a purchase transaction.
```

Example:

```text
Cart
├── Laptop × 1
└── Mouse × 2
```

Customer can freely change it.

After checkout:

```text
Order
├── Laptop × 1
└── Mouse × 2
```

The order becomes a transactional record.

---

# 5. Cart Lifecycle

Recommended statuses:

```text
ACTIVE
CHECKOUT
CONVERTED
ABANDONED
EXPIRED
MERGED
```

Typical flow:

```text
ACTIVE
   │
   ▼
CHECKOUT
   │
   ├──► CONVERTED
   │
   └──► ACTIVE
```

Inactive carts may eventually become:

```text
ABANDONED
```

or:

```text
EXPIRED
```

---

# 6. CART-FR-001 — Create Cart

A cart may be created:

- Explicitly.
- Automatically when an item is added.
- During guest shopping.
- When a customer logs in.

Recommended behavior:

```text
Customer
   │
   ▼
Add Product
   │
   ▼
Existing Cart?
   │
 ┌─┴───┐
No    Yes
│      │
▼      ▼
Create  Update
Cart    Cart
```

---

# 7. Authenticated Cart

Authenticated customers should have a persistent cart.

```text
User
  │
  ▼
Customer
  │
  ▼
Cart
```

A customer should normally have one active cart per supported shopping context.

---

# 8. Guest Cart

Guests should be able to add products without creating an account.

```text
Guest
  │
  ▼
Anonymous Cart
  │
  ▼
Cart Token
```

The browser stores a secure cart identifier/token.

Do not expose sequential database IDs as guest cart identifiers.

---

# 9. Guest Cart Security

Guest carts should use:

```text
opaqueCartToken
```

rather than:

```text
cartId = 123
```

This reduces predictable-resource attacks.

Guest cart operations must validate possession of the token.

---

# 10. Guest-to-Customer Cart Merge

When a guest logs in:

```text
Guest Cart
    │
    ▼
Authentication
    │
    ▼
Customer Cart
    │
    ▼
Merge
```

Example:

```text
Guest:
Laptop × 1
Mouse × 1

Customer:
Laptop × 1
Keyboard × 1
```

After merge:

```text
Laptop × 2
Mouse × 1
Keyboard × 1
```

Subject to inventory and maximum-quantity rules.

---

# 11. CART-FR-002 — Add Item

A customer can add a product or variant.

Input:

```text
productId
variantId
quantity
```

The backend must validate:

- Product exists.
- Product is active.
- Product is purchasable.
- Variant exists.
- Variant belongs to product.
- Quantity is valid.
- Customer can purchase it.

Never trust frontend product information.

---

# 12. Add Item Flow

```text
Add Item
   │
   ▼
Validate Product
   │
   ▼
Validate Variant
   │
   ▼
Validate Quantity
   │
   ▼
Resolve Current Price
   │
   ▼
Check Availability
   │
   ▼
Add / Increase Item
```

Important:

> Adding an item to a cart does not necessarily reserve inventory.

Inventory reservation normally happens during checkout.

---

# 13. CART-FR-003 — Update Quantity

Customer can change quantity.

Example:

```text
Before:
Laptop × 1

After:
Laptop × 3
```

The system should validate the new quantity against:

- Product rules.
- Variant rules.
- Inventory availability.
- Maximum order quantity.
- Seller restrictions.

---

# 14. CART-FR-004 — Remove Item

Customer can remove an item.

```text
Cart
├── Laptop × 1
├── Mouse × 2
└── Keyboard × 1

Remove Mouse

Cart
├── Laptop × 1
└── Keyboard × 1
```

Removing an item should not affect inventory if no reservation exists.

---

# 15. CART-FR-005 — Clear Cart

Customer may clear the entire cart.

```text
DELETE /cart/items
```

or:

```text
clearCart
```

All active cart items are removed.

---

# 16. Cart Item Identity

A cart item should represent a specific sellable configuration.

For example:

```text
T-Shirt
Color = Black
Size = Large
```

The cart should reference the variant:

```text
variantId
```

rather than only:

```text
productId
```

Two variants should not accidentally collapse into one cart item.

---

# 17. Cart Item Data

Recommended:

```text
CartItem
-------------------------
id
cartId
productId
variantId
quantity
createdAt
updatedAt
```

Product information should normally be retrieved from Product Management.

---

# 18. Cart Item Snapshot

Although the cart references Product, limited display snapshots may be useful.

Example:

```text
productNameSnapshot
skuSnapshot
imageSnapshot
```

However, these should be treated as display/cache data, not authoritative product information.

Do not turn Cart into a duplicate Product database.

---

# 19. Price Handling

Cart should not own pricing.

Instead:

```text
Cart
   │
   ▼
Pricing
   │
   ▼
Effective Price
```

When retrieving the cart:

```text
Cart Item
    │
    ▼
Price Resolver
    │
    ▼
Current Effective Price
```

---

# 20. Cart Price Refresh

Prices can change after an item is added.

Example:

```text
At 10:00
Product = GHS 100

At 12:00
Product = GHS 120
```

The cart should detect the change.

Example response:

```text
Price changed
Previous: GHS 100
Current:  GHS 120
```

The customer should be informed before checkout.

---

# 21. Never Trust Cart Prices

A malicious client could send:

```json
{
  "price": 1.00
}
```

for a product costing:

```text
GHS 1,500
```

The backend must ignore client-provided prices.

The authoritative flow is:

```text
Frontend
   │
   ▼
Product/Variant ID
   │
   ▼
Backend
   │
   ▼
Pricing Module
   │
   ▼
Authoritative Price
```

---

# 22. Inventory Handling

Cart should query Inventory.

```text
Cart
  │
  ▼
Inventory
  │
  ▼
Availability
```

Example:

```text
Cart quantity:
10

Available:
6
```

Result:

```text
Only 6 units are currently available.
```

---

# 23. No Reservation on Add-to-Cart

Recommended default:

```text
Add to Cart
     ↓
No reservation
```

Why?

Because customers can leave carts for hours or days.

If every cart permanently reserves inventory:

```text
100 units
↓
1,000 abandoned carts
↓
Inventory unavailable
```

Therefore reservations should normally happen at checkout.

---

# 24. Optional Cart Reservation

Some businesses may require:

```text
Flash Sale
Limited Product
High Demand
```

In such cases:

```text
Cart
 │
 ▼
Temporary Reservation
 │
 ▼
Expiration
```

This should be an explicit business feature rather than the default behavior.

---

# 25. CART-FR-006 — Cart Validation

Before checkout, the cart must be validated.

Validate:

```text
Product active?
Variant active?
Category valid?
Price current?
Inventory available?
Quantity valid?
Seller active?
Store active?
```

Example:

```text
Cart Validation
│
├── Product ✓
├── Variant ✓
├── Price ✓
├── Inventory ✗
└── Store ✓
```

Result:

```text
CHECKOUT_BLOCKED
```

---

# 26. Cart Validation Result

Recommended response:

```text
CartValidationResult
-------------------------
valid
errors[]
warnings[]
items[]
subtotal
discount
tax
shippingEstimate
grandTotal
currency
```

Errors prevent checkout.

Warnings may require customer confirmation.

---

# 27. Cart Totals

Cart should expose calculated totals.

Example:

```text
Subtotal        GHS 1,500
Discount        GHS   100
Shipping        GHS    50
Tax             GHS     0
--------------------------------
Total           GHS 1,450
```

However, Cart should not necessarily own the calculation logic for:

- Taxes.
- Shipping.
- Promotions.

Instead:

```text
Cart
 ├── Pricing
 ├── Promotion
 ├── Tax
 └── Shipping
        │
        ▼
      Totals
```

---

# 28. Money Calculation

Use:

```java
BigDecimal
```

Never:

```java
double
float
```

Example:

```java
BigDecimal subtotal;
BigDecimal discount;
BigDecimal total;
```

All monetary calculations must be deterministic.

---

# 29. CART-FR-007 — Multiple Stores

Fynza is a marketplace, so a cart may contain products from multiple stores.

Example:

```text
Cart
│
├── Store A
│   ├── Laptop
│   └── Mouse
│
└── Store B
    └── Keyboard
```

The cart should support this if Fynza allows multi-seller checkout.

---

# 30. Multi-Store Checkout

A single customer cart may become multiple seller/order groups.

```text
Cart
   │
   ├── Store A
   │      └── Order A
   │
   └── Store B
          └── Order B
```

Alternatively, Fynza can create:

```text
Parent Order
   ├── Seller Order A
   └── Seller Order B
```

The exact order architecture should be finalized in Order Management.

Cart should remain capable of grouping items by store.

---

# 31. Store-Level Cart Groups

A useful read model:

```text
CartGroup
-------------------------
storeId
storeName
items
subtotal
shippingEstimate
```

This is especially useful for:

- Seller-specific shipping.
- Seller-specific promotions.
- Seller-specific policies.

---

# 32. Quantity Limits

Products may have:

```text
minimumQuantity
maximumQuantity
quantityIncrement
```

Example:

```text
Minimum = 2
Maximum = 20
Increment = 2
```

Valid:

```text
2
4
6
8
```

Invalid:

```text
1
3
5
```

This can be configured by Product or future Commerce Rules.

---

# 33. Cart Item Validation

Every item should pass:

```text
quantity > 0
```

and:

```text
quantity <= allowedMaximum
```

and where required:

```text
quantity % quantityIncrement == 0
```

---

# 34. CART-FR-008 — Cart Expiration

Guest carts should have an expiration policy.

Example:

```text
Guest Cart
Expires:
30 days
```

Authenticated carts may be retained longer.

Do not delete cart history immediately.

Use:

```text
EXPIRED
```

before eventual archival/deletion.

---

# 35. Abandoned Cart

A cart becomes abandoned when:

```text
No activity
+
Configured inactivity period
```

Example:

```text
Last activity:
30 days ago

Status:
ABANDONED
```

This can trigger:

```text
Notification
Analytics
Marketing
```

only where the customer has appropriate consent.

---

# 36. Cart Recovery

Future functionality:

```text
Abandoned Cart
      │
      ▼
Reminder
      │
      ▼
Customer
      │
      ▼
Restore Cart
```

Recovery must revalidate:

- Product availability.
- Current price.
- Product status.
- Seller/store status.

Never assume the old cart remains purchasable.

---

# 37. CART-FR-009 — Cart Persistence

Authenticated cart data should persist across:

```text
Browser sessions
Devices
Logins
```

Example:

```text
Desktop
   │
   ▼
Cart
   │
   ▼
Mobile
   │
   ▼
Same Cart
```

---

# 38. CART-FR-010 — Cart Merge Rules

When merging guest and customer carts:

1. Match by product/variant.
2. Combine quantities.
3. Apply maximum quantity.
4. Validate availability.
5. Resolve current price.
6. Remove unavailable items or mark them.
7. Preserve the customer cart as authoritative where conflicts exist.

Example:

```text
Guest:
Laptop × 2

Customer:
Laptop × 3

Merged:
Laptop × 5
```

If maximum is 4:

```text
Laptop × 4
```

with a warning.

---

# 39. Cart Errors

Recommended error codes:

```text
CART_NOT_FOUND
CART_ACCESS_DENIED
CART_ITEM_NOT_FOUND

PRODUCT_NOT_FOUND
PRODUCT_NOT_PURCHASABLE
VARIANT_NOT_FOUND
VARIANT_NOT_ACTIVE

INVALID_QUANTITY
MAX_QUANTITY_EXCEEDED
INSUFFICIENT_STOCK

PRICE_CHANGED
PRICE_UNAVAILABLE

STORE_INACTIVE
SELLER_INACTIVE

CART_EMPTY
CHECKOUT_BLOCKED
```

---

# 40. Cart API — REST

## Customer

```text
GET    /cart
POST   /cart/items
PATCH  /cart/items/{itemId}
DELETE /cart/items/{itemId}
DELETE /cart/items
POST   /cart/validate
POST   /cart/merge
```

## Guest

```text
GET    /guest/cart
POST   /guest/cart/items
PATCH  /guest/cart/items/{itemId}
DELETE /guest/cart/items/{itemId}
```

The exact guest API can instead use the same `/cart` endpoints with a secure cart token.

---

# 41. Add Item Request

```json
{
  "productId": "prod_123",
  "variantId": "var_456",
  "quantity": 2
}
```

The backend resolves:

```text
Product
Variant
Price
Availability
```

---

# 42. Update Item Request

```json
{
  "quantity": 4
}
```

No price should be accepted from the client.

---

# 43. GraphQL Queries

```graphql
cart
cartSummary
cartItems
cartValidation
```

Example:

```graphql
query {
  cart {
    id
    status
    items {
      id
      quantity
      product {
        id
        name
      }
      variant {
        id
        sku
      }
      pricing {
        effectivePrice
        currency
      }
    }
    totals {
      subtotal
      discount
      total
      currency
    }
  }
}
```

---

# 44. GraphQL Mutations

```graphql
addCartItem
updateCartItem
removeCartItem
clearCart
validateCart
mergeCart
```

Future:

```graphql
saveCart
restoreCart
```

---

# 45. GraphQL Structure

```text
cart/
└── graphql/
    ├── resolver/
    │   ├── CartResolver
    │   └── CartMutationResolver
    │
    ├── input/
    │   ├── AddCartItemInput
    │   ├── UpdateCartItemInput
    │   ├── CartFilterInput
    │   └── MergeCartInput
    │
    └── payload/
        ├── CartPayload
        ├── CartItemPayload
        ├── CartValidationPayload
        └── CartTotalsPayload
```

---

# 46. Authorization

Permissions:

```text
cart.read
cart.create
cart.update
cart.delete
cart.validate
cart.merge
```

Customers may manage:

```text
OWN_CART
```

Never:

```text
ANY_CART
```

unless explicitly authorized for support/admin operations.

---

# 47. Seller Access

Sellers generally should not access customer carts.

A seller should not be able to query:

```text
Customer A's cart
```

because it contains potentially sensitive purchasing intent.

Seller-facing analytics may later expose anonymized/aggregated cart metrics.

---

# 48. Admin Access

Support/admin users may need limited access for customer support.

Access should be:

```text
permission-based
+
audited
+
purpose-limited
```

Avoid giving every administrator unrestricted access to customer carts.

---

# 49. Domain Events

Recommended events:

```text
CART_CREATED
CART_UPDATED
CART_ITEM_ADDED
CART_ITEM_UPDATED
CART_ITEM_REMOVED
CART_CLEARED

CART_MERGED
CART_VALIDATED

CART_ABANDONED
CART_EXPIRED
CART_CONVERTED

CART_PRICE_CHANGED
CART_ITEM_UNAVAILABLE
```

---

# 50. Event Consumers

```text
Cart
 │
 ├──► Analytics
 ├──► Notification
 ├──► Marketing
 ├──► Recommendation
 └──► Checkout
```

For example:

```text
CART_ABANDONED
       │
       ▼
Marketing / Notification
```

Only where consent and business policy permit.

---

# 51. Checkout Integration

Cart is the input to Checkout.

```text
Cart
  │
  ▼
Validate
  │
  ▼
Calculate
  │
  ▼
Checkout
```

Checkout should then:

```text
Resolve Price
      ↓
Validate Inventory
      ↓
Reserve Inventory
      ↓
Calculate Final Totals
      ↓
Create Order
      ↓
Payment
```

Cart should not create the final order itself.

---

# 52. Cart-to-Order Snapshot

When checkout succeeds:

```text
Cart
 │
 ▼
Order
```

The Order module creates immutable snapshots:

```text
OrderItem
├── productId
├── variantId
├── productNameSnapshot
├── skuSnapshot
├── unitPrice
├── quantity
├── discount
├── tax
└── total
```

Cart remains mutable.

Order becomes historical.

---

# 53. Database Model

## Cart

```text
Cart
-------------------------
id
publicId
customerId
guestTokenHash
status
currency
lastActivityAt
expiresAt
createdAt
updatedAt
```

`customerId` may be nullable for guest carts.

---

# 54. Cart Item

```text
CartItem
-------------------------
id
cartId
productId
variantId
quantity
createdAt
updatedAt
```

Recommended uniqueness:

```text
(cartId, productId, variantId)
```

This prevents duplicate rows for the same sellable variant.

---

# 55. Cart Metadata

Future:

```text
CartMetadata
-------------------------
cartId
key
value
```

Possible uses:

```text
channel
device
campaign
referral
```

Be careful not to turn this into an uncontrolled JSON dumping ground.

---

# 56. Cart Read Model

For the frontend:

```text
Cart
+
Product
+
Variant
+
Store
+
Pricing
+
Inventory
```

can be presented as:

```text
CartSummaryView
```

Example:

```text
CartSummaryView
-------------------------
cartId
storeId
storeName
productId
productName
variantId
sku
quantity
availableQuantity
unitPrice
discount
lineTotal
currency
```

This is a read model.

It should not become a giant Cart entity.

---

# 57. Database Views

If the cart page requires complex joins:

```text
Cart
 ├── Product
 ├── Variant
 ├── Store
 ├── Pricing
 └── Inventory
```

use:

```text
cart/
└── query/
    ├── projection/
    └── view/
```

This follows the architecture already established for Fynza.

---

# 58. Caching

Cart data can be cached, especially for guests.

Potential cache:

```text
cart:{cartToken}
cart:{customerId}
```

But the database remains authoritative.

Pricing and inventory should be refreshed when required.

Never use stale cache as the final checkout authority.

---

# 59. Concurrency

Customers can modify carts from multiple devices.

Example:

```text
Desktop:
quantity = 2

Mobile:
quantity = 3
```

Use:

- Optimistic locking.
- Version numbers.
- Atomic updates where required.

Example:

```java
@Version
private Long version;
```

Return a conflict if the client is updating stale cart state.

---

# 60. Idempotency

Add-item operations may be retried due to:

- Network failures.
- Mobile retries.
- Client retries.
- API gateway retries.

For important operations, support:

```text
Idempotency-Key
```

Example:

```text
Request:
IDEMPOTENCY-123
```

Repeated request:

```text
IDEMPOTENCY-123
```

should not accidentally add the item twice.

---

# 61. Security

Requirements:

- Secure guest cart tokens.
- No sequential guest identifiers.
- Ownership validation.
- Input validation.
- Rate limiting.
- No client-side price trust.
- No client-side inventory trust.
- Authorization on every cart operation.
- Avoid exposing other customers' carts.
- Audit privileged cart access.

---

# 62. Rate Limiting

Potential abuse:

```text
POST /cart/items
```

thousands of times per second.

Apply rate limits by:

```text
User
IP
Guest Cart Token
```

where appropriate.

---

# 63. Testing

## Unit Tests

Test:

- Cart creation.
- Add item.
- Update quantity.
- Remove item.
- Clear cart.
- Quantity validation.
- Merge logic.
- Expiration.
- Status transitions.
- Totals.

---

# 64. Repository Tests

Test:

- Cart persistence.
- Unique cart item constraint.
- Optimistic locking.
- Guest cart lookup.
- Customer cart lookup.
- Expiration queries.

---

# 65. Integration Tests

Test:

```text
Cart
 │
 ├── Product
 ├── Pricing
 └── Inventory
```

Examples:

```text
Product inactive
→ Cart validation fails

Price changed
→ Cart warning

Insufficient inventory
→ Checkout blocked
```

---

# 66. Security Tests

Test:

```text
Customer A → Customer B cart
```

Expected:

```text
ACCESS_DENIED
```

Also test:

- Guest token guessing.
- GraphQL authorization bypass.
- REST authorization bypass.
- Seller accessing carts.
- Admin privilege restrictions.
- Cart IDOR.

---

# 67. Concurrency Tests

Example:

```text
Same customer
   │
   ├── Device A → quantity 3
   └── Device B → quantity 5
```

Ensure:

- No corrupted state.
- No lost updates where prohibited.
- Version conflicts handled correctly.

---

# 68. E2E — Normal Shopping

```text
Customer
   │
   ▼
Browse Product
   │
   ▼
Add to Cart
   │
   ▼
View Cart
   │
   ▼
Update Quantity
   │
   ▼
Validate Cart
   │
   ▼
Checkout
```

---

# 69. E2E — Guest to Customer

```text
Guest
 │
 ▼
Add Product
 │
 ▼
Guest Cart
 │
 ▼
Login
 │
 ▼
Merge Cart
 │
 ▼
Customer Cart
```

---

# 70. E2E — Price Change

```text
Add Product
 │
 ▼
Price = GHS 100
 │
 ▼
Seller changes price
 │
 ▼
Cart refresh
 │
 ▼
Price = GHS 120
 │
 ▼
Customer informed
```

---

# 71. E2E — Stock Change

```text
Cart:
5 units

Inventory:
5 units

Another customer buys 3

Inventory:
2 units

Cart refresh

Result:
Only 2 available
```

Checkout must be blocked or quantity adjusted according to business policy.

---

# 72. Module Structure

```text
cart/
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
└── CartModule.java
```

Tests:

```text
test/
└── cart/
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

# 73. Recommended Entities

Core:

```text
Cart
CartItem
```

Future:

```text
CartGroup
CartMetadata
CartReservation
CartRecovery
```

---

# 74. Dependencies

```text
Cart
│
├── Authentication
├── Customer
├── Product
├── Pricing
├── Inventory
├── Store
├── Authorization
└── Common
```

Cart consumers:

```text
Cart
 │
 ├── Checkout
 ├── Order
 ├── Analytics
 ├── Notification
 └── Recommendation
```

---

# 75. Milestones

## M1 — Cart Foundation

- [ ] Cart entity.
- [ ] Cart item entity.
- [ ] Authenticated cart.
- [ ] Basic CRUD.
- [ ] Quantity validation.
- [ ] Ownership validation.

## M2 — Guest Cart

- [ ] Guest cart.
- [ ] Secure cart token.
- [ ] Guest persistence.
- [ ] Guest expiration.
- [ ] Guest-to-customer merge.

## M3 — Commerce Integration

- [ ] Product integration.
- [ ] Variant validation.
- [ ] Pricing integration.
- [ ] Inventory integration.
- [ ] Availability validation.

## M4 — Cart Validation

- [ ] Price change detection.
- [ ] Product status validation.
- [ ] Inventory validation.
- [ ] Seller/store validation.
- [ ] Cart totals.

## M5 — Checkout Integration

- [ ] Checkout handoff.
- [ ] Inventory reservation.
- [ ] Price confirmation.
- [ ] Order creation.
- [ ] Cart conversion.

## M6 — Optimization

- [ ] Read models.
- [ ] Database views.
- [ ] Caching.
- [ ] Concurrency handling.
- [ ] Idempotency.
- [ ] Performance optimization.

## M7 — Advanced Cart

- [ ] Abandoned cart.
- [ ] Cart recovery.
- [ ] Saved carts.
- [ ] Multi-store grouping.
- [ ] Optional cart reservations.

---

# 76. Definition of Done

- [ ] Authenticated carts implemented.
- [ ] Guest carts implemented.
- [ ] Secure guest token implemented.
- [ ] Cart items implemented.
- [ ] Add item implemented.
- [ ] Update quantity implemented.
- [ ] Remove item implemented.
- [ ] Clear cart implemented.
- [ ] Product validation implemented.
- [ ] Variant validation implemented.
- [ ] Pricing integration implemented.
- [ ] Inventory integration implemented.
- [ ] Availability validation implemented.
- [ ] Price-change detection implemented.
- [ ] Cart validation implemented.
- [ ] Cart totals implemented.
- [ ] Guest/customer cart merge implemented.
- [ ] Cart expiration implemented.
- [ ] REST API implemented.
- [ ] GraphQL API implemented.
- [ ] Authorization implemented.
- [ ] Domain events implemented.
- [ ] Idempotency implemented.
- [ ] Concurrency handling implemented.
- [ ] Read models implemented where required.
- [ ] Caching implemented safely.
- [ ] Checkout integration implemented.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] Concurrency tests pass.
- [ ] E2E tests pass.
- [ ] Documentation completed.

---

# 77. Architectural Boundaries

The commerce architecture now becomes:

```text
Category
    │
    ▼
Product
    │
    ├──────────────┐
    ▼              ▼
Pricing        Inventory
    │              │
    └──────┬───────┘
           ▼
          Cart
           │
           ▼
        Checkout
           │
           ▼
         Order
           │
           ▼
        Payment
```

Responsibilities:

```text
Category
= How is the catalog organized?

Product
= What is being sold?

Pricing
= How much does it cost?

Inventory
= How many are available?

Cart
= What does the customer intend to purchase?

Checkout
= Can the intended purchase become a transaction?

Order
= What was actually purchased?

Payment
= How was it paid for?
```

---

# 78. Key Architectural Principle

> **Cart Management owns purchase intent, not product, price, inventory, or order data.**

The most important rule is:

```text
Cart
   │
   ├── references Product
   ├── asks Pricing for price
   ├── asks Inventory for availability
   │
   ▼
Checkout
```

Cart should remain relatively lightweight.

Do not turn it into:

```text
Cart
 ├── Product
 ├── Pricing
 ├── Inventory
 ├── Payment
 ├── Shipping
 ├── Tax
 ├── Promotion
 ├── Customer
 └── Order
```

Instead, each domain owns its own responsibility and Cart orchestrates the customer's current shopping state.

---

# 79. Recommended Fynza Commerce Flow

```text
                    ┌──────────────┐
                    │   Category   │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │   Product    │
                    └──────┬───────┘
                           │
                 ┌─────────┴─────────┐
                 ▼                   ▼
          ┌────────────┐      ┌────────────┐
          │  Pricing   │      │ Inventory  │
          └─────┬──────┘      └──────┬─────┘
                │                    │
                └─────────┬──────────┘
                          ▼
                    ┌───────────┐
                    │   Cart    │
                    └─────┬─────┘
                          │
                          ▼
                    ┌───────────┐
                    │ Checkout  │
                    └─────┬─────┘
                          │
                  ┌───────┴────────┐
                  ▼                ▼
             Inventory          Pricing
             Reservation        Validation
                  │                │
                  └───────┬────────┘
                          ▼
                    ┌───────────┐
                    │   Order   │
                    └─────┬─────┘
                          │
                          ▼
                    ┌───────────┐
                    │  Payment  │
                    └───────────┘
```

This establishes the customer shopping layer while preserving clean domain boundaries for the next module: **`12 — Wishlist Management PRD`**.