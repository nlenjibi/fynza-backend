# Seller Management API — Frontend Integration Guide

Base paths: `/v1/sellers/me` (self-service mutations) · `/v1/admin/sellers` (admin mutations) · `POST /graphql` (all reads)  
All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**  
> Use REST only to mutate seller state (onboarding, application submission, lifecycle actions).  
> Use GraphQL for all reads (profile, verifications, status history, admin search).  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [How Seller Profiles Are Created](#3-how-seller-profiles-are-created)
4. [Self-Service — Read My Profile (GraphQL)](#4-self-service--read-my-profile-graphql)
5. [Self-Service — Update Display Name (REST)](#5-self-service--update-display-name-rest)
6. [Self-Service — Business Onboarding (REST)](#6-self-service--business-onboarding-rest)
7. [Self-Service — Submit Application (REST)](#7-self-service--submit-application-rest)
8. [Self-Service — Initiate Verification (REST)](#8-self-service--initiate-verification-rest)
9. [Admin — Read Sellers (GraphQL)](#9-admin--read-sellers-graphql)
10. [Admin — Lifecycle Actions (REST)](#10-admin--lifecycle-actions-rest)
11. [Admin — Review Verification (REST)](#11-admin--review-verification-rest)
12. [Status Transition Rules](#12-status-transition-rules)
13. [Verification Flow](#13-verification-flow)
14. [Error Reference](#14-error-reference)
15. [Quick Reference](#15-quick-reference)

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

`data` is `null` for void endpoints. `message` is always safe to display to the user.

### GraphQL envelope

```json
{
  "data": {
    "mySeller": { ... }
  }
}
```

On error:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Seller profile not found",
      "extensions": { "status": 404 }
    }
  ]
}
```

Check `errors[0].message` and `errors[0].extensions.status` for GraphQL error handling.

---

## 2. Reference Types

### `SellerStatus`

| Value | Meaning |
|---|---|
| `DRAFT` | Profile created but onboarding not yet submitted |
| `PENDING_VERIFICATION` | Application submitted — awaiting admin review |
| `UNDER_REVIEW` | Admin is actively reviewing the application |
| `ACTIVE` | Fully approved and live on the platform |
| `SUSPENDED` | Temporarily restricted by admin; optional auto-expiry |
| `REJECTED` | Application rejected — seller can correct and resubmit |
| `BLOCKED` | Permanently restricted by admin — no self-recovery |
| `CLOSED` | Account permanently closed |

### `SellerType`

| Value | Meaning |
|---|---|
| `INDIVIDUAL` | Single person selling on the platform (default) |
| `BUSINESS` | Registered business entity |
| `ORGANIZATION` | NGO, cooperative, or similar organization |

### `VerificationType`

| Value | Meaning |
|---|---|
| `IDENTITY` | Government-issued ID check |
| `BUSINESS` | Business registration documents |
| `CONTACT` | Contact details verification |
| `PAYOUT` | Bank account / payout method verification |

### `VerificationItemStatus`

| Value | Meaning |
|---|---|
| `NOT_STARTED` | Verification item has not been submitted yet |
| `PENDING` | Submitted by seller, awaiting admin review |
| `IN_REVIEW` | Admin is actively reviewing |
| `VERIFIED` | Approved |
| `REJECTED` | Rejected — seller must resubmit |
| `EXPIRED` | Previously verified but now past its validity period |

### TypeScript interfaces

```typescript
interface SellerProfile {
  publicId:      string;           // UUID
  sellerNumber:  string;           // e.g. "SEL-000001"
  displayName:   string | null;
  sellerType:    SellerType;
  status:        SellerStatus;
  business:      SellerBusiness | null;
  verifications: SellerVerification[];
  createdAt:     string;           // ISO-8601 UTC
  updatedAt:     string;
}

interface SellerBusiness {
  publicId:           string;
  legalName:          string | null;
  businessName:       string | null;
  businessType:       string | null;
  registrationNumber: string | null;
  taxIdentifier:      string | null;
  description:        string | null;
  website:            string | null;
  email:              string | null;
  phone:              string | null;
  country:            string | null;
  region:             string | null;
  city:               string | null;
  address:            string | null;
  createdAt:          string;
  updatedAt:          string;
}

interface SellerVerification {
  publicId:         string;
  verificationType: VerificationType;
  status:           VerificationItemStatus;
  submittedAt:      string | null;
  reviewedAt:       string | null;
  rejectionReason:  string | null;
  expiresAt:        string | null;
  createdAt:        string;
  updatedAt:        string;
}

interface SellerStatusHistory {
  previousStatus: SellerStatus | null;
  newStatus:      SellerStatus;
  reason:         string | null;
  changedBy:      string | null;   // UUID of admin who made the change
  expiresAt:      string | null;   // set when suspension has a fixed end date
  createdAt:      string;
}

interface SellerSummary {           // used in admin paginated list
  publicId:     string;
  sellerNumber: string;
  displayName:  string | null;
  sellerType:   SellerType;
  status:       SellerStatus;
  createdAt:    string;
}
```

---

## 3. How Seller Profiles Are Created

Seller profiles are provisioned automatically — you do **not** create one manually. The flow is:

1. Call `POST /v1/auth/register` with `role: "SELLER"` (see [auth.md](auth.md))
2. The backend publishes a `UserRegisteredEvent` after the user row is committed
3. `SellerProvisioningListener` asynchronously creates the `Seller` record with `status: DRAFT`, `sellerType: INDIVIDUAL`, and a unique `sellerNumber` (e.g. `SEL-000001`)

**What this means for your UI**: after registration and email verification, query `mySeller` via GraphQL — the profile will be there. Status starts at `DRAFT`; the seller must complete onboarding before going live.

---

## 4. Self-Service — Read My Profile (GraphQL)

All seller reads go through GraphQL at `POST /graphql`. Requires `Authorization: Bearer <accessToken>` from a `SELLER` account.

### Get my full seller profile

```graphql
query MySeller {
  mySeller {
    publicId
    sellerNumber
    displayName
    sellerType
    status
    business {
      publicId
      legalName
      businessName
      businessType
      registrationNumber
      taxIdentifier
      description
      website
      email
      phone
      country
      region
      city
      address
      updatedAt
    }
    verifications {
      publicId
      verificationType
      status
      submittedAt
      reviewedAt
      rejectionReason
      expiresAt
    }
    createdAt
    updatedAt
  }
}
```

**Response**

```json
{
  "data": {
    "mySeller": {
      "publicId": "a1b2c3d4-...",
      "sellerNumber": "SEL-000001",
      "displayName": "Jane's Store",
      "sellerType": "INDIVIDUAL",
      "status": "DRAFT",
      "business": null,
      "verifications": [],
      "createdAt": "2026-01-15T09:00:00Z",
      "updatedAt": "2026-09-08T12:00:00Z"
    }
  }
}
```

`business` is `null` until the seller saves their onboarding details for the first time. `verifications` is an empty array until the seller initiates at least one verification item.

---

## 5. Self-Service — Update Display Name (REST)

```
PATCH /v1/sellers/me
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request**

```json
{
  "displayName": "Jane's Boutique"
}
```

| Field | Constraint |
|---|---|
| `displayName` | Optional · max 255 characters |

**Response `200`**

```json
{
  "success": true,
  "message": "Seller profile updated",
  "data": {
    "publicId":     "a1b2c3d4-...",
    "sellerNumber": "SEL-000001",
    "displayName":  "Jane's Boutique",
    "sellerType":   "INDIVIDUAL",
    "status":       "DRAFT",
    "createdAt":    "2026-01-15T09:00:00Z",
    "updatedAt":    "2026-09-09T08:30:00Z"
  }
}
```

After a successful update, re-query `mySeller` via GraphQL to refresh your local state.

---

## 6. Self-Service — Business Onboarding (REST)

Captures the seller's business details. Both `POST` (first save) and `PATCH` (subsequent updates) accept the same body — all fields are optional so the seller can save progress incrementally.

```
POST  /v1/sellers/me/onboarding    ← first submission
PATCH /v1/sellers/me/onboarding    ← subsequent updates
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request** (all fields optional — send only what changed)

```json
{
  "legalName":          "Jane Doe",
  "businessName":       "Jane's Boutique",
  "businessType":       "Retail",
  "registrationNumber": "GH-1234567",
  "taxIdentifier":      "TIN-9876543",
  "description":        "Handmade accessories and fashion",
  "website":            "https://janesboutique.com",
  "email":              "contact@janesboutique.com",
  "phone":              "+233201234567",
  "country":            "Ghana",
  "region":             "Greater Accra",
  "city":               "Accra",
  "address":            "14 Ring Road East, Osu"
}
```

| Field | Constraint |
|---|---|
| `legalName` / `businessName` | max 255 characters |
| `businessType` | max 50 characters |
| `registrationNumber` / `taxIdentifier` | max 100 characters |
| `website` / `address` | max 500 characters |
| `email` | max 255 characters |
| `phone` | max 50 characters |
| `country` / `region` / `city` | max 100 characters each |

**Response `200`**

```json
{
  "success": true,
  "message": "Onboarding saved",
  "data": {
    "publicId":     "a1b2c3d4-...",
    "sellerNumber": "SEL-000001",
    "displayName":  "Jane's Boutique",
    "sellerType":   "INDIVIDUAL",
    "status":       "DRAFT",
    "business": {
      "publicId":           "b2c3d4e5-...",
      "legalName":          "Jane Doe",
      "businessName":       "Jane's Boutique",
      "country":            "Ghana",
      "city":               "Accra",
      "updatedAt":          "2026-09-09T08:30:00Z"
    },
    "verifications": [],
    "createdAt":    "2026-01-15T09:00:00Z",
    "updatedAt":    "2026-09-09T08:30:00Z"
  }
}
```

After saving, re-query `mySeller` via GraphQL to get the full updated business object.

---

## 7. Self-Service — Submit Application (REST)

Once onboarding details are complete, the seller submits their application. This transitions status from `DRAFT` → `PENDING_VERIFICATION`.

```
POST /v1/sellers/me/application
Authorization: Bearer <accessToken>
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Application submitted",
  "data": {
    "publicId":     "a1b2c3d4-...",
    "sellerNumber": "SEL-000001",
    "displayName":  "Jane's Boutique",
    "sellerType":   "INDIVIDUAL",
    "status":       "PENDING_VERIFICATION",
    "createdAt":    "2026-01-15T09:00:00Z",
    "updatedAt":    "2026-09-09T09:00:00Z"
  }
}
```

**`422 Unprocessable Entity`** if the seller is not in `DRAFT` status.

**UI guidance**: after a successful submit, re-query `mySeller` and show a "Your application is under review" message. Disable the submit button — the application cannot be resubmitted until an admin moves the seller back to `DRAFT` (e.g. after rejection).

---

## 8. Self-Service — Initiate Verification (REST)

Verification items represent individual checks (identity, business documents, etc.) that make up the seller approval process. The seller initiates each one when they are ready to submit the relevant documents.

```
POST /v1/sellers/me/verification?type={verificationType}
Authorization: Bearer <accessToken>
```

**Query parameter**

| Parameter | Type | Required | Values |
|---|---|---|---|
| `type` | `VerificationType` | Yes | `IDENTITY` \| `BUSINESS` \| `CONTACT` \| `PAYOUT` |

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Verification submitted",
  "data": {
    "publicId":         "c3d4e5f6-...",
    "verificationType": "IDENTITY",
    "status":           "PENDING",
    "submittedAt":      "2026-09-09T09:05:00Z",
    "reviewedAt":       null,
    "rejectionReason":  null,
    "expiresAt":        null,
    "createdAt":        "2026-09-09T09:05:00Z",
    "updatedAt":        "2026-09-09T09:05:00Z"
  }
}
```

Calling this endpoint transitions an existing `NOT_STARTED` or `REJECTED` item to `PENDING`, or creates a new item if it does not yet exist.

After initiating, re-query `mySeller { verifications { ... } }` via GraphQL to refresh the verification checklist.

---

## 9. Admin — Read Sellers (GraphQL)

All admin reads go through GraphQL. Requires an `ADMIN` role bearer token.

### Get a seller by ID

```graphql
query GetSeller($id: ID!) {
  seller(id: $id) {
    publicId
    sellerNumber
    displayName
    sellerType
    status
    business {
      legalName
      businessName
      businessType
      registrationNumber
      country
      city
    }
    verifications {
      verificationType
      status
      submittedAt
      rejectionReason
    }
    createdAt
    updatedAt
  }
}
```

**Variables**

```json
{ "id": "a1b2c3d4-..." }
```

Returns `null` if not found.

---

### Search / list sellers

```graphql
query ListSellers($filter: SellerFilterInput) {
  sellers(filter: $filter) {
    content {
      publicId
      sellerNumber
      displayName
      sellerType
      status
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
  "filter": {
    "query":      "jane",
    "status":     "PENDING_VERIFICATION",
    "sellerType": "INDIVIDUAL",
    "page":       0,
    "size":       20
  }
}
```

All filter fields are optional. `query` matches against `displayName`, `sellerNumber`, and business name. Omit `filter` entirely to list all sellers.

---

### Get seller status history

Returns the full audit trail for a seller, newest first.

```graphql
query SellerStatusHistory($sellerId: ID!) {
  sellerStatusHistory(sellerId: $sellerId) {
    previousStatus
    newStatus
    reason
    changedBy
    expiresAt
    createdAt
  }
}
```

**Variables**

```json
{ "sellerId": "a1b2c3d4-..." }
```

---

## 10. Admin — Lifecycle Actions (REST)

All admin mutations require an `ADMIN` bearer token. `{id}` in all paths is the seller's public UUID.

### Approve a seller

Moves the seller from `UNDER_REVIEW` to `ACTIVE`. No request body.

```
POST /v1/admin/sellers/{id}/approve
Authorization: Bearer <accessToken>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Seller approved",
  "data": { ...SellerResponse (status: "ACTIVE") }
}
```

---

### Reject a seller

Moves the seller from `UNDER_REVIEW` or `PENDING_VERIFICATION` to `REJECTED`.

```
POST /v1/admin/sellers/{id}/reject
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request** (optional body)

```json
{
  "reason": "Incomplete business registration documents"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Seller rejected",
  "data": { ...SellerResponse (status: "REJECTED") }
}
```

---

### Suspend a seller

Moves the seller from `ACTIVE` to `SUSPENDED`. An optional `expiresAt` triggers automatic reactivation by a background job.

```
POST /v1/admin/sellers/{id}/suspend
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request**

```json
{
  "reason":    "Multiple unresolved customer complaints",
  "expiresAt": "2026-10-01T00:00:00Z"
}
```

| Field | Required | Notes |
|---|---|---|
| `reason` | No | Stored in status history and audit log |
| `expiresAt` | No | ISO-8601 UTC — omit for indefinite suspension |

**Response `200`**

```json
{
  "success": true,
  "message": "Seller suspended",
  "data": { ...SellerResponse (status: "SUSPENDED") }
}
```

**`422`** if the transition is invalid (e.g. suspending a `BLOCKED` seller).

---

### Reactivate a seller

Moves a `SUSPENDED` seller back to `ACTIVE`. No request body.

```
POST /v1/admin/sellers/{id}/activate
Authorization: Bearer <accessToken>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Seller reactivated",
  "data": { ...SellerResponse (status: "ACTIVE") }
}
```

---

### Block a seller

Moves a seller to `BLOCKED`. Stronger than suspension — blocked sellers cannot be reactivated.

```
POST /v1/admin/sellers/{id}/block
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request**

```json
{
  "reason": "Fraud detected — account permanently restricted"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Seller blocked",
  "data": { ...SellerResponse (status: "BLOCKED") }
}
```

---

## 11. Admin — Review Verification (REST)

```
POST /v1/admin/sellers/{id}/verification/review?type={verificationType}&approved={true|false}&reason={text}
Authorization: Bearer <accessToken>
```

**Query parameters**

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `type` | `VerificationType` | Yes | Which item to review (`IDENTITY`, `BUSINESS`, etc.) |
| `approved` | `boolean` | Yes | `true` → `VERIFIED`; `false` → `REJECTED` |
| `reason` | `String` | No | Required when `approved=false` |

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Verification reviewed",
  "data": {
    "publicId":         "c3d4e5f6-...",
    "verificationType": "IDENTITY",
    "status":           "VERIFIED",
    "submittedAt":      "2026-09-01T09:05:00Z",
    "reviewedAt":       "2026-09-02T10:00:00Z",
    "rejectionReason":  null,
    "expiresAt":        null,
    "createdAt":        "2026-09-01T09:05:00Z",
    "updatedAt":        "2026-09-02T10:00:00Z"
  }
}
```

---

## 12. Status Transition Rules

Only the following transitions are permitted. Any other transition returns `422 Unprocessable Entity`.

```
DRAFT                ──→  PENDING_VERIFICATION   (seller submits application)
PENDING_VERIFICATION ──→  UNDER_REVIEW           (admin begins review)
PENDING_VERIFICATION ──→  REJECTED               (admin rejects directly)
UNDER_REVIEW         ──→  ACTIVE                 (admin approves)
UNDER_REVIEW         ──→  REJECTED               (admin rejects)
REJECTED             ──→  DRAFT                  (seller can correct and resubmit)
ACTIVE               ──→  SUSPENDED | BLOCKED | CLOSED
SUSPENDED            ──→  ACTIVE | BLOCKED | CLOSED
BLOCKED              ──→  CLOSED
CLOSED               ──→  (none — terminal state)
```

**UI guidance by status**

| Status | What to show |
|---|---|
| `DRAFT` | Onboarding form + "Submit application" button |
| `PENDING_VERIFICATION` | "Application under review" — no actions available |
| `UNDER_REVIEW` | "Under review" — no actions available |
| `ACTIVE` | Normal seller dashboard |
| `SUSPENDED` | Suspension reason + expiry date if set; hide listing/store actions |
| `REJECTED` | Rejection reason prominently; allow editing details and resubmitting |
| `BLOCKED` | Block message; no recovery path |
| `CLOSED` | Account closed; no actions |

**Admin panel button visibility**

| Current status | Show |
|---|---|
| `PENDING_VERIFICATION` or `UNDER_REVIEW` | Approve + Reject |
| `ACTIVE` | Suspend + Block |
| `SUSPENDED` | Activate + Block |
| `REJECTED` | Block |

---

## 13. Verification Flow

```
1. Seller submits application   → seller status: PENDING_VERIFICATION
2. Seller initiates each verification item
   POST /v1/sellers/me/verification?type=IDENTITY  → item status: PENDING
3. Admin reviews each item
   POST /v1/admin/sellers/{id}/verification/review?type=IDENTITY&approved=true
   → item status: VERIFIED
4. Admin approves the seller once all checks pass
   POST /v1/admin/sellers/{id}/approve
   → seller status: ACTIVE
```

**Verification item lifecycle**

```
NOT_STARTED → PENDING    (seller initiates)
PENDING     → IN_REVIEW  (admin starts review)
IN_REVIEW   → VERIFIED | REJECTED
REJECTED    → PENDING    (seller resubmits)
VERIFIED    → EXPIRED    (if expiry date passes)
EXPIRED     → PENDING    (seller resubmits for renewal)
```

**UI guidance**:
- Show a checklist of all four `VerificationType` values with their current `VerificationItemStatus`.
- When an item is `REJECTED`, show `rejectionReason` prominently so the seller knows what to fix before resubmitting.
- When a `VERIFIED` item has `expiresAt` set, show a renewal warning 30 days before expiry.

---

## 14. Error Reference

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure (field too long, missing required query param) |
| `401 Unauthorized` | Missing or expired access token — refresh and retry |
| `403 Forbidden` | Wrong role (e.g. `CUSTOMER` calling a `SELLER` endpoint) |
| `404 Not Found` | Seller UUID does not exist |
| `409 Conflict` | Seller profile already exists for this user |
| `422 Unprocessable Entity` | Invalid status transition |

For `400` errors, `data` may contain a field-error map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "legalName": "size must be between 0 and 255"
  }
}
```

**On `401`**: call `POST /v1/auth/refresh-token`. On failure, redirect to login.  
**On `422`**: disable the action button based on current `status` to prevent the user from retrying an impossible transition (see [§12](#12-status-transition-rules)).

---

## 15. Quick Reference

```
Self-service mutations — REST (requires SELLER role)
─────────────────────────────────────────────────────────────────────
PATCH  /v1/sellers/me                                 🔒  Update display name
POST   /v1/sellers/me/onboarding                      🔒  Save onboarding (initial)
PATCH  /v1/sellers/me/onboarding                      🔒  Update onboarding details
POST   /v1/sellers/me/application                     🔒  Submit application for review
POST   /v1/sellers/me/verification?type=...           🔒  Initiate / resubmit a verification item

Self-service reads — GraphQL (requires SELLER role)
─────────────────────────────────────────────────────────────────────
query mySeller          Full profile: displayName, status, business details, verifications

Admin mutations — REST (requires ADMIN role)
─────────────────────────────────────────────────────────────────────
POST   /v1/admin/sellers/{id}/approve                 🔒  Approve seller → ACTIVE
POST   /v1/admin/sellers/{id}/reject                  🔒  Reject seller → REJECTED
POST   /v1/admin/sellers/{id}/suspend                 🔒  Suspend seller → SUSPENDED
POST   /v1/admin/sellers/{id}/activate                🔒  Reactivate seller → ACTIVE
POST   /v1/admin/sellers/{id}/block                   🔒  Block seller → BLOCKED
POST   /v1/admin/sellers/{id}/verification/review     🔒  Approve or reject a verification item

Admin reads — GraphQL (requires ADMIN role)
─────────────────────────────────────────────────────────────────────
query seller(id)                          Get seller by public UUID
query sellers(filter)                     Search / paginated list
query sellerStatusHistory(sellerId)       Full status audit trail

🔒 = requires Authorization: Bearer <accessToken>
```
