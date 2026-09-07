# 09 — Pricing Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Pricing Management  
**Module ID:** `PRICING-09`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Product Management, Seller Management, Store Management, Category & Taxonomy, Authorization, Common

---

# 1. Overview

Pricing Management is responsible for determining the monetary value of products and purchasable variants within Fynza.

The module manages:

- Product prices.
- Variant prices.
- Currency.
- Price types.
- Sale prices.
- Promotional prices.
- Price validity periods.
- Customer-specific pricing.
- Seller/store pricing.
- Price history.
- Price rules.
- Tax-inclusive/exclusive representation where applicable.
- Price rounding.
- Price calculation.
- Price snapshots for transactions.

The central architectural principle is:

```text
Product
    │
    │ What is it?
    ▼
Pricing
    │
    │ How much does it cost?
    ▼
Cart / Order
```

Product Management should **not** become responsible for pricing logic.

---

# 2. Goals

Pricing Management should:

1. Provide centralized price management.
2. Support product and variant pricing.
3. Support multiple currencies.
4. Support regular and sale prices.
5. Support scheduled prices.
6. Maintain price history.
7. Support seller/store-level pricing.
8. Provide reliable price calculation.
9. Prevent invalid prices.
10. Provide transaction-safe price snapshots.
11. Support future promotions and discounts.
12. Support regional pricing.
13. Support customer-specific pricing where required.
14. Provide APIs for storefronts, carts, and orders.
15. Maintain strong financial auditability.

---

# 3. Non-Goals

Pricing Management does not own:

- Product definitions.
- Product inventory.
- Product categories.
- Orders.
- Payments.
- Seller verification.
- Shipping.
- Customer accounts.
- Coupons as a standalone domain.
- Promotion campaign ownership.

Promotion/Coupon Management may later provide discounts that Pricing evaluates.

---

# 4. Pricing Architecture

```text
Product
   │
   └── Product Variant
            │
            ▼
         Pricing
            │
      ┌─────┼────────┐
      ▼     ▼        ▼
   Base    Sale    Pricing Rules
   Price   Price
      │     │        │
      └─────┼────────┘
            ▼
       Effective Price
            │
            ▼
       Cart / Order
```

---

# 5. Core Pricing Concepts

Fynza should distinguish:

```text
Base Price
Sale Price
Effective Price
Discount
Tax
Final Price
```

Example:

```text
Base Price      = GHS 1,500
Sale Price      = GHS 1,299
Discount        = GHS 201
Tax             = calculated separately
Final Price     = calculated according to tax configuration
```

Do not store only a single `price` field and attempt to encode every pricing concept into it.

---

# 6. PRICE-FR-001 — Base Price

Every sellable product/variant should have a base price when required by the product type.

Example:

```text
Product: Laptop
SKU: LAP-001

Base Price:
GHS 8,500.00
```

The base price represents the normal selling price before temporary discounts.

---

# 7. PRICE-FR-002 — Variant Pricing

Variants may have independent prices.

Example:

```text
T-Shirt

Small  → GHS 100
Medium → GHS 100
Large  → GHS 110
XL     → GHS 120
```

Pricing should therefore support:

```text
Product
   │
   ├── Variant A → Price
   ├── Variant B → Price
   └── Variant C → Price
```

A variant may inherit the parent product price when business rules allow.

---

# 8. Price Ownership

Recommended ownership:

```text
Seller
   │
   ▼
Store
   │
   ▼
Product
   │
   ▼
Variant
   │
   ▼
Price
```

A seller/store should be authorized to manage prices only for products they own.

Authorization must validate:

```text
Authenticated User
       │
       ▼
Seller Membership
       │
       ▼
Store Ownership
       │
       ▼
Product Ownership
       │
       ▼
Price
```

---

# 9. PRICE-FR-003 — Currency

Every price must have an explicit currency.

Example:

```text
amount   = 1299.00
currency = GHS
```

Recommended ISO currency codes:

```text
GHS
USD
EUR
GBP
NGN
```

Do not infer currency from the user's browser or locale.

---

# 10. Money Representation

Avoid floating-point numbers for financial calculations.

Bad:

```java
double price;
```

Recommended:

```java
BigDecimal amount;
Currency currency;
```

Example:

```java
BigDecimal amount = new BigDecimal("1299.99");
```

Database recommendation:

```sql
DECIMAL(19,4)
```

or an equivalent fixed-precision monetary representation.

---

# 11. PRICE-FR-004 — Price Status

Recommended price statuses:

```text
DRAFT
ACTIVE
SCHEDULED
EXPIRED
DISABLED
```

Example:

```text
Current Price
     │
     ▼
ACTIVE

Future Price
     │
     ▼
SCHEDULED
```

---

# 12. Price Validity

Prices can have validity periods.

```text
validFrom
validUntil
```

Example:

```text
Normal Price
01 Jan → 30 Nov

Black Friday Price
01 Dec → 05 Dec
```

The system should determine the effective price using the current timestamp and pricing rules.

---

# 13. PRICE-FR-005 — Scheduled Pricing

Sellers/admins may schedule prices.

Example:

```text
Current:
GHS 1,500

Scheduled:
GHS 1,299

Starts:
2026-11-27 00:00
```

At the effective time, the scheduled price becomes active.

A scheduled job may assist with activation, but price resolution should not depend entirely on a cron job.

Prefer:

```text
effectiveFrom <= now
AND
(effectiveUntil IS NULL OR effectiveUntil > now)
```

This makes pricing resilient if a scheduled worker is delayed.

---

# 14. PRICE-FR-006 — Sale Price

A temporary discounted price may be defined.

Example:

```text
Regular Price: GHS 2,000
Sale Price:    GHS 1,700
```

The system should calculate:

```text
Discount Amount = 2,000 - 1,700
               = 300

Discount % = 15%
```

Derived values should generally be calculated rather than stored redundantly.

---

# 15. Discount vs Price

Pricing should distinguish between:

```text
Price
```

and:

```text
Discount
```

For example:

```text
Base Price
   ↓
Promotion
   ↓
Discount
   ↓
Effective Price
```

The Promotion module may later determine the discount.

Pricing provides the final price calculation engine.

---

# 16. PRICE-FR-007 — Price Rules

The system should support pricing rules.

Examples:

```text
If customer group = VIP
→ 10% discount

If quantity >= 10
→ bulk price

If region = Ghana
→ GHS price

If region = Nigeria
→ NGN price
```

Initially Fynza should keep pricing rules simple.

Complex promotion rules should belong to the Promotion module.

---

# 17. Customer-Specific Pricing

Future enterprise functionality may support:

```text
Retail Price
Wholesale Price
VIP Price
Business Price
Partner Price
```

Example:

```text
Customer Type: RETAIL
Price: GHS 100

Customer Type: WHOLESALE
Price: GHS 85
```

This should be implemented through pricing tiers rather than duplicating products.

---

# 18. PRICE-FR-008 — Quantity-Based Pricing

Fynza may support tiered pricing.

Example:

```text
Quantity       Unit Price

1–4            GHS 100
5–9            GHS 90
10–49          GHS 80
50+            GHS 70
```

Model:

```text
Price
 │
 └── PriceTier
      ├── minQuantity
      ├── maxQuantity
      └── unitPrice
```

---

# 19. Price List

A price list can group prices under a commercial context.

Example:

```text
Retail Price List
Wholesale Price List
Corporate Price List
Ghana Price List
Nigeria Price List
```

Recommended model:

```text
PriceList
   │
   └── Price
```

Initially Fynza can use a default price list.

---

# 20. Recommended Pricing Model

```text
PriceList
    │
    ├── Product Price
    │
    └── Variant Price
```

Example:

```text
Retail
│
├── Laptop A → GHS 8,500
├── Laptop B → GHS 6,900
└── Laptop C → GHS 12,000
```

---

# 21. PRICE-FR-009 — Price History

Every significant price change should be traceable.

Example:

```text
01 Sep → GHS 1,500
10 Sep → GHS 1,400
20 Sep → GHS 1,299
```

Model:

```text
PriceHistory
-------------------------
id
priceId
oldAmount
newAmount
oldCurrency
newCurrency
changedBy
reason
createdAt
```

This is important for:

- Auditing.
- Seller reporting.
- Dispute resolution.
- Analytics.
- Pricing analysis.

---

# 22. Price Versioning

An alternative to mutating prices directly is versioned pricing.

```text
Price Version 1
GHS 1,500
ACTIVE

Price Version 2
GHS 1,400
ACTIVE

Price Version 3
GHS 1,299
SCHEDULED
```

For an enterprise marketplace, immutable price versions are recommended.

---

# 23. Recommended Price Model

```text
Price
-------------------------
id
publicId
priceListId
productId
variantId
amount
currency
status
validFrom
validUntil
createdBy
createdAt
updatedAt
```

Constraints should prevent ambiguous overlapping active prices.

---

# 24. Price Scope

A price should have an explicit scope.

Possible scopes:

```text
PRODUCT
VARIANT
```

Future scopes:

```text
CUSTOMER_GROUP
REGION
CHANNEL
PRICE_LIST
```

Example:

```text
Price
 ├── Product
 ├── Variant
 ├── Price List
 └── Region
```

---

# 25. PRICE-FR-010 — Effective Price

The system should expose an effective price.

Example:

```text
Base:
GHS 1,500

Active Sale:
GHS 1,299

Effective:
GHS 1,299
```

API consumers should not have to reproduce pricing logic themselves.

---

# 26. Price Resolution Engine

Recommended service:

```text
PriceResolver
```

Input:

```text
productId
variantId
quantity
customerContext
storeContext
currency
timestamp
```

Output:

```text
PriceResult
```

Example:

```text
PriceResult
-------------------------
basePrice
discount
effectivePrice
currency
priceSource
validFrom
validUntil
```

---

# 27. Price Resolution Flow

```text
Request
   │
   ▼
Identify Product/Variant
   │
   ▼
Determine Price List
   │
   ▼
Load Applicable Prices
   │
   ▼
Evaluate Validity
   │
   ▼
Evaluate Pricing Rules
   │
   ▼
Apply Discount
   │
   ▼
Round Money
   │
   ▼
Return Effective Price
```

---

# 28. Price Rounding

All monetary calculations must have deterministic rounding rules.

Example:

```text
Scale: 2 decimal places
Rounding: HALF_UP
```

However, rounding policy should be configurable per currency/business requirement.

Never use binary floating-point arithmetic for monetary calculations.

---

# 29. Tax Boundary

Tax should not be deeply coupled to Product.

Recommended future architecture:

```text
Pricing
   │
   ▼
Tax
   │
   ▼
Final Amount
```

Depending on jurisdiction, taxes may be:

- Included in displayed price.
- Added at checkout.
- Calculated by a Tax module.

Pricing should expose enough information for Tax calculation.

---

# 30. Display Price vs Transaction Price

Fynza must distinguish:

```text
Display Price
```

from:

```text
Order Price Snapshot
```

A product page may show:

```text
GHS 1,299
```

But an order must preserve the price that was actually accepted.

---

# 31. Order Price Snapshot

When an order is created:

```text
OrderItem
├── productId
├── variantId
├── unitPrice
├── currency
├── quantity
├── discount
├── tax
└── total
```

The order must not continuously query the current price.

Example:

```text
Product price today:
GHS 1,500

Customer purchased:
GHS 1,299

Tomorrow:
Price becomes GHS 1,700
```

The existing order remains:

```text
GHS 1,299
```

---

# 32. Cart Pricing

Cart prices may change before checkout.

Therefore:

```text
Cart
   │
   ▼
Price Resolver
   │
   ▼
Current Price
```

At checkout:

```text
Cart
   │
   ▼
Recalculate
   │
   ▼
Confirm
   │
   ▼
Order Snapshot
```

Never trust a price supplied by the frontend.

---

# 33. Price Validation

Validate:

- Amount is positive where required.
- Currency is supported.
- Decimal precision is valid.
- Product/variant exists.
- Seller owns product.
- Price list exists.
- Dates are valid.
- `validUntil > validFrom`.
- No invalid overlapping prices.
- Sale price is not invalidly higher than base price where rules prohibit it.

---

# 34. Seller Price Management

Seller workflow:

```text
Seller
  │
  ▼
Select Product
  │
  ▼
Set Price
  │
  ▼
Validate
  │
  ▼
Save
  │
  ▼
Price History
  │
  ▼
Publish / Activate
```

Seller must never be able to modify another seller's prices.

---

# 35. Administrative Pricing

Administrators may have additional capabilities:

```text
View prices
Create price
Update price
Disable price
Schedule price
View history
Override price
```

Any override must be audited.

---

# 36. Price Override

Administrative overrides should be explicit.

```text
Price Override
-------------------------
id
priceId
reason
oldAmount
newAmount
approvedBy
createdAt
```

Do not silently overwrite seller pricing.

---

# 37. Price Import

Enterprise sellers may eventually need bulk pricing.

Supported formats could include:

```text
CSV
Excel
API
```

Example:

```text
SKU,PRICE,CURRENCY
LAP-001,8500,GHS
LAP-002,6900,GHS
```

Bulk imports should use asynchronous processing for large datasets.

---

# 38. Pricing API — REST

## Public

```text
GET /products/{productId}/price
GET /products/{productId}/prices
GET /products/{productId}/variants/{variantId}/price
```

## Seller

```text
POST  /seller/prices
PATCH /seller/prices/{id}
POST  /seller/prices/{id}/activate
POST  /seller/prices/{id}/disable
POST  /seller/prices/{id}/schedule
GET   /seller/products/{productId}/prices
GET   /seller/prices/{id}/history
```

## Admin

```text
GET   /admin/prices
GET   /admin/prices/{id}
PATCH /admin/prices/{id}
POST  /admin/prices/{id}/override
GET   /admin/prices/{id}/history
```

---

# 39. GraphQL Queries

```graphql
price(productId: ID!, variantId: ID): Price

effectivePrice(
    productId: ID!
    variantId: ID
    quantity: Int
): PriceResult

prices(
    productId: ID
): [Price!]!

priceHistory(priceId: ID!): [PriceHistory!]!
```

---

# 40. GraphQL Mutations

```graphql
createPrice
updatePrice
activatePrice
disablePrice
schedulePrice

createPriceTier
updatePriceTier
deletePriceTier

createPriceList
updatePriceList

overridePrice
```

---

# 41. GraphQL Structure

```text
pricing/
└── graphql/
    ├── resolver/
    │   ├── PriceResolver
    │   ├── PriceListResolver
    │   └── PricingAdminResolver
    │
    ├── input/
    │   ├── CreatePriceInput
    │   ├── UpdatePriceInput
    │   ├── PriceFilterInput
    │   ├── PriceContextInput
    │   └── PriceTierInput
    │
    └── payload/
        ├── PricePayload
        ├── PriceResultPayload
        ├── PriceListPayload
        └── PriceHistoryPayload
```

---

# 42. Authorization

Permissions:

```text
price.read
price.create
price.update
price.delete
price.activate
price.disable
price.schedule
price.history.read

price.bulk_import
price.override
```

Recommended scopes:

```text
STORE
PRODUCT
VARIANT
GLOBAL
```

Seller example:

```text
seller
   │
   ▼
store
   │
   ▼
product
   │
   ▼
variant
   │
   ▼
price
```

---

# 43. Domain Events

Recommended events:

```text
PRICE_CREATED
PRICE_UPDATED
PRICE_ACTIVATED
PRICE_DISABLED
PRICE_SCHEDULED
PRICE_EXPIRED

PRICE_CHANGED
PRICE_OVERRIDE_CREATED

PRICE_LIST_CREATED
PRICE_LIST_UPDATED

PRICE_TIER_CREATED
PRICE_TIER_UPDATED
PRICE_TIER_DELETED
```

---

# 44. Event Consumers

```text
Pricing
   │
   ├──► Product
   ├──► Cart
   ├──► Order
   ├──► Search
   ├──► Analytics
   ├──► Notification
   └──► Promotion
```

Example:

```text
PRICE_CHANGED
      │
      ├──► Search index update
      ├──► Cache invalidation
      ├──► Analytics
      └──► Seller notification
```

---

# 45. Caching

Effective prices can be cached carefully.

Example:

```text
price:product:{productId}
price:variant:{variantId}
price:effective:{productId}:{contextHash}
```

Cache must be invalidated when:

```text
PRICE_CHANGED
PRICE_ACTIVATED
PRICE_DISABLED
PRICE_EXPIRED
```

Never allow stale cache values to compromise checkout correctness.

Checkout should always perform authoritative price validation.

---

# 46. Database Views

Pricing read models may combine:

```text
Product
+
Variant
+
Current Price
+
Sale Price
+
Currency
```

Example:

```text
ProductPriceView
-------------------------
productId
variantId
productName
sku
basePrice
effectivePrice
currency
discountAmount
discountPercentage
```

These are read models only.

Do not turn them into domain entities.

---

# 47. Pricing Module Structure

```text
pricing/
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
└── PricingModule.java
```

Tests:

```text
test/
└── pricing/
    ├── service/
    ├── repository/
    ├── policy/
    ├── controller/
    ├── graphql/
    ├── integration/
    └── security/
```

---

# 48. Suggested Entities

```text
Price
PriceList
PriceTier
PriceHistory
PriceOverride
```

Future:

```text
PricingRule
CustomerPrice
RegionalPrice
```

---

# 49. Database Relationships

```text
PriceList
   │
   └──< Price
           │
           ├── Product
           │
           └── Variant

Price
   │
   └──< PriceHistory

Price
   │
   └──< PriceOverride

Price
   │
   └──< PriceTier
```

---

# 50. Important Constraints

Recommended constraints:

```text
Price.amount >= 0
```

and:

```text
currency IS NOT NULL
```

plus appropriate uniqueness/index rules.

For example:

```text
(priceListId, productId, variantId, validity period)
```

must not produce ambiguous active prices.

---

# 51. Performance Requirements

Pricing should support:

- High-volume product-page requests.
- High-volume cart recalculation.
- Checkout price validation.
- Bulk seller updates.

Recommended indexes:

```text
Price
├── productId
├── variantId
├── priceListId
├── status
├── currency
├── validFrom
└── validUntil
```

---

# 52. Financial Integrity

Pricing is a financially sensitive module.

Requirements:

- Never use floating-point monetary calculations.
- Never trust client-provided prices.
- Use database transactions where required.
- Preserve price history.
- Preserve order snapshots.
- Audit administrative overrides.
- Use deterministic rounding.
- Avoid silent price mutation.
- Validate currency.
- Validate price boundaries.

---

# 53. Concurrency

Potential race condition:

```text
Seller A
   │
   ├── changes price → GHS 100
   │
Seller B
   │
   └── changes price → GHS 80
```

Use:

- Optimistic locking.
- Version numbers.
- Transactional updates.

Example:

```java
@Version
private Long version;
```

The exact implementation should depend on whether the system uses immutable price versions or mutable price records.

---

# 54. Security Testing

Test:

- Seller changing another seller's price.
- Seller changing another store's price.
- Customer creating prices.
- Unauthorized admin override.
- Price manipulation through GraphQL.
- Price manipulation through REST.
- Frontend price tampering.
- Checkout using stale price.
- Currency manipulation.
- Negative price injection.
- Excessive precision.
- Concurrent price updates.

---

# 55. Business Scenarios

## Scenario 1 — Normal Product

```text
Product
Laptop

Price:
GHS 8,500
```

Customer sees:

```text
GHS 8,500
```

---

## Scenario 2 — Sale

```text
Base:
GHS 8,500

Sale:
GHS 7,999
```

Customer sees:

```text
GHS 7,999
Was GHS 8,500
```

---

## Scenario 3 — Scheduled Sale

```text
Today:
GHS 8,500

Tomorrow:
GHS 7,999
```

At the scheduled time:

```text
Effective Price = GHS 7,999
```

---

## Scenario 4 — Price Change After Cart

```text
Cart:
GHS 100

Seller changes:
GHS 120
```

At checkout:

```text
Recalculate
     │
     ▼
GHS 120
     │
     ▼
Customer notified
```

The customer should not be charged the old price unless Fynza explicitly implements price-locking.

---

# 56. Price Locking

Future capability:

```text
Cart Price Lock
```

Example:

```text
Price locked:
30 minutes
```

This is useful for:

- Flash sales.
- Limited offers.
- High-volatility pricing.
- Checkout protection.

However, it adds complexity and should not be implemented by default.

---

# 57. User Stories

### US-001 — Set Product Price

> As a seller, I want to set a price for my product so that customers can purchase it.

### US-002 — Variant Pricing

> As a seller, I want different variants to have different prices so that each configuration can be priced correctly.

### US-003 — Sale Price

> As a seller, I want to offer temporary sale prices so that I can run promotions.

### US-004 — Scheduled Pricing

> As a seller, I want to schedule a future price so that pricing changes automatically at the correct time.

### US-005 — Price History

> As an administrator, I want to see price history so that pricing changes are auditable.

### US-006 — Effective Price

> As a customer, I want to see the current effective price so that I know what I will pay.

### US-007 — Secure Checkout

> As a customer, I want checkout to validate the current price so that the transaction uses an authoritative price.

### US-008 — Bulk Pricing

> As a seller, I want quantity-based pricing so that I can offer wholesale discounts.

---

# 58. Milestones

## M1 — Pricing Foundation

- [ ] Price entity.
- [ ] Money value handling.
- [ ] Currency support.
- [ ] Price CRUD.
- [ ] Validation.
- [ ] Seller ownership.

## M2 — Effective Pricing

- [ ] Active price.
- [ ] Sale price.
- [ ] Scheduled price.
- [ ] Price resolver.
- [ ] Price validity.

## M3 — Price History

- [ ] Immutable history.
- [ ] Audit.
- [ ] Administrative overrides.
- [ ] Price versioning.

## M4 — Advanced Pricing

- [ ] Price lists.
- [ ] Price tiers.
- [ ] Quantity pricing.
- [ ] Customer groups.
- [ ] Regional pricing.

## M5 — Commerce Integration

- [ ] Product integration.
- [ ] Cart integration.
- [ ] Checkout integration.
- [ ] Order price snapshots.
- [ ] Promotion integration.

## M6 — Optimization

- [ ] Pricing cache.
- [ ] Read models.
- [ ] Bulk import.
- [ ] Performance optimization.
- [ ] Pricing analytics.

---

# 59. Definition of Done

- [ ] Product prices supported.
- [ ] Variant prices supported.
- [ ] Currency supported.
- [ ] Base prices supported.
- [ ] Sale prices supported.
- [ ] Scheduled prices supported.
- [ ] Effective-price resolution implemented.
- [ ] Price history implemented.
- [ ] Price auditing implemented.
- [ ] Seller ownership validation implemented.
- [ ] Administrative override implemented.
- [ ] Price lists implemented where required.
- [ ] Quantity pricing implemented where required.
- [ ] REST API implemented.
- [ ] GraphQL API implemented.
- [ ] Domain events implemented.
- [ ] Read models implemented where required.
- [ ] Caching implemented where appropriate.
- [ ] Checkout price validation implemented.
- [ ] Order price snapshots implemented.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] E2E tests pass.
- [ ] Documentation completed.

---

# 60. Architectural Boundaries

The final separation should be:

```text
┌──────────────────────┐
│ Category Management  │
│ What category?       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Product Management   │
│ What is being sold?  │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Pricing Management   │
│ How much?            │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Inventory Management │
│ How many?             │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Cart / Order         │
│ What was purchased?  │
└──────────────────────┘
```

---

# 61. Key Architectural Principle

> **Pricing Management owns the rules and authoritative calculation of monetary values. Product Management owns what is being sold; Pricing Management owns how that product is priced.**

The most important rule is:

```text
Product.price ❌

Product
   │
   ▼
Pricing
   │
   ▼
Effective Price
```

This separation allows Fynza to later introduce:

- Promotions.
- Coupons.
- Wholesale pricing.
- Regional pricing.
- Customer-specific pricing.
- Dynamic pricing.
- Subscription pricing.
- Marketplace commissions.

without turning the Product module into a monolithic pricing engine.

---

# 62. Recommended Fynza Flow

The core commerce flow should eventually become:

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
                    ▼
                  Order
```

This gives Fynza a clean foundation for the next major commerce domain:

**`10 — Inventory Management PRD`**