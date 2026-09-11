# Store Management API — Frontend Integration Guide

Base paths: `/v1/sellers/me/stores` (seller mutations) · `/v1/admin/stores` (admin mutations) · `POST /graphql` (all reads)  
All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**  
> Use REST only to create or mutate store state (create, update, status transitions, settings, policies).  
> Use GraphQL for all reads (store profile, listings, status history, settings, policies).  
> See [auth.md](auth.md) for token acquisition and refresh. See [seller.md](seller.md) for seller onboarding.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [How Stores Are Created](#3-how-stores-are-created)
4. [Seller — Create a Store (REST)](#4-seller--create-a-store-rest)
5. [Seller — Update Store Details (REST)](#5-seller--update-store-details-rest)
6. [Seller — Submit for Review (REST)](#6-seller--submit-for-review-rest)
7. [Seller — Change Visibility (REST)](#7-seller--change-visibility-rest)
8. [Seller — Pause a Store (REST)](#8-seller--pause-a-store-rest)
9. [Seller — Close a Store (REST)](#9-seller--close-a-store-rest)
10. [Seller — Store Settings (REST)](#10-seller--store-settings-rest)
11. [Seller — Store Policies (REST)](#11-seller--store-policies-rest)
12. [Admin — Update Store Status (REST)](#12-admin--update-store-status-rest)
13. [Admin — View Status History (REST)](#13-admin--view-status-history-rest)
14. [Read Stores (GraphQL)](#14-read-stores-graphql)
15. [Status Transition Rules](#15-status-transition-rules)
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
    "myStore": { ... }
  }
}
```

On error:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Store not found",
      "extensions": { "status": 404 }
    }
  ]
}
```

Check `errors[0].message` and `errors[0].extensions.status` for GraphQL error handling.

---

## 2. Reference Types

### `StoreStatus`

| Value | Meaning |
|---|---|
| `DRAFT` | Created but not yet submitted for review |
| `PENDING_REVIEW` | Submitted by seller; awaiting admin approval |
| `ACTIVE` | Approved and open — visible to buyers |
| `PAUSED` | Temporarily closed by the seller; self-recoverable |
| `SUSPENDED` | Taken offline by admin; seller cannot self-restore |
| `CLOSED` | Permanently closed — no further trading |
| `ARCHIVED` | Terminal state; data preserved but no actions possible |

### `StoreVisibility`

| Value | Meaning |
|---|---|
| `PUBLIC` | Listed in search and browse |
| `PRIVATE` | Only visible to the seller in their own dashboard |
| `UNLISTED` | Accessible via direct link but excluded from listings |

### `StorePolicyType`

| Value | Meaning |
|---|---|
| `RETURN` | Return eligibility rules |
| `REFUND` | Refund terms and timelines |
| `SHIPPING` | Shipping methods and costs |
| `CANCELLATION` | Order cancellation window and conditions |
| `WARRANTY` | Warranty or guarantee terms |

### `StorePolicyStatus`

| Value | Meaning |
|---|---|
| `DRAFT` | Being written; not surfaced to buyers |
| `ACTIVE` | Published and shown on the store page |
| `ARCHIVED` | Superseded by a newer version |

### TypeScript interfaces

```typescript
interface Store {
  id:             string;        // UUID — use this for all subsequent API calls
  storeName:      string;
  slug:           string;        // URL-friendly; e.g. "janes-boutique"
  description:    string | null;
  logoMediaId:    string | null; // media module reference, not a URL
  bannerMediaId:  string | null;
  status:         StoreStatus;
  visibility:     StoreVisibility;
  businessEmail:  string | null;
  businessPhone:  string | null;
  website:        string | null;
  createdAt:      string;        // ISO-8601 UTC
  updatedAt:      string;
}

interface StoreDetail extends Store {
  settings: StoreSettings;
  policies: StorePolicy[];
}

interface StoreSummary {
  id:                string;     // UUID
  storeName:         string;
  slug:              string;
  logoMediaId:       string | null;
  status:            StoreStatus;
  visibility:        StoreVisibility;
  sellerDisplayName: string | null;
  productCount:      number;
  avgRating:         number;
  reviewCount:       number;
  orderCount:        number;
  createdAt:         string;
}

interface StoreSettings {
  id:                    string;  // UUID
  currency:              string;  // ISO-4217 code, e.g. "GHS"
  timezone:              string;  // IANA tz, e.g. "Africa/Accra"
  language:              string;  // BCP-47 code, e.g. "en"
  orderNotifications:    boolean;
  customerNotifications: boolean;
  updatedAt:             string;
}

interface StorePolicy {
  id:            string;           // UUID
  type:          StorePolicyType;
  title:         string;
  content:       string;
  version:       number;           // increments on every update
  status:        StorePolicyStatus;
  effectiveFrom: string | null;    // ISO-8601 UTC
  createdAt:     string;
  updatedAt:     string;
}

interface StoreStatusHistory {
  id:             string;
  previousStatus: StoreStatus | null;
  newStatus:      StoreStatus;
  reason:         string | null;
  expiresAt:      string | null;   // set when a suspension has a fixed end date
  createdAt:      string;
}

interface StorePage {
  content:       StoreSummary[];
  totalElements: number;
  totalPages:    number;
  currentPage:   number;
  hasNextPage:   boolean;
}
```

---

## 3. How Stores Are Created

Each seller account can have **at most one active store** at a time. The system enforces this server-side — a `409` is returned if the seller already has a non-archived store.

Stores are not provisioned automatically. The seller must call `POST /v1/sellers/me/stores` explicitly after their seller account is approved (`SellerStatus=ACTIVE`). See [seller.md](seller.md) for the seller onboarding flow that precedes store creation.

Stores start in `DRAFT` status with `PRIVATE` visibility. They only appear in public listings once `status=ACTIVE` **and** `visibility=PUBLIC`. The `slug` is generated from the store name if not supplied; uniqueness is guaranteed across the store's lifetime (past slugs are reserved in a slug-history table so they cannot be reused by another store).

---

## 4. Seller — Create a Store (REST)

```
POST /v1/sellers/me/stores
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "storeName":     "Jane's Boutique",
  "slug":          "janes-boutique",
  "description":   "Handmade accessories and fashion from Accra.",
  "businessEmail": "hello@janesboutique.com",
  "businessPhone": "+233201234567",
  "website":       "https://janesboutique.com"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `storeName` | Yes | 2–100 characters | |
| `slug` | No | max 120 chars · `^[a-z0-9]+(?:-[a-z0-9]+)*$` | Auto-generated from `storeName` if omitted; `409` if taken |
| `description` | No | max 1 000 characters | |
| `businessEmail` | No | valid email · max 255 chars | |
| `businessPhone` | No | max 30 characters | |
| `website` | No | max 255 characters | |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Store created",
  "data": {
    "id":            "aa11bb22-...",
    "storeName":     "Jane's Boutique",
    "slug":          "janes-boutique",
    "description":   "Handmade accessories and fashion from Accra.",
    "logoMediaId":   null,
    "bannerMediaId": null,
    "status":        "DRAFT",
    "visibility":    "PRIVATE",
    "businessEmail": "hello@janesboutique.com",
    "businessPhone": "+233201234567",
    "website":       "https://janesboutique.com",
    "createdAt":     "2026-09-10T10:00:00Z",
    "updatedAt":     "2026-09-10T10:00:00Z",
    "settings": {
      "id":                    "cc33dd44-...",
      "currency":              "GHS",
      "timezone":              "Africa/Accra",
      "language":              "en",
      "orderNotifications":    true,
      "customerNotifications": true,
      "updatedAt":             "2026-09-10T10:00:00Z"
    },
    "policies": []
  }
}
```

Store the returned `id` — it is the UUID you pass to product and variant endpoints as `{storeId}`. Settings are auto-created with defaults (GHS / Africa/Accra / en).

---

## 5. Seller — Update Store Details (REST)

All fields are optional — send only what changed. Changing `storeName` does **not** automatically update the slug.

```
PATCH /v1/sellers/me/stores/me
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "storeName":     "Jane's Boutique & Co.",
  "description":   "Revised store description.",
  "logoMediaId":   "media-uuid-or-key",
  "bannerMediaId": "media-uuid-or-key",
  "businessEmail": "new@janesboutique.com",
  "businessPhone": "+233209876543",
  "website":       "https://janesboutique.com"
}
```

| Field | Constraints | Notes |
|---|---|---|
| `storeName` | 2–100 characters | |
| `description` | max 1 000 characters | |
| `logoMediaId` | — | Reference to the media module; not a URL |
| `bannerMediaId` | — | Reference to the media module; not a URL |
| `businessEmail` | valid email · max 255 chars | |
| `businessPhone` | max 30 characters | |
| `website` | max 255 characters | |

**Response `200`**

```json
{
  "success": true,
  "message": "Store updated",
  "data": { ...Store }
}
```

After a successful update, re-query `myStore` via GraphQL to refresh your local state.

---

## 6. Seller — Submit for Review (REST)

Transitions: `DRAFT → PENDING_REVIEW`. No request body.

```
POST /v1/sellers/me/stores/me/submit
Authorization: Bearer <accessToken>   ← SELLER role required
```

**Response `200`**

```json
{
  "success": true,
  "message": "Store submitted for review",
  "data": { ...Store (status: "PENDING_REVIEW") }
}
```

**`422 Unprocessable Entity`** if the store is not in `DRAFT` status.

**UI guidance**: after a successful submit, show "Your store is under review." Disable the submit button — the store cannot be resubmitted until an admin acts on it or returns it to `DRAFT`.

---

## 7. Seller — Change Visibility (REST)

Controls whether the store appears in public browse/search. Visibility changes are independent of status — a seller can make an `ACTIVE` store `UNLISTED` without deactivating it.

```
PATCH /v1/sellers/me/stores/me/visibility
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "visibility": "PUBLIC"
}
```

| Field | Required | Values |
|---|---|---|
| `visibility` | Yes | `PUBLIC` \| `PRIVATE` \| `UNLISTED` |

**Response `200`**

```json
{
  "success": true,
  "message": "Store visibility updated",
  "data": { ...Store (visibility: "PUBLIC") }
}
```

**Public visibility rule**: a store appears in buyer-facing browse only when `status=ACTIVE` **and** `visibility=PUBLIC`. Changing visibility alone does not change the status.

---

## 8. Seller — Pause a Store (REST)

Transitions: `ACTIVE → PAUSED`. The store is taken offline temporarily. The seller can resume it at any time by calling the admin-equivalent restore path, or by contacting support. This is a self-service operation for temporary closures (holidays, stock issues, etc.).

```
POST /v1/sellers/me/stores/me/pause
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body** (optional)

```json
{
  "reason": "Closed for restocking — back in 2 weeks."
}
```

| Field | Required | Notes |
|---|---|---|
| `reason` | No | Stored in status history; not shown to buyers |

**Response `200`**

```json
{
  "success": true,
  "message": "Store paused",
  "data": { ...Store (status: "PAUSED") }
}
```

**`422`** if the store is not `ACTIVE`.

To resume a paused store, use the admin `PATCH /v1/admin/stores/{storeId}/status` endpoint with `status: "ACTIVE"`, or call seller-facing resume if exposed in a future release.

---

## 9. Seller — Close a Store (REST)

Transitions: `ACTIVE | PAUSED → CLOSED`. Permanently closes the store. This action cannot be undone by the seller — only an admin can archive or otherwise act on a closed store.

```
POST /v1/sellers/me/stores/me/close
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body** (optional)

```json
{
  "reason": "Closing the business."
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Store closed",
  "data": { ...Store (status: "CLOSED") }
}
```

**UI guidance**: gate this action behind a confirmation dialog — it is irreversible from the seller's perspective. Show a clear warning that existing orders will need to be fulfilled before closure.

---

## 10. Seller — Store Settings (REST)

Store settings control currency, timezone, language, and notification preferences. Settings are created automatically when the store is created (defaults: GHS / Africa/Accra / en / notifications on).

### Update settings

All fields are optional — send only what changed.

```
PATCH /v1/sellers/me/stores/me/settings
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "currency":              "GHS",
  "timezone":              "Africa/Accra",
  "language":              "en",
  "orderNotifications":    true,
  "customerNotifications": false
}
```

| Field | Constraints | Notes |
|---|---|---|
| `currency` | `^[A-Z]{3}$` | ISO-4217 currency code |
| `timezone` | max 50 characters | IANA timezone identifier |
| `language` | max 10 characters | BCP-47 language code |
| `orderNotifications` | boolean | Email alert on new orders |
| `customerNotifications` | boolean | Email alert on customer messages |

**Response `200`**

```json
{
  "success": true,
  "message": "Store settings updated",
  "data": {
    "id":                    "cc33dd44-...",
    "currency":              "GHS",
    "timezone":              "Africa/Accra",
    "language":              "en",
    "orderNotifications":    true,
    "customerNotifications": false,
    "updatedAt":             "2026-09-10T11:00:00Z"
  }
}
```

To read current settings, use the `myStore { settings { ... } }` GraphQL query.

---

## 11. Seller — Store Policies (REST)

Policies are documents the seller publishes on their store page (return rules, refund terms, etc.). Each `StorePolicyType` can have one policy at a time. Policies are versioned — every update increments the `version` counter. Deletion is soft (the record is hidden, not removed).

### Create a policy

```
POST /v1/sellers/me/stores/me/policies
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Request body**

```json
{
  "type":          "RETURN",
  "title":         "30-Day Return Policy",
  "content":       "Items may be returned within 30 days of delivery in original condition...",
  "status":        "ACTIVE",
  "effectiveFrom": "2026-09-01T00:00:00Z"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `type` | Yes | `StorePolicyType` | `RETURN` \| `REFUND` \| `SHIPPING` \| `CANCELLATION` \| `WARRANTY` |
| `title` | Yes | max 255 characters | |
| `content` | Yes | — | Rich text or plain text; rendered on the store page |
| `status` | No | `StorePolicyStatus` | Defaults to `DRAFT` if omitted |
| `effectiveFrom` | No | ISO-8601 UTC | When the policy takes effect; `null` means immediately |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Policy created",
  "data": {
    "id":            "ee55ff66-...",
    "type":          "RETURN",
    "title":         "30-Day Return Policy",
    "content":       "Items may be returned within 30 days...",
    "version":       1,
    "status":        "ACTIVE",
    "effectiveFrom": "2026-09-01T00:00:00Z",
    "createdAt":     "2026-09-10T11:05:00Z",
    "updatedAt":     "2026-09-10T11:05:00Z"
  }
}
```

---

### Update a policy

All fields are optional. Every successful update increments `version` by 1 automatically.

```
PATCH /v1/sellers/me/stores/me/policies/{policyId}
Authorization: Bearer <accessToken>   ← SELLER role required
Content-Type: application/json
```

**Path parameter**: `policyId` — the policy's UUID.

**Request body**

```json
{
  "title":   "Updated Return Policy",
  "content": "Items may be returned within 14 days...",
  "status":  "ACTIVE"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Policy updated",
  "data": { ...StorePolicy (version: 2) }
}
```

---

### Delete a policy

Soft-deletes the policy — removes it from the store page but preserves the record.

```
DELETE /v1/sellers/me/stores/me/policies/{policyId}
Authorization: Bearer <accessToken>   ← SELLER role required
```

**Response `200`**

```json
{
  "success": true,
  "message": "Policy deleted",
  "data": null
}
```

To read all current policies, use the `myStore { policies { ... } }` GraphQL query.

---

## 12. Admin — Update Store Status (REST)

Admins use a single endpoint to approve, suspend, reactivate, or close a store. The target `status` field drives the transition; an invalid transition returns `422`.

```
PATCH /v1/admin/stores/{storeId}/status
Authorization: Bearer <accessToken>   ← ADMIN role required
Content-Type: application/json
```

**Path parameter**: `storeId` — the store's public UUID.

**Request body**

```json
{
  "status":    "SUSPENDED",
  "reason":    "Multiple verified buyer complaints about counterfeit goods.",
  "expiresAt": "2026-10-01T00:00:00Z"
}
```

| Field | Required | Notes |
|---|---|---|
| `status` | Yes | Target `StoreStatus` |
| `reason` | No | Stored in status history and audit log; recommended for suspend/close |
| `expiresAt` | No | ISO-8601 UTC; set to schedule automatic reactivation for timed suspensions |

**Common transitions and their `status` values**

| Intent | `status` value |
|---|---|
| Approve submitted store | `"ACTIVE"` |
| Reactivate paused/suspended store | `"ACTIVE"` |
| Suspend store | `"SUSPENDED"` |
| Close store | `"CLOSED"` |
| Archive closed store | `"ARCHIVED"` |

**Response `200`**

```json
{
  "success": true,
  "message": "Store status updated",
  "data": { ...Store (status: "SUSPENDED") }
}
```

**`422 Unprocessable Entity`** if the transition is not permitted (see [§15](#15-status-transition-rules)).

---

## 13. Admin — View Status History (REST)

Returns the full audit trail of status changes for a store, newest first.

```
GET /v1/admin/stores/{storeId}/status-history
Authorization: Bearer <accessToken>   ← ADMIN role required
```

**Response `200`**

```json
{
  "success": true,
  "message": "Status history retrieved",
  "data": [
    {
      "id":             "gg77hh88-...",
      "previousStatus": "PENDING_REVIEW",
      "newStatus":      "ACTIVE",
      "reason":         null,
      "expiresAt":      null,
      "createdAt":      "2026-09-10T12:00:00Z"
    },
    {
      "id":             "ii99jj00-...",
      "previousStatus": "DRAFT",
      "newStatus":      "PENDING_REVIEW",
      "reason":         null,
      "expiresAt":      null,
      "createdAt":      "2026-09-10T10:00:00Z"
    }
  ]
}
```

The same data is available via the `storeStatusHistory(storeId)` GraphQL query (preferred for admin dashboard reads).

---

## 14. Read Stores (GraphQL)

All store reads go through `POST /graphql`. Public queries require no auth token. Seller and admin queries require a bearer token with the appropriate role.

---

### Get my store (seller)

Returns the authenticated seller's store with settings and policies. Requires a `SELLER` bearer token.

```graphql
query MyStore {
  myStore {
    id
    storeName
    slug
    description
    logoMediaId
    bannerMediaId
    status
    visibility
    businessEmail
    businessPhone
    website
    createdAt
    updatedAt
    settings {
      id
      currency
      timezone
      language
      orderNotifications
      customerNotifications
      updatedAt
    }
    policies {
      id
      type
      title
      content
      version
      status
      effectiveFrom
      createdAt
      updatedAt
    }
  }
}
```

**Response**

```json
{
  "data": {
    "myStore": {
      "id":          "aa11bb22-...",
      "storeName":   "Jane's Boutique",
      "slug":        "janes-boutique",
      "status":      "ACTIVE",
      "visibility":  "PUBLIC",
      "settings": {
        "currency":  "GHS",
        "timezone":  "Africa/Accra",
        "language":  "en",
        "orderNotifications":    true,
        "customerNotifications": true,
        "updatedAt": "2026-09-10T11:00:00Z"
      },
      "policies": [
        {
          "type":    "RETURN",
          "title":   "30-Day Return Policy",
          "version": 1,
          "status":  "ACTIVE"
        }
      ]
    }
  }
}
```

`403` if the token does not carry the `SELLER` role.

---

### Browse public stores

Returns stores matching the given filters. No auth required. Only `ACTIVE` stores with `visibility=PUBLIC` are returned by default.

```graphql
query BrowseStores($query: String, $page: Int, $size: Int) {
  stores(query: $query, page: $page, size: $size) {
    content {
      id
      storeName
      slug
      logoMediaId
      status
      visibility
      sellerDisplayName
      productCount
      avgRating
      reviewCount
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
{ "query": "boutique", "page": 0, "size": 20 }
```

`query` is a free-text filter on store name. Omit to list all visible stores. Default page size is 20.

---

### Get a store by slug

Used for store detail/profile pages where the URL contains the slug.

```graphql
query StoreBySlug($slug: String!) {
  storeBySlug(slug: $slug) {
    id
    storeName
    slug
    description
    logoMediaId
    bannerMediaId
    status
    visibility
    sellerDisplayName
    productCount
    avgRating
    reviewCount
    orderCount
    createdAt
  }
}
```

**Variables**

```json
{ "slug": "janes-boutique" }
```

Returns `null` (or a GraphQL error) if not found.

---

### Admin — search stores

Returns all stores regardless of status. Requires an `ADMIN` bearer token.

```graphql
query AdminStores(
  $query: String,
  $status: StoreStatus,
  $visibility: StoreVisibility,
  $page: Int,
  $size: Int
) {
  stores(
    query: $query,
    status: $status,
    visibility: $visibility,
    page: $page,
    size: $size
  ) {
    content {
      id
      storeName
      slug
      status
      visibility
      sellerDisplayName
      productCount
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
{
  "status":     "PENDING_REVIEW",
  "page":       0,
  "size":       20
}
```

All filter fields are optional. Omit `status` and `visibility` to return stores in any state.

---

### Admin — store status history

Returns the full transition audit trail for a given store. Requires an `ADMIN` bearer token.

```graphql
query StoreStatusHistory($storeId: ID!) {
  storeStatusHistory(storeId: $storeId) {
    id
    previousStatus
    newStatus
    reason
    expiresAt
    createdAt
  }
}
```

**Variables**

```json
{ "storeId": "aa11bb22-..." }
```

---

## 15. Status Transition Rules

Only the following transitions are permitted. Any other attempt returns `422 Unprocessable Entity`.

```
DRAFT          ──→  PENDING_REVIEW         (seller submits)
DRAFT          ──→  ARCHIVED               (admin only)

PENDING_REVIEW ──→  ACTIVE                 (admin approves)
PENDING_REVIEW ──→  DRAFT                  (admin sends back)

ACTIVE         ──→  PAUSED                 (seller pauses)
ACTIVE         ──→  SUSPENDED              (admin suspends)
ACTIVE         ──→  CLOSED                 (seller or admin closes)

PAUSED         ──→  ACTIVE                 (admin reactivates)
PAUSED         ──→  CLOSED                 (seller or admin closes)

SUSPENDED      ──→  ACTIVE                 (admin restores)
SUSPENDED      ──→  CLOSED                 (admin closes)

CLOSED         ──→  ARCHIVED               (admin archives)

ARCHIVED       ──→  (none — terminal state)
```

**UI guidance by status**

| Status | Seller sees | Admin sees |
|---|---|---|
| `DRAFT` | Edit form + "Submit for review" button | No action required |
| `PENDING_REVIEW` | "Under review" — no seller actions | Approve (→ ACTIVE) · Send back (→ DRAFT) |
| `ACTIVE` | Pause + Close buttons · Visibility toggle | Suspend · Close |
| `PAUSED` | "Store paused" message + resume instructions | Activate · Close |
| `SUSPENDED` | "Suspended by admin" + reason if available | Activate · Close |
| `CLOSED` | "Store closed" — no actions | Archive |
| `ARCHIVED` | Hidden from seller dashboard | No actions |

**Timed suspensions**: when `expiresAt` is set on a `SUSPENDED` store, a background scheduler automatically transitions it back to `ACTIVE` at expiry. Show the expiry date to the seller so they know when their store will reopen.

---

## 16. Error Reference

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure — field too long, invalid pattern, missing required field |
| `401 Unauthorized` | Missing or expired access token — refresh and retry |
| `403 Forbidden` | Wrong role or the seller does not own the resource |
| `404 Not Found` | Store or policy UUID does not exist |
| `409 Conflict` | Seller already has an active store; or requested slug is already taken |
| `422 Unprocessable Entity` | Invalid status transition |

For `400` errors, `data` may contain a field-error map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "storeName": "size must be between 2 and 100",
    "slug": "must match \"^[a-z0-9]+(?:-[a-z0-9]+)*$\""
  }
}
```

**On `401`**: call `POST /v1/auth/refresh-token`. On failure, redirect to login.  
**On `409` (slug taken)**: prompt the seller to choose a different slug.  
**On `422`**: inspect the store's current `status` and use the transition table in [§15](#15-status-transition-rules) to determine which actions to show or hide.

---

## 17. Quick Reference

```
Seller mutations — REST  (requires SELLER role)
──────────────────────────────────────────────────────────────────────────
POST   /v1/sellers/me/stores                          🔒  Create store (→ DRAFT / PRIVATE)
PATCH  /v1/sellers/me/stores/me                       🔒  Update name, description, logo, contact
POST   /v1/sellers/me/stores/me/submit                🔒  Submit for review (→ PENDING_REVIEW)
PATCH  /v1/sellers/me/stores/me/visibility            🔒  Change visibility
POST   /v1/sellers/me/stores/me/pause                 🔒  Pause store (→ PAUSED)
POST   /v1/sellers/me/stores/me/close                 🔒  Close store permanently (→ CLOSED)
PATCH  /v1/sellers/me/stores/me/settings              🔒  Update currency, timezone, notifications
POST   /v1/sellers/me/stores/me/policies              🔒  Create a store policy
PATCH  /v1/sellers/me/stores/me/policies/{policyId}   🔒  Update a policy (version++)
DELETE /v1/sellers/me/stores/me/policies/{policyId}   🔒  Soft-delete a policy

Admin mutations — REST  (requires ADMIN role)
──────────────────────────────────────────────────────────────────────────
PATCH  /v1/admin/stores/{storeId}/status              🔒  Approve / suspend / reactivate / close / archive
GET    /v1/admin/stores/{storeId}/status-history      🔒  Status audit trail  ← prefer GraphQL

Reads — GraphQL  (public unless noted)
──────────────────────────────────────────────────────────────────────────
query myStore                                         🔒  Seller's own store with settings + policies
query stores(query, status, visibility, page, size)       Public listing (or admin with filters)
query storeBySlug(slug)                                   Store profile page by slug
query storeStatusHistory(storeId)                     🔒  Admin: full status audit trail

🔒 = requires Authorization: Bearer <accessToken>
```
