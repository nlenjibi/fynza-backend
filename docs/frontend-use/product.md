# Product Management API — Frontend Integration Guide

Base paths: `/v1/sellers/me/stores/{storeId}/products` (seller mutations) · `/v1/admin/products` (admin mutations) · `POST /graphql` (all reads)  
All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**  
> Use REST only to create, update, or transition product state.  
> Use GraphQL for all reads (product details, listings, variants, seller dashboard).  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [Product Lifecycle Overview](#3-product-lifecycle-overview)
4. [Seller — Create a Product (REST)](#4-seller--create-a-product-rest)
5. [Seller — Update a Product (REST)](#5-seller--update-a-product-rest)
6. [Seller — Submit for Review (REST)](#6-seller--submit-for-review-rest)
7. [Seller — Publish a Product (REST)](#7-seller--publish-a-product-rest)
8. [Seller — Deactivate a Product (REST)](#8-seller--deactivate-a-product-rest)
9. [Seller — Archive a Product (REST)](#9-seller--archive-a-product-rest)
10. [Seller — Delete a Product (REST)](#10-seller--delete-a-product-rest)
11. [Seller — Variant Management (REST)](#11-seller--variant-management-rest)
12. [Admin — Approve / Reject / Suspend / Restore (REST)](#12-admin--approve--reject--suspend--restore-rest)
13. [Read Products (GraphQL)](#13-read-products-graphql)
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
    "products": { ... }
  }
}
```

On error:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Product not found",
      "extensions": { "status": 404 }
    }
  ]
}
```

Check `errors[0].message` and `errors[0].extensions.status` for GraphQL error handling.

---

## 2. Reference Types

### `ProductStatus`

| Value | Meaning |
|---|---|
| `DRAFT` | Created by seller; not yet submitted for review |
| `PENDING_REVIEW` | Submitted by seller; awaiting admin decision |
| `ACTIVE` | Approved and live — visible to buyers |
| `INACTIVE` | Deactivated by seller or rejected by admin |
| `SUSPENDED` | Removed from sale by admin; seller cannot re-publish independently |
| `ARCHIVED` | Retired by seller; no longer sold but preserved for history |
| `DELETED` | Soft-deleted; hidden from all surfaces |

### `ProductType`

| Value | Meaning |
|---|---|
| `SIMPLE` | Single SKU product with no variants (default) |
| `VARIANT` | Product with multiple variants (size, colour, etc.) |

### `ProductVisibility`

| Value | Meaning |
|---|---|
| `PUBLIC` | Visible in search and browse — default for approved products |
| `PRIVATE` | Only visible to the seller in their dashboard |
| `UNLISTED` | Accessible via direct link but not in listings |

### TypeScript interfaces

```typescript
interface Product {
  id:            string;          // UUID — use this for all subsequent API calls
  productNumber: string;          // e.g. "PRD-000001"
  name:          string;
  slug:          string;          // URL-friendly; e.g. "blue-linen-shirt"
  brand:         string | null;
  sku:           string | null;   // product-level SKU; distinct from variant SKUs
  description:   string | null;
  productType:   ProductType;
  status:        ProductStatus;
  visibility:    ProductVisibility;
  createdAt:     string;          // ISO-8601 UTC
  updatedAt:     string;
}

interface ProductVariant {
  id:            string;          // UUID
  productId:     string;          // UUID of parent product
  sku:           string;          // required; unique per product
  barcode:       string | null;
  variantName:   string | null;
  variantStatus: string;          // "ACTIVE" | "INACTIVE"
  size:          string | null;
  color:         string | null;
  isActive:      boolean;
  createdAt:     string;
  updatedAt:     string;
}

interface ProductPage {
  content:       Product[];
  totalElements: number;
  totalPages:    number;
  currentPage:   number;
  pageSize:      number;
  hasNextPage:   boolean;
}
```

---

## 3. Product Lifecycle Overview

```
Seller creates product  →  DRAFT / PRIVATE
         │
         ▼
POST …/submit           →  PENDING_REVIEW   (awaiting admin)
         │
    ┌────┴────┐
    ▼         ▼
Admin       Admin
approve     reject
    │         │
    ▼         ▼
 ACTIVE    INACTIVE   (seller can fix and resubmit)
    │
    ├──▶  Seller deactivates  →  INACTIVE
    ├──▶  Seller archives     →  ARCHIVED
    ├──▶  Admin suspends      →  SUSPENDED
    └──▶  Seller deletes      →  DELETED (soft)
```

Products start `DRAFT` and `PRIVATE`. They only appear in public search once `status=ACTIVE` **and** `visibility=PUBLIC`. Never rely on a product's `storeId` / `sellerId` fields from the response for subsequent API calls — those are internal surrogate keys; use the UUID `id` field and the seller's store UUID instead.

---

## 4. Seller — Create a Product (REST)

Creates a product in `DRAFT` status with `PRIVATE` visibility. The seller must be the owner of `{storeId}` — the service enforces this regardless of what the token contains.

```
POST /v1/sellers/me/stores/{storeId}/products
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Path parameters**

| Parameter | Type | Notes |
|---|---|---|
| `storeId` | `UUID` | The seller's store public ID |

**Request body**

```json
{
  "name":            "Blue Linen Shirt",
  "description":     "Breathable linen shirt for warm climates.",
  "brand":           "Kente & Co",
  "sku":             "KNC-SHIRT-BLU",
  "productType":     "SIMPLE",
  "primaryCategoryId": "7f3d2a1b-..."
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `name` | Yes | max 255 characters | |
| `description` | No | max 5 000 characters | |
| `brand` | No | — | |
| `sku` | No | — | Product-level SKU; distinct from variant SKUs |
| `productType` | No | `SIMPLE` \| `VARIANT` | Defaults to `SIMPLE` if omitted |
| `primaryCategoryId` | No | UUID | Links the product to a category on creation |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Product created",
  "data": {
    "id":            "9a3c1d2e-...",
    "productNumber": "PRD-000042",
    "name":          "Blue Linen Shirt",
    "slug":          "blue-linen-shirt",
    "brand":         "Kente & Co",
    "sku":           "KNC-SHIRT-BLU",
    "description":   "Breathable linen shirt for warm climates.",
    "productType":   "SIMPLE",
    "status":        "DRAFT",
    "visibility":    "PRIVATE",
    "createdAt":     "2026-09-10T10:00:00Z",
    "updatedAt":     "2026-09-10T10:00:00Z"
  }
}
```

Store the returned `id` — it is the UUID you pass to all subsequent product endpoints.

---

## 5. Seller — Update a Product (REST)

All fields are optional — send only what changed.

```
PATCH /v1/sellers/me/stores/{storeId}/products/{productId}
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "name":        "Classic Blue Linen Shirt",
  "description": "Updated description.",
  "brand":       "Kente & Co",
  "sku":         "KNC-SHIRT-BLU-V2"
}
```

| Field | Constraints | Notes |
|---|---|---|
| `name` | max 255 characters | Changing the name regenerates the slug |
| `description` | max 5 000 characters | |
| `brand` | — | |
| `sku` | — | |

**Response `200`**

```json
{
  "success": true,
  "message": "Product updated",
  "data": { ...Product }
}
```

After a successful update, re-query via GraphQL to refresh your local state.

---

## 6. Seller — Submit for Review (REST)

Transitions: `DRAFT → PENDING_REVIEW`. No request body.

```
POST /v1/sellers/me/stores/{storeId}/products/{productId}/submit
Authorization: Bearer <accessToken>   ← SELLER role required
```

**Response `200`**

```json
{
  "success": true,
  "message": "Product submitted for review",
  "data": { ...Product (status: "PENDING_REVIEW") }
}
```

**`422 Unprocessable Entity`** if the product is not in `DRAFT` status.

**UI guidance**: disable the submit button after success and show "Awaiting admin review." The product cannot be resubmitted until the admin acts on it.

---

## 7. Seller — Publish a Product (REST)

Transitions an approved product to `ACTIVE`. This is available after admin approval — the product will have been moved through `PENDING_REVIEW → ACTIVE` by the admin, but a seller can also call this to re-publish from `INACTIVE`.

```
POST /v1/sellers/me/stores/{storeId}/products/{productId}/publish
Authorization: Bearer <accessToken>   ← SELLER role required
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Product published",
  "data": { ...Product (status: "ACTIVE") }
}
```

**`422`** if the transition is not permitted (see [§14](#14-status-transition-rules)).

---

## 8. Seller — Deactivate a Product (REST)

Transitions: `ACTIVE → INACTIVE`. The product is removed from public search but preserved.

```
POST /v1/sellers/me/stores/{storeId}/products/{productId}/deactivate
Authorization: Bearer <accessToken>   ← SELLER role required
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Product deactivated",
  "data": { ...Product (status: "INACTIVE") }
}
```

The seller can call `publish` to bring it back to `ACTIVE`.

---

## 9. Seller — Archive a Product (REST)

Retires the product. Archived products are no longer sold but their data is preserved.

```
POST /v1/sellers/me/stores/{storeId}/products/{productId}/archive
Authorization: Bearer <accessToken>   ← SELLER role required
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Product archived",
  "data": { ...Product (status: "ARCHIVED") }
}
```

---

## 10. Seller — Delete a Product (REST)

Soft-deletes the product. Sets `is_active = false` and `status = DELETED`. The record is not physically removed and will not appear on any API surface.

```
DELETE /v1/sellers/me/stores/{storeId}/products/{productId}
Authorization: Bearer <accessToken>   ← SELLER role required
```

**Response `200`**

```json
{
  "success": true,
  "message": "Product deleted",
  "data": null
}
```

---

## 11. Seller — Variant Management (REST)

Variants represent size/colour/option combinations of a `VARIANT`-type product. Each variant has its own SKU and can have independent inventory tracked by the inventory module.

### Create a variant

```
POST /v1/sellers/me/stores/{storeId}/products/{productId}/variants
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "sku":         "KNC-SHIRT-BLU-M",
  "barcode":     "6001234567890",
  "variantName": "Medium",
  "size":        "M",
  "color":       "Blue"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `sku` | Yes | max 255 characters | Must be unique per product; `409` on conflict |
| `barcode` | No | — | |
| `variantName` | No | max 255 characters | Display label for the variant |
| `size` | No | max 50 characters | |
| `color` | No | max 50 characters | |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Variant created",
  "data": {
    "id":            "c4d5e6f7-...",
    "productId":     "9a3c1d2e-...",
    "sku":           "KNC-SHIRT-BLU-M",
    "barcode":       "6001234567890",
    "variantName":   "Medium",
    "variantStatus": "ACTIVE",
    "size":          "M",
    "color":         "Blue",
    "isActive":      true,
    "createdAt":     "2026-09-10T10:05:00Z",
    "updatedAt":     "2026-09-10T10:05:00Z"
  }
}
```

---

### Update a variant

All fields are optional.

```
PATCH /v1/sellers/me/stores/{storeId}/products/{productId}/variants/{variantId}
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "sku":         "KNC-SHIRT-BLU-MD",
  "variantName": "Medium (Updated)",
  "size":        "M",
  "color":       "Sky Blue"
}
```

Changing `sku` triggers a uniqueness check. **`409`** if the new SKU conflicts with another variant on the same product.

**Response `200`**

```json
{
  "success": true,
  "message": "Variant updated",
  "data": { ...ProductVariant }
}
```

---

### Delete a variant

Soft-deletes the variant — sets `isActive = false`.

```
DELETE /v1/sellers/me/stores/{storeId}/products/{productId}/variants/{variantId}
Authorization: Bearer <accessToken>   ← SELLER role required
```

**Response `200`**

```json
{
  "success": true,
  "message": "Variant deleted",
  "data": null
}
```

---

## 12. Admin — Approve / Reject / Suspend / Restore (REST)

All admin mutations require an `ADMIN` bearer token. `{id}` is the product's public UUID.

### Approve a product

Transitions: `PENDING_REVIEW → ACTIVE`. Makes the product live.

```
POST /v1/admin/products/{id}/approve
Authorization: Bearer <accessToken>   ← ADMIN role required
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Product approved",
  "data": { ...Product (status: "ACTIVE") }
}
```

---

### Reject a product

Transitions: `PENDING_REVIEW → INACTIVE`. The seller is expected to address the issue and resubmit.

```
POST /v1/admin/products/{id}/reject
Authorization: Bearer <accessToken>   ← ADMIN role required
Content-Type: application/json
```

**Request body** (optional)

```json
{
  "reason": "Product images are missing or do not meet quality guidelines."
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Product rejected",
  "data": { ...Product (status: "INACTIVE") }
}
```

---

### Suspend a product

Removes the product from sale. The seller cannot re-publish a suspended product — only an admin can restore it.

```
POST /v1/admin/products/{id}/suspend
Authorization: Bearer <accessToken>   ← ADMIN role required
Content-Type: application/json
```

**Request body** (optional)

```json
{
  "reason": "Product violates platform prohibited items policy."
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Product suspended",
  "data": { ...Product (status: "SUSPENDED") }
}
```

---

### Restore a suspended product

Transitions: `SUSPENDED → ACTIVE`.

```
POST /v1/admin/products/{id}/restore
Authorization: Bearer <accessToken>   ← ADMIN role required
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Product restored",
  "data": { ...Product (status: "ACTIVE") }
}
```

---

## 13. Read Products (GraphQL)

All product reads go through `POST /graphql`. Public queries (browsing, product page) require no auth token. Seller and admin queries require a bearer token with the appropriate role.

---

### Browse public products

Returns only `status=ACTIVE` and `visibility=PUBLIC` products. No auth required.

```graphql
query BrowseProducts($page: Int, $size: Int) {
  products(page: $page, size: $size) {
    content {
      id
      productNumber
      name
      slug
      brand
      sku
      description
      productType
      status
      visibility
      createdAt
    }
    totalElements
    totalPages
    currentPage
    pageSize
    hasNextPage
  }
}
```

**Variables**

```json
{ "page": 0, "size": 20 }
```

Default: `page=0`, `size=20`, sorted `createdAt DESC`. Omit variables to use defaults.

---

### Get a product by ID

```graphql
query GetProduct($id: ID!) {
  product(id: $id) {
    id
    productNumber
    name
    slug
    brand
    sku
    description
    productType
    status
    visibility
    createdAt
    updatedAt
  }
}
```

**Variables**

```json
{ "id": "9a3c1d2e-..." }
```

Returns `null` if the product does not exist or is soft-deleted.

---

### Get a product by slug

Useful for product detail pages where the URL contains the slug.

```graphql
query GetProductBySlug($slug: String!) {
  productBySlug(slug: $slug) {
    id
    productNumber
    name
    slug
    brand
    description
    productType
    status
    visibility
    createdAt
    updatedAt
  }
}
```

**Variables**

```json
{ "slug": "blue-linen-shirt" }
```

Returns `null` if not found.

---

### Browse products by store

Lists active products for a given store. No auth required.

```graphql
query StoreProducts($storeId: ID!, $page: Int, $size: Int) {
  productsByStore(storeId: $storeId, page: $page, size: $size) {
    content {
      id
      name
      slug
      brand
      productType
      status
      visibility
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
{ "storeId": "aa11bb22-...", "page": 0, "size": 20 }
```

---

### Get variants for a product

Returns all active variants (`isActive=true`) for a product, ordered by SKU. No auth required.

```graphql
query ProductVariants($productId: ID!) {
  productVariants(productId: $productId) {
    id
    productId
    sku
    barcode
    variantName
    variantStatus
    size
    color
    isActive
    createdAt
    updatedAt
  }
}
```

**Variables**

```json
{ "productId": "9a3c1d2e-..." }
```

---

### Seller — my products dashboard

Returns all products the authenticated seller owns across a given store, regardless of status. Requires a `SELLER` bearer token.

```graphql
query MyProducts($storeId: ID!, $page: Int, $size: Int) {
  myProducts(storeId: $storeId, page: $page, size: $size) {
    content {
      id
      productNumber
      name
      slug
      productType
      status
      visibility
      createdAt
      updatedAt
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
{ "storeId": "aa11bb22-...", "page": 0, "size": 20 }
```

`403` if the token does not carry the `SELLER` role.

---

## 14. Status Transition Rules

Only the following transitions are permitted. Any other attempt returns `422 Unprocessable Entity`.

```
DRAFT          ──→  PENDING_REVIEW    (seller submits)
DRAFT          ──→  ARCHIVED | DELETED

PENDING_REVIEW ──→  ACTIVE            (admin approves)
PENDING_REVIEW ──→  INACTIVE          (admin rejects)
PENDING_REVIEW ──→  DRAFT | SUSPENDED

ACTIVE         ──→  INACTIVE          (seller deactivates)
ACTIVE         ──→  SUSPENDED         (admin suspends)
ACTIVE         ──→  ARCHIVED | PENDING_REVIEW

INACTIVE       ──→  ACTIVE            (seller publishes)
INACTIVE       ──→  SUSPENDED         (admin suspends)
INACTIVE       ──→  ARCHIVED | PENDING_REVIEW

SUSPENDED      ──→  ACTIVE            (admin restores)
SUSPENDED      ──→  INACTIVE | ARCHIVED

ARCHIVED       ──→  DRAFT | DELETED

DELETED        ──→  (none — terminal state)
```

**UI guidance by status**

| Status | Seller sees | Admin sees |
|---|---|---|
| `DRAFT` | Edit form + "Submit for review" button | No action needed |
| `PENDING_REVIEW` | "Under review" — no seller actions | Approve + Reject buttons |
| `ACTIVE` | Deactivate + Archive buttons | Suspend button |
| `INACTIVE` | Publish + Archive buttons | Suspend button |
| `SUSPENDED` | "Suspended by admin" message; reason if available | Restore button |
| `ARCHIVED` | No edit actions | No actions |
| `DELETED` | Hidden from all surfaces | Hidden from all surfaces |

**Public visibility rule**: a product appears in buyer-facing browse/search only when `status=ACTIVE` **and** `visibility=PUBLIC` **and** `isActive=true`. Any other combination means the product is not visible to buyers.

---

## 15. Error Reference

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure — field too long, missing required field |
| `401 Unauthorized` | Missing or expired access token — refresh and retry |
| `403 Forbidden` | Wrong role, or the seller does not own this product/store |
| `404 Not Found` | Product or variant UUID does not exist |
| `409 Conflict` | Variant SKU already exists for this product |
| `422 Unprocessable Entity` | Invalid status transition |

For `400` errors, `data` may contain a field-error map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "name": "size must be between 0 and 255"
  }
}
```

**On `401`**: call `POST /v1/auth/refresh-token`. On failure, redirect to login.  
**On `403`**: the seller is calling an endpoint for a store they do not own — this is a client bug, not a permissions edge case to recover from.  
**On `422`**: inspect the product's current `status` and use the transition table in [§14](#14-status-transition-rules) to determine which actions to show.

---

## 16. Quick Reference

```
Seller mutations — REST  (requires SELLER role + ownership of storeId)
─────────────────────────────────────────────────────────────────────────
POST   /v1/sellers/me/stores/{storeId}/products                         🔒  Create product (→ DRAFT)
PATCH  /v1/sellers/me/stores/{storeId}/products/{productId}             🔒  Update details
POST   /v1/sellers/me/stores/{storeId}/products/{productId}/submit      🔒  Submit for review (→ PENDING_REVIEW)
POST   /v1/sellers/me/stores/{storeId}/products/{productId}/publish     🔒  Publish (→ ACTIVE)
POST   /v1/sellers/me/stores/{storeId}/products/{productId}/deactivate  🔒  Deactivate (→ INACTIVE)
POST   /v1/sellers/me/stores/{storeId}/products/{productId}/archive     🔒  Archive (→ ARCHIVED)
DELETE /v1/sellers/me/stores/{storeId}/products/{productId}             🔒  Soft-delete (→ DELETED)

POST   /v1/sellers/me/stores/{storeId}/products/{productId}/variants                      🔒  Add variant
PATCH  /v1/sellers/me/stores/{storeId}/products/{productId}/variants/{variantId}          🔒  Update variant
DELETE /v1/sellers/me/stores/{storeId}/products/{productId}/variants/{variantId}          🔒  Delete variant

Admin mutations — REST  (requires ADMIN role)
─────────────────────────────────────────────────────────────────────────
POST   /v1/admin/products/{id}/approve   🔒  Approve → ACTIVE
POST   /v1/admin/products/{id}/reject    🔒  Reject  → INACTIVE
POST   /v1/admin/products/{id}/suspend   🔒  Suspend → SUSPENDED
POST   /v1/admin/products/{id}/restore   🔒  Restore → ACTIVE

Reads — GraphQL  (public unless noted)
─────────────────────────────────────────────────────────────────────────
query products(page, size)                    Public listing (ACTIVE + PUBLIC only)
query product(id)                             Single product by UUID
query productBySlug(slug)                     Single product by slug (for product pages)
query productsByStore(storeId, page, size)    Products in a store
query productVariants(productId)              Active variants for a product
query myProducts(storeId, page, size)         🔒  Seller's own products (all statuses)

🔒 = requires Authorization: Bearer <accessToken>
```
