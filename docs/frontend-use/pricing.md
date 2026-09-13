# Pricing Management API — Frontend Integration Guide

Base paths: `/v1/seller/prices` (seller mutations) · `/v1/admin/prices` (admin mutations) · `POST /graphql` (all reads)  
All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**  
> Use REST only to create, update, or transition price state.  
> Use GraphQL for all reads — product page price display, effective price, price history.  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [Price Lifecycle Overview](#3-price-lifecycle-overview)
4. [Seller — Create a Price (REST)](#4-seller--create-a-price-rest)
5. [Seller — Update a Price (REST)](#5-seller--update-a-price-rest)
6. [Seller — Activate a Price (REST)](#6-seller--activate-a-price-rest)
7. [Seller — Disable a Price (REST)](#7-seller--disable-a-price-rest)
8. [Seller — Schedule a Price (REST)](#8-seller--schedule-a-price-rest)
9. [Seller — Price Tiers (REST)](#9-seller--price-tiers-rest)
10. [Admin — Update a Price (REST)](#10-admin--update-a-price-rest)
11. [Admin — Override a Price (REST)](#11-admin--override-a-price-rest)
12. [Cart — Validate Prices Before Checkout (REST)](#12-cart--validate-prices-before-checkout-rest)
13. [Read Prices (GraphQL)](#13-read-prices-graphql)
14. [Status Transition Rules](#14-status-transition-rules)
15. [Error Reference](#15-error-reference)
16. [Quick Reference](#16-quick-reference)

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

`data` is `null` for void endpoints (delete). `message` is always safe to display.

### GraphQL envelope

```json
{
  "data": {
    "effectivePrice": { ... }
  }
}
```

On error:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Price not found",
      "extensions": { "status": 404 }
    }
  ]
}
```

Check `errors[0].message` and `errors[0].extensions.status` for GraphQL error handling.

---

## 2. Reference Types

### `PriceStatus`

| Value | Meaning |
|---|---|
| `DRAFT` | Created by seller; not yet active — not used for checkout |
| `ACTIVE` | The live price — buyers see this |
| `SCHEDULED` | Will activate automatically at `validFrom` |
| `EXPIRED` | Past its `validUntil` date |
| `DISABLED` | Manually disabled by seller or admin |

### `SupportedCurrency`

| Value | Description |
|---|---|
| `GHS` | Ghanaian Cedi (platform default) |
| `USD` | US Dollar |
| `EUR` | Euro |
| `GBP` | British Pound |
| `NGN` | Nigerian Naira |

### TypeScript interfaces

```typescript
interface Price {
  publicId:   string;          // UUID — use for all subsequent calls
  productId:  string;          // UUID of the product this price belongs to
  variantId:  string | null;   // UUID of the variant (null = applies to base product)
  amount:     string;          // Decimal string, e.g. "120.00" (DECIMAL 19,4 precision)
  saleAmount: string | null;   // Discounted price; null if no promotion
  currency:   string;          // e.g. "GHS"
  status:     PriceStatus;
  validFrom:  string | null;   // ISO-8601 UTC; null = active immediately
  validUntil: string | null;   // ISO-8601 UTC; null = no expiry
  isActive:   boolean;
  tiers:      PriceTier[];
  createdAt:  string;
  updatedAt:  string;
}

interface PriceTier {
  publicId:    string;
  minQuantity: number;         // inclusive lower bound
  maxQuantity: number | null;  // inclusive upper bound; null = open-ended
  unitPrice:   string;         // Decimal string
  currency:    string;
  isActive:    boolean;
  createdAt:   string;
}

interface PriceResult {
  priceId:         string;
  productId:       string;
  variantId:       string | null;
  basePrice:       string;     // Original amount at 2dp
  salePrice:       string | null;
  effectivePrice:  string;     // The price the buyer pays — use this for display and checkout
  discountAmount:  string;     // basePrice - effectivePrice
  discountPercent: string;     // Discount as a percentage, 2dp
  currency:        string;
  validFrom:       string | null;
  validUntil:      string | null;
}

interface CartValidateItem {
  productId:     string;       // UUID
  variantId?:    string;       // UUID, optional
  quantity:      number;       // ≥ 1
  currency:      SupportedCurrency;
  expectedPrice: string;       // The price the user saw when they added to cart
}

interface CartItemValidationResult {
  productId:    string;
  variantId:    string | null;
  quantity:     number;
  expectedPrice: string;
  currentPrice: string | null;  // null when the price can no longer be resolved
  priceChanged: boolean;        // true = price mismatch or item unavailable
  currency:     string;
  message:      string;         // Safe to display directly to the user
}
```

---

## 3. Price Lifecycle Overview

```
Seller creates price  →  DRAFT
         │
         ▼
POST …/{id}/activate    →  ACTIVE    ← The live checkout price
         │
    ┌────┼─────────────┐
    ▼    ▼             ▼
 Seller  Admin     Seller
disable  override  schedule
    │                  │
    ▼                  ▼
DISABLED           SCHEDULED  → auto-activates at validFrom
```

- Only one `ACTIVE` price per (product + variant + currency) combination is allowed at a time.  
  Attempting to activate a second one returns `409 Conflict`.
- A `SCHEDULED` price does **not** require a cron job — it activates automatically when its `validFrom` is reached and a price is queried.
- `saleAmount`, when present, is the effective checkout price — `amount` is the "was" price shown as a strikethrough.
- Tier pricing applies when `quantity > 1` and matching tiers are configured. The tier with the highest `minQuantity` that covers the requested quantity wins.

---

## 4. Seller — Create a Price (REST)

Creates a price in `DRAFT` status. A draft price is not used for checkout until activated.

```
POST /v1/seller/prices
Authorization: Bearer <accessToken>   ← requires price.create permission
Content-Type: application/json
```

**Request body**

```json
{
  "productId":   "9a3c1d2e-...",
  "variantId":   null,
  "amount":      "120.00",
  "saleAmount":  "99.99",
  "currency":    "GHS",
  "priceListId": null,
  "validFrom":   null,
  "validUntil":  null
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `productId` | Yes | UUID | The product this price belongs to |
| `variantId` | No | UUID | Omit for a product-level price; provide for a variant-specific price |
| `amount` | Yes | ≥ 0, up to 15 integer + 4 decimal digits | The full/base price |
| `saleAmount` | No | ≥ 0, ≤ `amount` | Promotional price; `effectivePrice` in queries will reflect this |
| `currency` | Yes | `GHS` \| `USD` \| `EUR` \| `GBP` \| `NGN` | |
| `priceListId` | No | UUID | Omit to use the platform default price list |
| `validFrom` | No | ISO-8601 UTC instant | Omit to make it valid from the moment of activation |
| `validUntil` | No | ISO-8601 UTC instant, after `validFrom` | Omit for no expiry |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Price created successfully",
  "data": {
    "publicId":   "f1a2b3c4-...",
    "productId":  "9a3c1d2e-...",
    "variantId":  null,
    "amount":     "120.00",
    "saleAmount": "99.99",
    "currency":   "GHS",
    "status":     "DRAFT",
    "validFrom":  null,
    "validUntil": null,
    "isActive":   true,
    "tiers":      [],
    "createdAt":  "2026-09-13T08:00:00Z",
    "updatedAt":  "2026-09-13T08:00:00Z"
  }
}
```

Store the returned `publicId` — pass it to all subsequent seller price endpoints.

---

## 5. Seller — Update a Price (REST)

Updates a `DRAFT` or `DISABLED` price. All fields are optional — send only what changed. The server merges the new values with the existing ones before validation, so partial updates are safe.

```
PATCH /v1/seller/prices/{id}
Authorization: Bearer <accessToken>   ← requires price.update permission
Content-Type: application/json
```

**Path parameters**

| Parameter | Type | Notes |
|---|---|---|
| `id` | `UUID` | The price's `publicId` |

**Request body**

```json
{
  "amount":     "115.00",
  "saleAmount": "95.00",
  "reason":     "Adjusted for back-to-school season"
}
```

| Field | Constraints | Notes |
|---|---|---|
| `amount` | ≥ 0 | |
| `saleAmount` | ≥ 0, ≤ merged `amount` | Must not exceed `amount` after merge |
| `currency` | `SupportedCurrency` | |
| `validFrom` | ISO-8601 UTC | |
| `validUntil` | ISO-8601 UTC, after `validFrom` | Cross-checked only when both `validFrom` and `validUntil` are present in the request |
| `reason` | Free text | Stored in price history |

**Response `200`**

```json
{
  "success": true,
  "message": "Price updated successfully",
  "data": { ...Price }
}
```

`403 Forbidden` if the seller does not own this price record.

---

## 6. Seller — Activate a Price (REST)

Transitions: `DRAFT | DISABLED → ACTIVE`. Makes the price the live checkout price for its product/variant/currency combination.

```
POST /v1/seller/prices/{id}/activate
Authorization: Bearer <accessToken>   ← requires price.activate permission
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Price activated successfully",
  "data": { ...Price (status: "ACTIVE") }
}
```

**`409 Conflict`** if another `ACTIVE` price already exists for the same (product + variant + currency). Disable the existing price first, then activate the new one.

**UI guidance**: only show the Activate button for prices in `DRAFT` or `DISABLED` status. After success, the product's checkout price is now this record's `effectivePrice`.

---

## 7. Seller — Disable a Price (REST)

Transitions: `ACTIVE → DISABLED`. Removes the price from checkout without deleting it.

```
POST /v1/seller/prices/{id}/disable
Authorization: Bearer <accessToken>   ← requires price.disable permission
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Price disabled successfully",
  "data": { ...Price (status: "DISABLED") }
}
```

Once disabled, the product has no active price in that currency. Buyers will see "Price unavailable" if no other active price exists. The seller can activate a different price or re-activate this one.

---

## 8. Seller — Schedule a Price (REST)

Transitions: `DRAFT → SCHEDULED`. The price will automatically become the effective price at `validFrom`, with no further action required.

```
POST /v1/seller/prices/{id}/schedule
Authorization: Bearer <accessToken>   ← requires price.schedule permission
Content-Type: application/json
```

**Request body**

```json
{
  "validFrom":  "2026-11-01T00:00:00Z",
  "validUntil": "2026-11-30T23:59:59Z"
}
```

| Field | Required | Notes |
|---|---|---|
| `validFrom` | Yes | ISO-8601 UTC; must be in the future |
| `validUntil` | No | ISO-8601 UTC; must be strictly after `validFrom` if provided |

**Response `200`**

```json
{
  "success": true,
  "message": "Price scheduled successfully",
  "data": { ...Price (status: "SCHEDULED") }
}
```

**`400 Bad Request`** if `validUntil` is not after `validFrom`.

**Important**: scheduling does **not** displace the currently active price. The scheduled price becomes effective only when it is queried and `now >= validFrom`. Configure the seller UI to show a warning if an active price already exists for the same product/variant/currency combination and will overlap with the scheduled window.

---

## 9. Seller — Price Tiers (REST)

Price tiers enable volume-based discounts — a lower per-unit price when the buyer orders in quantity. Tiers are associated with a specific price record (identified by its `publicId`).

### Add a tier

```
POST /v1/seller/prices/tiers
Authorization: Bearer <accessToken>   ← requires price.create permission
Content-Type: application/json
```

**Request body**

```json
{
  "priceId":     "f1a2b3c4-...",
  "minQuantity": 10,
  "maxQuantity": 49,
  "unitPrice":   "108.00",
  "currency":    "GHS"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `priceId` | Yes | UUID | The `publicId` of the parent price |
| `minQuantity` | Yes | ≥ 1 | Inclusive lower bound |
| `maxQuantity` | No | ≥ 1, ≥ `minQuantity` | Inclusive upper bound; omit for an open-ended tier (e.g. "50+") |
| `unitPrice` | Yes | ≥ 0 | Per-unit price at this tier — typically less than the base price `amount` |
| `currency` | Yes | `SupportedCurrency` | Should match the parent price's currency |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Price tier created successfully",
  "data": {
    "publicId":    "a1b2c3d4-...",
    "minQuantity": 10,
    "maxQuantity": 49,
    "unitPrice":   "108.00",
    "currency":    "GHS",
    "isActive":    true,
    "createdAt":   "2026-09-13T09:00:00Z"
  }
}
```

**Tier selection logic**: when a buyer adds `quantity=20` to their cart, the resolver picks the tier with the highest `minQuantity` that covers 20 — so if both a `10–49` and a `5–99` tier exist, the `10–49` tier wins (more specific). If quantity is `1`, tiers are skipped entirely and `saleAmount` (or `amount`) is used.

---

### Update a tier

All fields are required (not a patch operation — replaces all tier values).

```
PATCH /v1/seller/prices/tiers/{tierId}
Authorization: Bearer <accessToken>   ← requires price.update permission
Content-Type: application/json
```

**Request body**

```json
{
  "priceId":     "f1a2b3c4-...",
  "minQuantity": 10,
  "maxQuantity": 99,
  "unitPrice":   "105.00",
  "currency":    "GHS"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Price tier updated successfully",
  "data": { ...PriceTier }
}
```

---

### Delete a tier

Soft-deletes the tier (`isActive = false`). It will no longer be applied to quantity calculations.

```
DELETE /v1/seller/prices/tiers/{tierId}
Authorization: Bearer <accessToken>   ← requires price.delete permission
```

**Response `200`**

```json
{
  "success": true,
  "message": "Price tier deleted successfully",
  "data": null
}
```

---

## 10. Admin — Update a Price (REST)

Admins can update any price record without ownership restriction. The same patch semantics as the seller update apply.

```
PATCH /v1/admin/prices/{id}
Authorization: Bearer <accessToken>   ← ADMIN role + price.update permission
Content-Type: application/json
```

**Request body** — same as [§5](#5-seller--update-a-price-rest).

**Response `200`**

```json
{
  "success": true,
  "message": "Price updated successfully",
  "data": { ...Price }
}
```

---

## 11. Admin — Override a Price (REST)

Creates an audited admin override — the price's `amount` is changed immediately and a permanent `PriceOverride` record is saved. Use this for compliance, fraud, or error-correction scenarios where a full audit trail is required.

```
POST /v1/admin/prices/{id}/override
Authorization: Bearer <accessToken>   ← ADMIN role + price.override permission
Content-Type: application/json
```

**Request body**

```json
{
  "newAmount": "85.00",
  "reason":    "Correcting pricing error reported by compliance team"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `newAmount` | Yes | ≥ 0 | New price to apply immediately |
| `reason` | Yes | Non-blank | Stored in the immutable override history |

**Response `200`**

```json
{
  "success": true,
  "message": "Price override applied successfully",
  "data": { ...Price (amount: "85.00") }
}
```

The override is recorded in `price_overrides` and is visible via the `priceHistory` GraphQL query. It cannot be reversed via the API — create a new override to correct an incorrect one.

---

## 12. Cart — Validate Prices Before Checkout (REST)

**Always call this endpoint before rendering the order confirmation step.** Prices can change while items are in a cart. This endpoint compares the price each item was added at against the current authoritative server price, bypassing all caches.

```
POST /v1/cart/validate
Authorization: Bearer <accessToken>   ← CUSTOMER role required
Content-Type: application/json
```

**Request body**

```json
{
  "items": [
    {
      "productId":     "9a3c1d2e-...",
      "variantId":     null,
      "quantity":      2,
      "currency":      "GHS",
      "expectedPrice": "99.99"
    },
    {
      "productId":     "b4c5d6e7-...",
      "variantId":     "c4d5e6f7-...",
      "quantity":      1,
      "currency":      "GHS",
      "expectedPrice": "45.00"
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `items` | Yes | Non-empty list |
| `items[].productId` | Yes | UUID |
| `items[].variantId` | No | UUID; omit for base-product prices |
| `items[].quantity` | Yes | ≥ 1 — used for tier price selection |
| `items[].currency` | Yes | `SupportedCurrency` |
| `items[].expectedPrice` | Yes | The price shown to the user when they added the item — used for comparison |

**Response `200`**

```json
{
  "success": true,
  "message": "Cart validated",
  "data": {
    "valid": false,
    "items": [
      {
        "productId":     "9a3c1d2e-...",
        "variantId":     null,
        "quantity":      2,
        "expectedPrice": "99.99",
        "currentPrice":  "99.99",
        "priceChanged":  false,
        "currency":      "GHS",
        "message":       "Price confirmed"
      },
      {
        "productId":     "b4c5d6e7-...",
        "variantId":     "c4d5e6f7-...",
        "quantity":      1,
        "expectedPrice": "45.00",
        "currentPrice":  "52.00",
        "priceChanged":  true,
        "currency":      "GHS",
        "message":       "Price changed from 45.00 to 52.00"
      }
    ]
  }
}
```

**`data.valid`** is `true` only when every item's `priceChanged` is `false`.

**`currentPrice` is `null`** when the product's price can no longer be resolved (product removed, price disabled). In that case `priceChanged` is `true` and `message` will say "Price could not be resolved — item may no longer be available."

**UI guidance**

```
if (!response.data.valid) {
  // Highlight changed items and block the checkout CTA
  const changed = response.data.items.filter(i => i.priceChanged)
  // Show each changed item's message to the user
  // Offer "Update cart" to refresh prices, or "Remove item"
}
```

Do not allow the user to proceed to payment until `valid === true`.

---

## 13. Read Prices (GraphQL)

All price reads go through `POST /graphql`. Public queries (product page price display) require no auth token. Seller and admin queries require a bearer token with the appropriate role/permission.

---

### Display price on a product page (public)

Returns the most recent price record for a product/currency — useful for showing `amount`, `saleAmount`, and `status`. Does not return `effectivePrice` — use `effectivePrice` query for that.

```graphql
query GetPrice($productId: ID!, $currency: SupportedCurrency) {
  price(productId: $productId, currency: $currency) {
    publicId
    productId
    variantId
    amount
    saleAmount
    currency
    status
    validFrom
    validUntil
    isActive
    tiers {
      publicId
      minQuantity
      maxQuantity
      unitPrice
      currency
      isActive
    }
  }
}
```

**Variables**

```json
{ "productId": "9a3c1d2e-...", "currency": "GHS" }
```

Returns `null` if no price exists. `currency` defaults to `GHS` if omitted.

---

### Resolve the checkout price for a quantity (public)

Returns the resolved `effectivePrice` taking into account `saleAmount`, tier pricing, and scheduling. **Always use this query — not `price` — when showing the per-unit price the buyer will pay.**

```graphql
query EffectivePrice(
  $productId: ID!
  $variantId: ID
  $quantity:  Int
  $currency:  SupportedCurrency
) {
  effectivePrice(
    productId: $productId
    variantId: $variantId
    quantity:  $quantity
    currency:  $currency
  ) {
    priceId
    productId
    variantId
    basePrice
    salePrice
    effectivePrice
    discountAmount
    discountPercent
    currency
    validFrom
    validUntil
  }
}
```

**Variables**

```json
{
  "productId": "9a3c1d2e-...",
  "variantId": null,
  "quantity":  10,
  "currency":  "GHS"
}
```

- `quantity` defaults to `1` if omitted — no tier pricing applied.
- `effectivePrice` is what to store in `expectedPrice` when the user adds to cart, and what to compare against in the validate call.
- Returns a GraphQL error with `status: 404` if no active price exists for that product/currency.

---

### List all prices for a product (public)

Returns all price records for a product (any status), ordered by creation date descending.

```graphql
query PricesForProduct($productId: ID!, $currency: SupportedCurrency) {
  prices(productId: $productId, currency: $currency) {
    publicId
    amount
    saleAmount
    currency
    status
    validFrom
    validUntil
    isActive
    createdAt
  }
}
```

**Variables**

```json
{ "productId": "9a3c1d2e-...", "currency": "GHS" }
```

---

### Seller — my prices for a product

Returns the authenticated seller's price records for a given product, paginated. Requires `price.read` permission.

```graphql
query MyPrices($productId: ID!, $page: PageInput) {
  sellerPrices(productId: $productId, page: $page) {
    content {
      publicId
      amount
      saleAmount
      currency
      status
      validFrom
      validUntil
      isActive
      tiers {
        publicId
        minQuantity
        maxQuantity
        unitPrice
        isActive
      }
      createdAt
      updatedAt
    }
    pageInfo {
      currentPage
      totalPages
      totalElements
      hasNextPage
    }
  }
}
```

**Variables**

```json
{ "productId": "9a3c1d2e-...", "page": { "page": 0, "size": 20 } }
```

---

### Admin — all prices (paginated)

Lists all price records across all sellers. Requires `ADMIN` role and `price.read` permission.

```graphql
query AdminPrices($page: PageInput) {
  adminPrices(page: $page) {
    content {
      publicId
      productId
      variantId
      amount
      saleAmount
      currency
      status
      isActive
      createdAt
      updatedAt
    }
    pageInfo {
      currentPage
      totalPages
      totalElements
      hasNextPage
    }
  }
}
```

**Variables**

```json
{ "page": { "page": 0, "size": 50 } }
```

---

### Admin — single price by ID

Requires `ADMIN` role and `price.read` permission.

```graphql
query AdminGetPrice($id: ID!) {
  adminPrice(id: $id) {
    publicId
    productId
    variantId
    amount
    saleAmount
    currency
    status
    validFrom
    validUntil
    isActive
    tiers {
      publicId
      minQuantity
      maxQuantity
      unitPrice
      isActive
    }
    createdAt
    updatedAt
  }
}
```

**Variables**

```json
{ "id": "f1a2b3c4-..." }
```

Returns `null` if not found.

---

### Admin / Seller — price history

Returns the full audit trail of changes to a price record, most recent first. Seller access requires `price.history.read` permission and ownership of the price. Admin access requires `ADMIN` role and `price.history.read`.

```graphql
query PriceHistory($priceId: ID!) {
  priceHistory(priceId: $priceId) {
    id
    oldAmount
    newAmount
    oldCurrency
    newCurrency
    oldStatus
    newStatus
    changedBy
    reason
    createdAt
  }
}
```

**Variables**

```json
{ "priceId": "f1a2b3c4-..." }
```

---

## 14. Status Transition Rules

Only the following transitions are allowed. Any other attempt returns `409 Conflict` or `400 Bad Request`.

```
DRAFT     ──▶  ACTIVE      (seller activates — subject to overlap check)
DRAFT     ──▶  SCHEDULED   (seller schedules)
DRAFT     ──▶  DISABLED    (effectively a no-op; price was never live)

ACTIVE    ──▶  DISABLED    (seller or admin disables)

SCHEDULED ──▶  ACTIVE      (automatic — happens at query time when validFrom is reached)
SCHEDULED ──▶  DISABLED    (seller cancels the scheduled price)

DISABLED  ──▶  ACTIVE      (seller or admin re-activates — subject to overlap check)
DISABLED  ──▶  DRAFT       (not a valid transition; create a new draft instead)

EXPIRED   ──▶  (no further transitions — terminal state)
```

**One active price per slot**: at any moment there can be at most one `ACTIVE` price per (product + variant + currency). Attempting to activate a second returns `409 Conflict`.

**UI guidance by status**

| Status | Seller sees | Admin sees |
|---|---|---|
| `DRAFT` | Edit · Activate · Schedule · Delete buttons | Update · Override buttons |
| `ACTIVE` | Disable button only (cannot delete live prices) | Update · Override · Disable buttons |
| `SCHEDULED` | Cancel (disable) button | Update · Disable buttons |
| `DISABLED` | Activate button | Update · Override · Activate buttons |
| `EXPIRED` | No actions — read-only | Read-only |

**Checkout rule**: buyers only see prices with `status = ACTIVE` that are within their `validFrom`/`validUntil` window. A `SCHEDULED` price is not used for checkout until it becomes `ACTIVE` at query time.

---

## 15. Error Reference

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure — missing required field, `saleAmount > amount`, `validUntil` not after `validFrom` |
| `401 Unauthorized` | Missing or expired access token — refresh and retry |
| `403 Forbidden` | Wrong role, missing permission, or seller does not own this price |
| `404 Not Found` | Price or tier UUID does not exist, or no active price found for the product/currency |
| `409 Conflict` | Another `ACTIVE` price already exists for the same product/variant/currency slot |

For `400` errors, `data` may contain a field-error map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "saleAmount": "saleAmount must not exceed amount"
  }
}
```

**On `401`**: call `POST /v1/auth/refresh-token`. On failure, redirect to login.  
**On `403`**: the authenticated user lacks the necessary permission or does not own the resource. This is a client-side routing bug, not a recoverable error for the end user.  
**On `409`** during activate: query `sellerPrices` or `adminPrices` to find the currently active price for that product/currency, disable it, then activate the new one.  
**On `404`** from `effectivePrice`: the product has no configured price in that currency — show "Price unavailable" rather than blocking the entire page.

---

## 16. Quick Reference

```
Seller mutations — REST  (requires price.* permission)
────────────────────────────────────────────────────────────────────────
POST   /v1/seller/prices                      🔒  Create price (→ DRAFT)
PATCH  /v1/seller/prices/{id}                 🔒  Update price (patch semantics)
POST   /v1/seller/prices/{id}/activate        🔒  Activate (→ ACTIVE)
POST   /v1/seller/prices/{id}/disable         🔒  Disable (→ DISABLED)
POST   /v1/seller/prices/{id}/schedule        🔒  Schedule (→ SCHEDULED)

POST   /v1/seller/prices/tiers                🔒  Add volume pricing tier
PATCH  /v1/seller/prices/tiers/{tierId}       🔒  Update tier
DELETE /v1/seller/prices/tiers/{tierId}       🔒  Soft-delete tier

Admin mutations — REST  (requires ADMIN role + price.* permission)
────────────────────────────────────────────────────────────────────────
PATCH  /v1/admin/prices/{id}                  🔒  Admin update (no ownership check)
POST   /v1/admin/prices/{id}/override         🔒  Audited price override

Cart — REST  (requires CUSTOMER role)
────────────────────────────────────────────────────────────────────────
POST   /v1/cart/validate                      🔒  Validate cart prices before checkout

Reads — GraphQL  (public unless noted)
────────────────────────────────────────────────────────────────────────
query price(productId, currency)                    Active price for a product
query effectivePrice(productId, variantId, quantity, currency)
                                                    Resolved checkout price (use this for display)
query prices(productId, currency)                   All price records for a product
query sellerPrices(productId, page)          🔒  Seller's prices for a product
query adminPrices(page)                      🔒  All prices — admin
query adminPrice(id)                         🔒  Single price — admin
query priceHistory(priceId)                  🔒  Immutable change log

🔒 = requires Authorization: Bearer <accessToken>
```

---

### Critical rules

1. **Never trust client-supplied prices at checkout.** Always call `POST /cart/validate` before proceeding to payment. The server re-resolves each price from the authoritative DB, bypassing Redis cache.
2. **Store `effectivePrice` when adding to cart** — that is the value to send as `expectedPrice` in the validate call.
3. **One active price per slot.** Disable the existing active price before activating a replacement. The `409` response means a live price is already serving that slot.
4. **Use `effectivePrice` for display**, not `amount`. `effectivePrice` already applies sale discounts and tier pricing for the given quantity.
5. **Scheduled prices require no frontend polling** — they activate server-side at query time when `validFrom` is reached.
