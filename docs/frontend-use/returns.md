# Returns & Refund Management API — Frontend Integration Guide

Base paths:
- `/v1/returns` — customer mutations
- `/v1/seller/returns` — seller mutations
- `/v1/admin/returns` — admin mutations
- `/v1/admin/return-policies` — policy management
- `POST /graphql` — all reads

All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**
> Return submissions, status transitions, inspections, refunds, and exchanges are mutations — use the REST endpoints below.
> All listing, detail, audit trail, and analytics queries go through GraphQL.
> See [auth.md](auth.md) for token acquisition. See [order.md](order.md) for the order that precedes a return. See [payment.md](payment.md) for the underlying refund payment flow.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [TypeScript Interfaces](#3-typescript-interfaces)
4. [Return Lifecycle Overview](#4-return-lifecycle-overview)
5. [Customer: Check Eligibility (GraphQL)](#5-customer-check-eligibility-graphql)
6. [Customer: Submit a Return (REST)](#6-customer-submit-a-return-rest)
7. [Customer: Add Evidence (REST)](#7-customer-add-evidence-rest)
8. [Customer: Cancel a Return (REST)](#8-customer-cancel-a-return-rest)
9. [Seller: Review, Approve, Reject (REST)](#9-seller-review-approve-reject-rest)
10. [Seller: Initiate Return Shipment (REST)](#10-seller-initiate-return-shipment-rest)
11. [Seller: Run Inspection (REST)](#11-seller-run-inspection-rest)
12. [Seller: Trigger Refund or Exchange (REST)](#12-seller-trigger-refund-or-exchange-rest)
13. [Admin: Escalate, Transit, Received, Reconcile (REST)](#13-admin-escalate-transit-received-reconcile-rest)
14. [Return Policy Management (REST)](#14-return-policy-management-rest)
15. [GraphQL Queries](#15-graphql-queries)
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

`message` is always safe to display to the user.

### GraphQL errors

```json
{
  "errors": [{ "message": "What went wrong", "extensions": { "status": 422 } }],
  "data": null
}
```

---

## 2. Reference Types

### `ReturnStatus`

| Value | Who sets it | Notes |
|---|---|---|
| `REQUESTED` | System (on create) | Awaiting seller/admin review |
| `UNDER_REVIEW` | Seller / Admin | Return is being evaluated |
| `APPROVED` | Seller / Admin | Approved; awaiting return shipment |
| `REJECTED` | Seller / Admin | Terminal — return denied |
| `RETURN_SHIPPING` | Seller | Customer shipping item back |
| `IN_TRANSIT` | Admin | Package picked up by carrier |
| `RECEIVED` | Admin | Warehouse received the package |
| `INSPECTION` | Seller / Admin | Physical inspection in progress |
| `APPROVED_FOR_REFUND` | System (inspection) | Full refund resolution |
| `PARTIALLY_APPROVED` | System (inspection) | Partial refund resolution |
| `REFUND_REQUESTED` | Seller / Admin | Refund calculation sent to payment module |
| `RESOLVED` | System | Refund succeeded — terminal |
| `FAILED` | System | Refund failed — retryable |
| `CANCELLED` | Customer | Customer withdrew the request |
| `EXPIRED` | System (scheduler) | Return window lapsed |

### `ReturnReason`

| Value | Display label |
|---|---|
| `DAMAGED_IN_TRANSIT` | Damaged in Transit |
| `DEFECTIVE_PRODUCT` | Defective Product |
| `WRONG_ITEM_RECEIVED` | Wrong Item Received |
| `MISSING_ITEM` | Missing Item |
| `NOT_AS_DESCRIBED` | Not as Described |
| `WRONG_SIZE` | Wrong Size |
| `WRONG_COLOR` | Wrong Colour |
| `QUALITY_ISSUE` | Quality Issue |
| `CHANGED_MIND` | Changed Mind |
| `NO_LONGER_NEEDED` | No Longer Needed |
| `DUPLICATE_ORDER` | Duplicate Order |
| `COUNTERFEIT_SUSPECTED` | Counterfeit Suspected |
| `LATE_DELIVERY` | Late Delivery |
| `OTHER` | Other |

### `ReturnItemCondition` (customer-reported)

| Value | Display label |
|---|---|
| `UNOPENED` | Unopened |
| `OPENED_UNUSED` | Opened, Unused |
| `LIGHTLY_USED` | Lightly Used |
| `HEAVILY_USED` | Heavily Used |
| `DAMAGED` | Damaged |
| `DEFECTIVE` | Defective |

### `ReturnEvidenceType`

| Value | Use |
|---|---|
| `PHOTO` | Product photo |
| `VIDEO` | Short video clip |
| `DOCUMENT` | Receipt, warranty, etc. |
| `OTHER` | Anything else |

### `InspectionCondition` (warehouse-reported)

`NEW` · `LIKE_NEW` · `GOOD` · `USED` · `DAMAGED` · `DEFECTIVE` · `INCOMPLETE` · `COUNTERFEIT` · `UNKNOWN`

### `InspectionResult`

| Value | Outcome |
|---|---|
| `PASS` | Item in acceptable condition |
| `PARTIAL_PASS` | Partial approval |
| `FAIL` | Item not acceptable |
| `FRAUD_REVIEW` | Flagged for fraud review |

### `ReturnResolution`

| Value | Effect |
|---|---|
| `FULL_REFUND` | Status → `APPROVED_FOR_REFUND` |
| `PARTIAL_REFUND` | Status → `PARTIALLY_APPROVED` |
| `REJECT_RETURN` | Status → `REJECTED` |

### `ReturnDispositionType`

`RESTOCK` · `DAMAGED` · `REFURBISH` · `QUARANTINE` · `DISPOSE` · `RETURN_TO_SUPPLIER`

### `RefundStatus` (internal to `ReturnRefundResponse`)

`PENDING` · `APPROVED` · `PROCESSING` · `SUCCEEDED` · `PARTIALLY_SUCCEEDED` · `FAILED` · `CANCELLED`

### `ExchangeStatus`

`PENDING` · `PROCESSING` · `COMPLETED` · `CANCELLED`

---

## 3. TypeScript Interfaces

```typescript
type ReturnStatus =
  | "REQUESTED" | "UNDER_REVIEW" | "APPROVED" | "REJECTED"
  | "RETURN_SHIPPING" | "IN_TRANSIT" | "RECEIVED" | "INSPECTION"
  | "APPROVED_FOR_REFUND" | "PARTIALLY_APPROVED" | "REFUND_REQUESTED"
  | "RESOLVED" | "FAILED" | "CANCELLED" | "EXPIRED";

type ReturnReason =
  | "DAMAGED_IN_TRANSIT" | "DEFECTIVE_PRODUCT" | "WRONG_ITEM_RECEIVED"
  | "MISSING_ITEM" | "NOT_AS_DESCRIBED" | "WRONG_SIZE" | "WRONG_COLOR"
  | "QUALITY_ISSUE" | "CHANGED_MIND" | "NO_LONGER_NEEDED"
  | "DUPLICATE_ORDER" | "COUNTERFEIT_SUSPECTED" | "LATE_DELIVERY" | "OTHER";

type ReturnItemCondition =
  | "UNOPENED" | "OPENED_UNUSED" | "LIGHTLY_USED" | "HEAVILY_USED"
  | "DAMAGED" | "DEFECTIVE";

type ReturnEvidenceType = "PHOTO" | "VIDEO" | "DOCUMENT" | "OTHER";

type InspectionCondition =
  | "NEW" | "LIKE_NEW" | "GOOD" | "USED" | "DAMAGED"
  | "DEFECTIVE" | "INCOMPLETE" | "COUNTERFEIT" | "UNKNOWN";

type InspectionResult = "PASS" | "PARTIAL_PASS" | "FAIL" | "FRAUD_REVIEW";
type ReturnResolution = "FULL_REFUND" | "PARTIAL_REFUND" | "REJECT_RETURN";
type ReturnDispositionType =
  | "RESTOCK" | "DAMAGED" | "REFURBISH" | "QUARANTINE" | "DISPOSE" | "RETURN_TO_SUPPLIER";
type RefundStatus =
  | "PENDING" | "APPROVED" | "PROCESSING" | "SUCCEEDED"
  | "PARTIALLY_SUCCEEDED" | "FAILED" | "CANCELLED";
type ExchangeStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "CANCELLED";

/** Shared item on every ReturnResponse */
interface ReturnItemResponse {
  publicId:         string;
  orderItemId:      string;
  productId:        string | null;
  productName:      string;
  quantity:         number;
  reason:           ReturnReason;
  condition:        ReturnItemCondition | null;
  unitPrice:        number | null;          // GHS
  approvedQuantity: number | null;
  receivedQuantity: number | null;
  resolution:       ReturnResolution | null;
  createdAt:        string;                 // ISO-8601 UTC
}

/** Main return object — returned by nearly every REST endpoint */
interface ReturnResponse {
  publicId:              string;            // UUID — use this in all API calls
  returnNumber:          string;            // e.g. "RET-2026-000042"
  orderId:               string;
  customerId:            string;
  sellerId:              string | null;
  storeId:               string | null;
  status:                ReturnStatus;
  reason:                ReturnReason;
  customerNote:          string | null;
  adminNote:             string | null;
  rejectionReason:       string | null;
  returnDeadline:        string | null;     // ISO-8601 UTC
  requestedAt:           string;
  approvedAt:            string | null;
  receivedAt:            string | null;
  rejectedAt:            string | null;
  resolvedAt:            string | null;
  returnShipmentId:      string | null;
  returnLabelReference:  string | null;
  isEscalated:           boolean;
  escalatedAt:           string | null;
  items:                 ReturnItemResponse[];
  createdAt:             string;
  updatedAt:             string;
}

/** POST /v1/returns — request body */
interface CreateReturnRequest {
  orderId:      string;                     // UUID
  reason:       ReturnReason;
  customerNote?: string;
  items: Array<{
    orderItemId: string;                    // UUID
    quantity:    number;
    reason:      ReturnReason;
    condition?:  ReturnItemCondition;
  }>;
}

/** POST /v1/returns/{returnId}/evidence — request body */
interface AddReturnEvidenceRequest {
  returnItemId?:   string;                  // UUID — omit to attach to whole return
  mediaReference:  string;                  // URL or storage key of the uploaded file
  evidenceType:    ReturnEvidenceType;
  description?:    string;
}

/** POST /v1/seller/returns/{returnId}/complete-inspection — request body */
interface CompleteInspectionRequest {
  resolution:    ReturnResolution;
  overallNotes?: string;
  itemResults: Array<{
    returnItemId:        string;            // UUID
    condition:           InspectionCondition;
    result:              InspectionResult;
    approvedQuantity?:   number;
    receivedQuantity?:   number;
    dispositionType?:    ReturnDispositionType;
    dispositionQuantity?: number;
    locationId?:         string;
    notes?:              string;
  }>;
}

/** Refund record for a return */
interface ReturnRefundResponse {
  publicId:         string;
  returnId:         string;
  orderId:          string;
  paymentId:        string | null;
  requestedAmount:  number;   // GHS
  approvedAmount:   number | null;
  currency:         string;   // "GHS"
  reason:           string | null;
  status:           RefundStatus;
  providerRefundId: string | null;
  providerReference:string | null;
  failureReason:    string | null;
  requestedAt:      string;
  approvedAt:       string | null;
  processedAt:      string | null;
  createdAt:        string;
}

/** Exchange record for a return */
interface ReturnExchangeResponse {
  publicId:                   string;
  returnId:                   string;
  originalOrderId:            string;
  exchangeOrderId:            string | null;
  status:                     ExchangeStatus;
  requestedItemsDescription:  string | null;
  notes:                      string | null;
  requestedBy:                string | null;
  processedBy:                string | null;
  processedAt:                string | null;
  createdAt:                  string;
  updatedAt:                  string;
}
```

> All monetary values are GHS. Parse with a decimal library — do not use `parseFloat`.

---

## 4. Return Lifecycle Overview

```
Customer checks eligibility
        │  GraphQL: returnEligibility(orderId)
        │  → eligible: true/false + deadline + allowedReasons
        ▼
Customer submits return
        │  POST /v1/returns
        │  → status: REQUESTED
        │  Optionally: POST /v1/returns/{id}/evidence (add photos/videos)
        ▼
Seller reviews
        │  POST /v1/seller/returns/{id}/review     → UNDER_REVIEW
        ├─ POST /v1/seller/returns/{id}/approve    → APPROVED
        └─ POST /v1/seller/returns/{id}/reject     → REJECTED (terminal)
        ▼  (if approved)
Seller initiates return shipping
        │  POST /v1/seller/returns/{id}/initiate-shipment  → RETURN_SHIPPING
        ▼
Carrier picks up package
        │  [shipping module event / admin override]
        │  POST /v1/admin/returns/{id}/in-transit          → IN_TRANSIT
        ▼
Warehouse receives package
        │  POST /v1/admin/returns/{id}/received            → RECEIVED
        ▼
Inspection
        │  POST /v1/seller/returns/{id}/inspect            → INSPECTION
        │  POST /v1/seller/returns/{id}/complete-inspection
        │    └─ resolution: FULL_REFUND    → APPROVED_FOR_REFUND
        │    └─ resolution: PARTIAL_REFUND → PARTIALLY_APPROVED
        │    └─ resolution: REJECT_RETURN  → REJECTED (terminal)
        ▼
Refund or Exchange
        ├─ POST /v1/seller/returns/{id}/request-refund
        │    └─ success → RESOLVED (terminal)
        │    └─ fail    → FAILED (retryable — call request-refund again)
        └─ POST /v1/seller/returns/{id}/exchange
             └─ Exchange record created; fulfilled externally
```

**System-initiated transitions:**
- Auto-expiry: `REQUESTED` / `APPROVED` / `RETURN_SHIPPING` → `EXPIRED` when `returnDeadline` passes (hourly scheduler)
- Fraud signals: detected on every new return; severity ≥ 7 auto-sets `isEscalated = true`

---

## 5. Customer: Check Eligibility (GraphQL)

Always call this before showing the return submission form. It tells you whether the order is eligible, how many days remain, and which reasons are permitted.

```graphql
query ReturnEligibility($orderId: ID!) {
  returnEligibility(orderId: $orderId) {
    eligible
    deadline
    remainingDays
    reason
    allowedReasons
    maximumReturnQuantity
    conditions
    warnings
  }
}
```

**Variables**

```json
{ "orderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6" }
```

**Response (eligible)**

```json
{
  "data": {
    "returnEligibility": {
      "eligible": true,
      "deadline": "2026-10-07T00:00:00Z",
      "remainingDays": 14,
      "reason": null,
      "allowedReasons": ["DAMAGED_IN_TRANSIT", "DEFECTIVE_PRODUCT", "WRONG_ITEM_RECEIVED"],
      "maximumReturnQuantity": null,
      "conditions": [],
      "warnings": []
    }
  }
}
```

**Response (ineligible)**

```json
{
  "data": {
    "returnEligibility": {
      "eligible": false,
      "deadline": "2026-09-01T00:00:00Z",
      "remainingDays": 0,
      "reason": "Return window of 30 days has expired",
      "allowedReasons": null
    }
  }
}
```

If `eligible` is `false`, show `reason` to the user and do not display the return form.

---

## 6. Customer: Submit a Return (REST)

Creates a return request for a delivered order. Each item in the order that the customer wants to return must be listed separately. The return enters `REQUESTED` status.

```
POST /v1/returns
Authorization: Bearer <accessToken>   ← return:create permission
Content-Type: application/json
```

**Request body**

```json
{
  "orderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "reason": "DEFECTIVE_PRODUCT",
  "customerNote": "The zipper broke on first use.",
  "items": [
    {
      "orderItemId": "a1b2c3d4-1111-2222-3333-444455556666",
      "quantity": 1,
      "reason": "DEFECTIVE_PRODUCT",
      "condition": "DEFECTIVE"
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `orderId` | Yes | UUID of the delivered order |
| `reason` | Yes | Overall `ReturnReason` for the request |
| `customerNote` | No | Free-text explanation — visible to seller |
| `items` | Yes | At least one item required |
| `items[].orderItemId` | Yes | UUID of the `OrderItem` to return |
| `items[].quantity` | Yes | Minimum 1; cannot exceed ordered quantity |
| `items[].reason` | Yes | Per-item `ReturnReason` |
| `items[].condition` | No | Customer-reported `ReturnItemCondition` |

**Response `201`**

```json
{
  "success": true,
  "message": "Return request submitted successfully",
  "data": {
    "publicId": "r1r2r3r4-...",
    "returnNumber": "RET-2026-000042",
    "orderId": "3fa85f64-...",
    "customerId": "c1c2c3c4-...",
    "sellerId": "s1s2s3s4-...",
    "status": "REQUESTED",
    "reason": "DEFECTIVE_PRODUCT",
    "customerNote": "The zipper broke on first use.",
    "returnDeadline": "2026-10-07T00:00:00Z",
    "requestedAt": "2026-09-23T10:00:00Z",
    "isEscalated": false,
    "items": [
      {
        "publicId": "i1i2i3i4-...",
        "orderItemId": "a1b2c3d4-...",
        "productName": "Canvas Backpack",
        "quantity": 1,
        "reason": "DEFECTIVE_PRODUCT",
        "condition": "DEFECTIVE",
        "unitPrice": 129.99
      }
    ],
    "createdAt": "2026-09-23T10:00:00Z",
    "updatedAt": "2026-09-23T10:00:00Z"
  }
}
```

**`400`** — missing required field.
**`403`** — order does not belong to the authenticated customer.
**`404`** — order not found.
**`422`** — order not delivered, return window expired, active return already exists, or reason not permitted by policy.

> Save `data.publicId` — this is the `returnId` used in every subsequent call.

---

## 7. Customer: Add Evidence (REST)

Attach photos or documents to support the return claim. Can be called multiple times. Optionally scoped to a specific item.

```
POST /v1/returns/{returnId}/evidence
Authorization: Bearer <accessToken>   ← return:create permission
Content-Type: application/json
```

Upload your file to media storage first (see [media.md](media.md)), then pass the resulting reference here.

**Path parameter**

| Parameter | Type |
|---|---|
| `returnId` | UUID — the `publicId` from `ReturnResponse` |

**Request body**

```json
{
  "returnItemId": "i1i2i3i4-...",
  "mediaReference": "https://cdn.fynza.com/returns/r1r2r3r4/zipper-defect.jpg",
  "evidenceType": "PHOTO",
  "description": "Close-up of the broken zipper teeth"
}
```

| Field | Required | Notes |
|---|---|---|
| `returnItemId` | No | Omit to attach evidence to the whole return |
| `mediaReference` | Yes | URL or storage key of the uploaded file |
| `evidenceType` | Yes | `PHOTO` · `VIDEO` · `DOCUMENT` · `OTHER` |
| `description` | No | Short label visible to the reviewer |

**Response `201`**

```json
{
  "success": true,
  "message": "Evidence added",
  "data": {
    "publicId": "e1e2e3e4-...",
    "returnId": "r1r2r3r4-...",
    "returnItemId": "i1i2i3i4-...",
    "mediaReference": "https://cdn.fynza.com/returns/...",
    "evidenceType": "PHOTO",
    "description": "Close-up of the broken zipper teeth",
    "uploadedBy": "c1c2c3c4-...",
    "createdAt": "2026-09-23T10:05:00Z"
  }
}
```

---

## 8. Customer: Cancel a Return (REST)

Cancels an open return. Only allowed while the return is `REQUESTED`.

```
POST /v1/returns/{returnId}/cancel
Authorization: Bearer <accessToken>   ← return:cancel permission
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Return cancelled successfully",
  "data": { "...ReturnResponse with status: CANCELLED..." }
}
```

**`422`** — return is not in a cancellable status.

---

## 9. Seller: Review, Approve, Reject (REST)

All seller mutation endpoints are under `/v1/seller/returns/{returnId}/...` and require the `returnId` UUID (`publicId`) in the path.

### Start review

```
POST /v1/seller/returns/{returnId}/review
Authorization: Bearer <accessToken>   ← return:review permission
```

No request body. Moves return from `REQUESTED` → `UNDER_REVIEW`.

---

### Approve

```
POST /v1/seller/returns/{returnId}/approve
Authorization: Bearer <accessToken>   ← return:approve permission
Content-Type: application/json
```

**Request body** (optional)

```json
{ "note": "Approved — please ship the item back using the label we will send." }
```

Moves return from `REQUESTED` or `UNDER_REVIEW` → `APPROVED`.

---

### Reject

```
POST /v1/seller/returns/{returnId}/reject
Authorization: Bearer <accessToken>   ← return:reject permission
Content-Type: application/json
```

**Request body** (required)

```json
{ "rejectionReason": "Item is outside the 30-day return window per our policy." }
```

| Field | Required | Notes |
|---|---|---|
| `rejectionReason` | Yes | Shown to the customer |

Moves return → `REJECTED` (terminal).

**Response shape** for all three endpoints above:

```json
{
  "success": true,
  "message": "Return approved",
  "data": { "...ReturnResponse..." }
}
```

**`422`** — return is not in a valid status for the requested transition.

---

## 10. Seller: Initiate Return Shipment (REST)

Links a shipment from the Shipping module to this return and moves status to `RETURN_SHIPPING`. Create the shipment via the Shipping API first (see [shipping.md](shipping.md)), then call this endpoint with the resulting shipment ID.

```
POST /v1/seller/returns/{returnId}/initiate-shipment
Authorization: Bearer <accessToken>   ← return:approve permission
Content-Type: application/json
```

**Request body**

```json
{
  "returnShipmentId": "sh1sh2sh3-...",
  "returnLabelReference": "https://cdn.fynza.com/labels/RET-2026-000042.pdf"
}
```

| Field | Required | Notes |
|---|---|---|
| `returnShipmentId` | Yes | `publicId` of the Shipping module shipment |
| `returnLabelReference` | No | URL of the prepaid return label to show the customer |

**Response `200`**

```json
{
  "success": true,
  "message": "Return shipment initiated",
  "data": {
    "publicId": "r1r2r3r4-...",
    "status": "RETURN_SHIPPING",
    "returnShipmentId": "sh1sh2sh3-...",
    "returnLabelReference": "https://cdn.fynza.com/labels/RET-2026-000042.pdf",
    "...rest of ReturnResponse..."
  }
}
```

---

## 11. Seller: Run Inspection (REST)

After the package is received at the warehouse, a seller or warehouse agent inspects each item and records the outcome.

### Start inspection

```
POST /v1/seller/returns/{returnId}/inspect
Authorization: Bearer <accessToken>   ← return:inspect permission
```

No request body. Moves return `RECEIVED` → `INSPECTION`.

---

### Complete inspection

Records per-item condition, result, approved/received quantities, disposition, and the overall resolution.

```
POST /v1/seller/returns/{returnId}/complete-inspection
Authorization: Bearer <accessToken>   ← return:inspect permission
Content-Type: application/json
```

**Request body**

```json
{
  "resolution": "FULL_REFUND",
  "overallNotes": "Item confirmed defective at the zipper mechanism.",
  "itemResults": [
    {
      "returnItemId": "i1i2i3i4-...",
      "condition": "DEFECTIVE",
      "result": "PASS",
      "approvedQuantity": 1,
      "receivedQuantity": 1,
      "dispositionType": "DISPOSE",
      "dispositionQuantity": 1,
      "locationId": "WAREHOUSE-A3",
      "notes": "Zipper mechanism broken — cannot be restocked"
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `resolution` | Yes | `FULL_REFUND` · `PARTIAL_REFUND` · `REJECT_RETURN` |
| `overallNotes` | No | Summary note for the audit trail |
| `itemResults` | Yes | One entry per returned item |
| `itemResults[].returnItemId` | Yes | `publicId` of the `ReturnItem` |
| `itemResults[].condition` | Yes | `InspectionCondition` |
| `itemResults[].result` | Yes | `InspectionResult` |
| `itemResults[].approvedQuantity` | No | Quantity approved for refund |
| `itemResults[].receivedQuantity` | No | Quantity actually received |
| `itemResults[].dispositionType` | No | What to do with the item |
| `itemResults[].dispositionQuantity` | No | How many units for that disposition |
| `itemResults[].locationId` | No | Warehouse bin / shelf reference |
| `itemResults[].notes` | No | Per-item inspection note |

**Resolution → status mapping**

| `resolution` | Resulting `ReturnStatus` |
|---|---|
| `FULL_REFUND` | `APPROVED_FOR_REFUND` |
| `PARTIAL_REFUND` | `PARTIALLY_APPROVED` |
| `REJECT_RETURN` | `REJECTED` |

---

## 12. Seller: Trigger Refund or Exchange (REST)

### Request refund

Calculates the refund amount server-side (sum of `unitPrice × approvedQuantity`, minus any restocking fee from the policy) and calls the Payment module. No amount field is sent from the frontend.

```
POST /v1/seller/returns/{returnId}/request-refund
Authorization: Bearer <accessToken>   ← return:resolve permission
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Refund requested",
  "data": {
    "publicId": "rf1rf2rf3-...",
    "returnId": "r1r2r3r4-...",
    "orderId": "3fa85f64-...",
    "requestedAmount": 129.99,
    "currency": "GHS",
    "status": "SUCCEEDED",
    "providerRefundId": "PSK-REF-20260923",
    "requestedAt": "2026-09-23T14:00:00Z",
    "processedAt": "2026-09-23T14:00:05Z",
    "createdAt": "2026-09-23T14:00:00Z"
  }
}
```

If `status` is `FAILED`, the refund is retryable — call `request-refund` again. The server resets the existing record and retries.

**`422`** — return is not in `APPROVED_FOR_REFUND`, `PARTIALLY_APPROVED`, or `FAILED` status.

---

### Request exchange

Requests a product exchange instead of a refund. The exchange is created with status `PENDING` and fulfilled externally (e.g. a new order is manually created for the replacement items).

```
POST /v1/seller/returns/{returnId}/exchange
Authorization: Bearer <accessToken>   ← return:resolve permission
Content-Type: application/json
```

**Request body**

```json
{
  "requestedItemsDescription": "Same item (Canvas Backpack) in Black, size M",
  "notes": "Customer prefers exchange over refund"
}
```

| Field | Required | Notes |
|---|---|---|
| `requestedItemsDescription` | Yes | What the customer wants in exchange |
| `notes` | No | Internal note |

**Response `200`**

```json
{
  "success": true,
  "message": "Exchange requested",
  "data": {
    "publicId": "ex1ex2ex3-...",
    "returnId": "r1r2r3r4-...",
    "originalOrderId": "3fa85f64-...",
    "exchangeOrderId": null,
    "status": "PENDING",
    "requestedItemsDescription": "Same item (Canvas Backpack) in Black, size M",
    "requestedBy": "s1s2s3s4-...",
    "createdAt": "2026-09-23T14:05:00Z",
    "updatedAt": "2026-09-23T14:05:00Z"
  }
}
```

**`422`** — return not in an exchange-eligible status, or exchange already exists for this return.

---

## 13. Admin: Escalate, Transit, Received, Reconcile (REST)

Admin endpoints mirror seller endpoints plus extras. All are under `/v1/admin/returns/{returnId}/...`.

### Escalate

```
POST /v1/admin/returns/{returnId}/escalate
Authorization: Bearer <accessToken>   ← return:admin.manage permission
Content-Type: application/json
```

```json
{ "reason": "Customer provided police report — requires senior review" }
```

Sets `isEscalated = true` and `escalatedAt` on the return.

---

### Mark in transit (admin override)

```
POST /v1/admin/returns/{returnId}/in-transit
Authorization: Bearer <accessToken>   ← return:admin.manage permission
```

No body. Use when the carrier webhook didn't fire or for manual overrides.

---

### Mark received

```
POST /v1/admin/returns/{returnId}/received
Authorization: Bearer <accessToken>   ← return:admin.manage permission
```

No body. Moves `IN_TRANSIT` → `RECEIVED`.

---

### Reconcile

Runs a consistency check on the return's state, refund status, shipment status, and inventory. Returns a `ReconciliationResult` describing any mismatches.

```
POST /v1/admin/returns/{returnId}/reconcile
Authorization: Bearer <accessToken>   ← return:admin.manage permission
```

No body.

**Response `200`**

```json
{
  "success": true,
  "message": "Reconciliation complete — result: MATCHED",
  "data": {
    "id": "42",
    "returnId": "r1r2r3r4-...",
    "returnStatus": "RESOLVED",
    "shipmentStatus": "DELIVERED",
    "refundStatus": "SUCCEEDED",
    "inventoryStatus": null,
    "result": "MATCHED",
    "resolvedBy": "admin-uuid-...",
    "resolvedAt": "2026-09-23T15:00:00Z",
    "createdAt": "2026-09-23T15:00:00Z"
  }
}
```

**`ReconciliationResult` values**

| Value | Meaning |
|---|---|
| `MATCHED` | All states consistent — no action needed |
| `REFUND_MISSING` | Return resolved but no refund record found |
| `SHIPMENT_MISSING` | Return in shipping status but no shipment linked |
| `INVENTORY_MISSING` | Received but inventory not updated |
| `STATUS_MISMATCH` | Return and refund statuses are inconsistent |
| `AMOUNT_MISMATCH` | Approved and processed amounts differ |
| `UNKNOWN` | Could not determine consistency |

---

## 14. Return Policy Management (REST)

Policies control return windows, eligible reasons, restocking fees, and shipping responsibility. They cascade: PRODUCT → CATEGORY → STORE → PLATFORM → system default.

### Create policy

```
POST /v1/admin/return-policies
Authorization: Bearer <accessToken>   ← return:policy.manage permission
Content-Type: application/json
```

```json
{
  "name": "Electronics 15-Day Policy",
  "scope": "CATEGORY",
  "categoryId": "cat-uuid-...",
  "returnWindowDays": 15,
  "isReturnable": true,
  "conditionRequired": true,
  "eligibleReasons": ["DEFECTIVE_PRODUCT", "WRONG_ITEM_RECEIVED", "DAMAGED_IN_TRANSIT"],
  "returnShippingResponsibility": "SELLER",
  "restockingFeePercent": 10.0,
  "refundMethod": "ORIGINAL_PAYMENT"
}
```

### Update policy

```
POST /v1/admin/return-policies/{policyId}
Authorization: Bearer <accessToken>   ← return:policy.manage permission
```

Same body as create. Replaces the policy in full.

### Toggle policy active state

```
POST /v1/admin/return-policies/{policyId}/toggle
Authorization: Bearer <accessToken>   ← return:policy.manage permission
```

No body. Flips `isActive` between `true` and `false`.

---

## 15. GraphQL Queries

All reads go through `POST /graphql` with `Authorization: Bearer <accessToken>`.

### Customer: my returns

```graphql
query MyReturns($page: Int, $size: Int) {
  myReturns(page: $page, size: $size) {
    content {
      publicId
      returnNumber
      orderId
      status
      reason
      requestedAt
      returnDeadline
      isEscalated
      items {
        productName
        quantity
        reason
        unitPrice
        resolution
      }
    }
    totalElements
    totalPages
    currentPage
    hasNextPage
  }
}
```

---

### Customer: single return

```graphql
query MyReturn($returnId: ID!) {
  myReturn(returnId: $returnId) {
    publicId
    returnNumber
    status
    reason
    customerNote
    adminNote
    rejectionReason
    returnShipmentId
    returnLabelReference
    isEscalated
    requestedAt
    approvedAt
    receivedAt
    resolvedAt
    items {
      publicId
      productName
      quantity
      condition
      approvedQuantity
      receivedQuantity
      resolution
      unitPrice
    }
  }
}
```

---

### Customer: check return eligibility

```graphql
query ReturnEligibility($orderId: ID!) {
  returnEligibility(orderId: $orderId) {
    eligible
    deadline
    remainingDays
    reason
    allowedReasons
    warnings
  }
}
```

---

### Returns for a specific order

```graphql
query OrderReturns($orderId: ID!) {
  orderReturns(orderId: $orderId) {
    publicId
    returnNumber
    status
    reason
    requestedAt
    items { productName quantity }
  }
}
```

---

### Seller: returns queue

```graphql
query SellerReturns($page: Int, $size: Int) {
  sellerReturns(page: $page, size: $size) {
    content {
      publicId
      returnNumber
      customerId
      status
      reason
      isEscalated
      requestedAt
      items { productName quantity condition }
    }
    totalElements
    totalPages
    currentPage
    hasNextPage
  }
}
```

---

### Admin: all returns

```graphql
query AdminReturns($page: Int, $size: Int) {
  adminReturns(page: $page, size: $size) {
    content {
      publicId
      returnNumber
      customerId
      sellerId
      status
      reason
      isEscalated
      escalatedAt
      requestedAt
    }
    totalElements
    totalPages
    currentPage
    hasNextPage
  }
}
```

---

### Evidence on a return

```graphql
query ReturnEvidence($returnId: ID!) {
  returnEvidence(returnId: $returnId) {
    publicId
    returnItemId
    mediaReference
    evidenceType
    description
    uploadedBy
    createdAt
  }
}
```

---

### Inspection and disposition records

```graphql
query ReturnInspections($returnId: ID!) {
  returnInspections(returnId: $returnId) {
    publicId
    returnItemId
    inspectedBy
    condition
    result
    notes
    inspectedAt
  }
  returnDispositions(returnId: $returnId) {
    publicId
    returnItemId
    type
    quantity
    locationId
    reason
    processedBy
    processedAt
  }
}
```

---

### Refund record

```graphql
query ReturnRefund($returnId: ID!) {
  returnRefund(returnId: $returnId) {
    publicId
    requestedAmount
    approvedAmount
    currency
    status
    providerRefundId
    failureReason
    requestedAt
    processedAt
  }
}
```

---

### Exchange record

```graphql
query ReturnExchange($returnId: ID!) {
  returnExchange(returnId: $returnId) {
    publicId
    status
    requestedItemsDescription
    exchangeOrderId
    processedAt
  }
}
```

---

### Audit trail

```graphql
query ReturnAuditTrail($returnId: ID!) {
  returnAuditTrail(returnId: $returnId) {
    id
    action
    previousStatus
    newStatus
    performedBy
    reason
    createdAt
  }
}
```

---

### Reconciliations

```graphql
query ReturnReconciliations($returnId: ID!) {
  returnReconciliations(returnId: $returnId) {
    id
    result
    returnStatus
    refundStatus
    shipmentStatus
    resolvedAt
    createdAt
  }
}
```

---

### Fraud signals (admin)

```graphql
query ReturnFraudSignals($returnId: ID!) {
  returnFraudSignals(returnId: $returnId) {
    id
    signalType
    description
    severity
    detectedAt
  }
}
```

---

### Platform analytics (admin)

```graphql
query ReturnAnalytics {
  returnAnalytics {
    totalReturns
    resolvedReturns
    pendingReturns
    failedReturns
    countByStatus { status count }
    countByReason { reason count }
  }
}
```

---

### Return policies (public + admin)

```graphql
query Policies($storeId: ID!, $policyId: ID!) {
  platformReturnPolicy {
    returnWindowDays
    isReturnable
    eligibleReasons
    returnShippingResponsibility
    restockingFeePercent
    refundMethod
  }
  storeReturnPolicy(storeId: $storeId) {
    name
    returnWindowDays
    isReturnable
    eligibleReasons
    restockingFeePercent
  }
  returnPolicy(policyId: $policyId) {
    publicId
    name
    scope
    returnWindowDays
    isActive
  }
}
```

---

## 16. Error Reference

### HTTP status codes

| Status | When |
|---|---|
| `400 Bad Request` | Missing required field or invalid body |
| `401 Unauthorized` | Missing or expired access token |
| `403 Forbidden` | Caller lacks the required permission |
| `404 Not Found` | Return, order, or policy UUID not found |
| `422 Unprocessable Entity` | Business rule violation — see below |

### Business rule violations (`422`)

| Message | User-facing copy |
|---|---|
| "Order must be delivered before a return can be requested" | "You can only request a return for delivered orders." |
| "Return window of N days has expired" | "The return window for this order has closed." |
| "An active return request already exists for this order" | "You already have an open return for this order." |
| "Reason X is not permitted under the current return policy" | "That return reason isn't accepted for this product." |
| "Return not in a valid state for this transition" | "This action isn't available for the current return status." |
| "Exchange already requested for this return" | "An exchange has already been submitted for this return." |
| "Refund execution failed: ..." | Show `failureReason` from `ReturnRefundResponse` if present. |

### Escalated returns

When `isEscalated = true` on a `ReturnResponse`, display a notice on your return detail screen (e.g. "This return has been flagged for review") — do not reveal fraud signal details to the customer.

---

## 17. Quick Reference

```
Customer mutations — REST  (/v1/returns/...)
────────────────────────────────────────────────────────────────────────
POST   /v1/returns                              🔒  Submit return request
POST   /v1/returns/{id}/evidence               🔒  Add photo / document
POST   /v1/returns/{id}/cancel                 🔒  Cancel open return

Seller mutations — REST  (/v1/seller/returns/...)
────────────────────────────────────────────────────────────────────────
POST   /v1/seller/returns/{id}/review          🔒  Move to UNDER_REVIEW
POST   /v1/seller/returns/{id}/approve         🔒  Approve return
POST   /v1/seller/returns/{id}/reject          🔒  Reject return
POST   /v1/seller/returns/{id}/initiate-shipment 🔒 Link shipment → RETURN_SHIPPING
POST   /v1/seller/returns/{id}/inspect         🔒  Start inspection
POST   /v1/seller/returns/{id}/complete-inspection 🔒 Record results + resolution
POST   /v1/seller/returns/{id}/request-refund  🔒  Trigger refund (server calculates amount)
POST   /v1/seller/returns/{id}/exchange        🔒  Request exchange

Admin mutations — REST  (/v1/admin/returns/...)
────────────────────────────────────────────────────────────────────────
POST   /v1/admin/returns/{id}/review           🔒  Move to UNDER_REVIEW
POST   /v1/admin/returns/{id}/approve          🔒  Approve return
POST   /v1/admin/returns/{id}/reject           🔒  Reject return
POST   /v1/admin/returns/{id}/escalate         🔒  Flag for escalation
POST   /v1/admin/returns/{id}/inspect          🔒  Start inspection
POST   /v1/admin/returns/{id}/complete-inspection 🔒 Record results + resolution
POST   /v1/admin/returns/{id}/in-transit       🔒  Mark package in transit
POST   /v1/admin/returns/{id}/received         🔒  Mark package received
POST   /v1/admin/returns/{id}/request-refund   🔒  Trigger refund
POST   /v1/admin/returns/{id}/reconcile        🔒  Run reconciliation check

Policy management — REST  (/v1/admin/return-policies/...)
────────────────────────────────────────────────────────────────────────
POST   /v1/admin/return-policies               🔒  Create policy
POST   /v1/admin/return-policies/{id}          🔒  Update policy
POST   /v1/admin/return-policies/{id}/toggle   🔒  Toggle active state

GraphQL reads — POST /graphql
────────────────────────────────────────────────────────────────────────
myReturns(page, size)                          🔒  Customer's returns (paginated)
myReturn(returnId)                             🔒  Single return (customer)
returnEligibility(orderId)                     🔒  Pre-check before showing form
orderReturns(orderId)                          🔒  Returns for an order
sellerReturns(page, size)                      🔒  Seller queue (paginated)
adminReturns(page, size)                       🔒  All returns — admin (paginated)
returnEvidence(returnId)                       🔒  Attached evidence
returnAuditTrail(returnId)                     🔒  Full audit history
returnInspections(returnId)                    🔒  Inspection records
returnDispositions(returnId)                   🔒  Disposition records
returnRefund(returnId)                         🔒  Refund record
returnExchange(returnId)                       🔒  Exchange record
returnReconciliations(returnId)                🔒  Reconciliation history
returnFraudSignals(returnId)                   🔒  Fraud signals — admin only
returnAnalytics                                🔒  Platform summary — admin only
platformReturnPolicy                               Platform default policy
storeReturnPolicy(storeId)                     🔒  Store-level policy
returnPolicy(policyId)                         🔒  Specific policy by ID

🔒 = requires Authorization: Bearer <accessToken>
```

### Integration rules

1. **Always call `returnEligibility` before showing the return form.** If `eligible` is `false`, show `reason` and hide the form.
2. **`returnId` is `publicId` (UUID), not `returnNumber`.** Never use `returnNumber` as a path parameter.
3. **Do not send a refund amount.** Call `request-refund` with no body — the server calculates it from approved quantities × unit price, minus the restocking fee from the applicable policy.
4. **Fraud signals are admin-only.** Do not expose `returnFraudSignals` or signal-derived copy to customers. When `isEscalated = true`, show a generic "under review" notice.
5. **Refund failures are retryable.** If `ReturnRefundResponse.status` is `FAILED`, show a retry button that calls `request-refund` again — the server resets and reattempts.
6. **Exchange and refund are mutually exclusive per return.** Once `request-refund` is called, calling `exchange` on the same return will be rejected (and vice versa).
7. **All timestamps are UTC.** Convert to local time in the UI.
8. **All prices are GHS.** Format as `GHS 129.99` — do not use `parseFloat` on monetary strings.
