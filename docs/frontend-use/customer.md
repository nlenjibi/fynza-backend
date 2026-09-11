# Customer Management API — Frontend Integration Guide

Base paths: `/v1/customers/me` (self-service mutations) · `/v1/admin/customers` (admin mutations) · `POST /graphql` (all reads)  
All REST requests and responses use `application/json`.

> **Auth**: Every endpoint requires `Authorization: Bearer <accessToken>`.  
> The API is split by design — **REST handles mutations** (create/update/delete), **GraphQL handles reads**.  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [How Customer Profiles Are Created](#3-how-customer-profiles-are-created)
4. [Self-Service — Read My Profile (GraphQL)](#4-self-service--read-my-profile-graphql)
5. [Self-Service — Update My Profile (REST)](#5-self-service--update-my-profile-rest)
6. [Self-Service — Preferences (GraphQL + REST)](#6-self-service--preferences-graphql--rest)
7. [Self-Service — Addresses (GraphQL + REST)](#7-self-service--addresses-graphql--rest)
8. [Admin — Read Customers (GraphQL)](#8-admin--read-customers-graphql)
9. [Admin — Lifecycle Actions (REST)](#9-admin--lifecycle-actions-rest)
10. [Status Transition Rules](#10-status-transition-rules)
11. [Permission Reference](#11-permission-reference)
12. [Error Reference](#12-error-reference)
13. [Quick Reference](#13-quick-reference)

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

`data` is `null` for void endpoints (e.g. `DELETE`). `message` is always safe to display to the user.

### GraphQL envelope

```json
{
  "data": {
    "meCustomer": { ... }
  }
}
```

On error:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Customer profile not found",
      "extensions": { "status": 404 }
    }
  ]
}
```

Check `errors[0].message` and `errors[0].extensions.status` for GraphQL error handling.

---

## 2. Reference Types

### `CustomerStatus`

| Value | Meaning |
|---|---|
| `PROSPECT` | Account registered but not yet fully active (transitional) |
| `ACTIVE` | Normal, fully usable account |
| `INACTIVE` | Self-deactivated or idle account |
| `SUSPENDED` | Temporarily restricted by an admin; optional expiry |
| `BLOCKED` | Permanently restricted by an admin — no self-recovery |
| `DELETED` | Account has been permanently removed |

### `CustomerAddressType`

| Value | Meaning |
|---|---|
| `HOME` | Residential address |
| `OFFICE` | Work address |
| `OTHER` | Any other address type |

### `CustomerProfile` (REST response / GraphQL type)

```typescript
interface CustomerProfile {
  id:             string;    // UUID — customer's public ID
  customerNumber: string;    // e.g. "CUS-000001"
  status:         CustomerStatus;
  firstName:      string;
  lastName:       string;
  email:          string;
  phone:          string | null;
  createdAt:      string;    // ISO-8601 UTC
  updatedAt:      string;    // ISO-8601 UTC
}
```

### `CustomerAddress`

```typescript
interface CustomerAddress {
  id:            string;               // UUID
  recipientName: string;
  phoneNumber:   string | null;
  addressLine1:  string;
  addressLine2:  string | null;
  city:          string;
  region:        string | null;
  country:       string;
  postalCode:    string | null;
  latitude:      number | null;
  longitude:     number | null;
  addressType:   CustomerAddressType;
  isDefault:     boolean;
  createdAt:     string;
  updatedAt:     string;
}
```

### `CustomerPreferences`

```typescript
interface CustomerPreferences {
  id:                 string;    // UUID
  language:           string;    // e.g. "en"
  currency:           string;    // e.g. "GHS"
  marketingOptIn:     boolean;
  emailNotifications: boolean;
  smsNotifications:   boolean;
  pushNotifications:  boolean;
  updatedAt:          string;
}
```

### `CustomerStatusHistory`

```typescript
interface CustomerStatusHistory {
  id:             string;
  previousStatus: CustomerStatus | null;
  newStatus:      CustomerStatus;
  reason:         string | null;
  changedBy:      string | null;   // UUID of admin who made the change
  createdAt:      string;
  expiresAt:      string | null;   // set when suspension has a fixed end date
}
```

---

## 3. How Customer Profiles Are Created

You do **not** create a customer profile manually. It is provisioned automatically by the backend after a user registers with `role: CUSTOMER`. The flow is:

1. Call `POST /v1/auth/register` (see [auth.md](auth.md))
2. The backend publishes a `UserRegisteredEvent` after the user row is saved
3. `CustomerProvisioningListener` picks it up asynchronously and creates the `Customer` record with `status: ACTIVE` and default preferences

**What this means for your UI**: after registration + email verification, the customer profile is ready. A small async delay (milliseconds) exists between registration and profile availability. If you query `meCustomer` immediately after login it will always be present.

---

## 4. Self-Service — Read My Profile (GraphQL)

All self-service reads go through GraphQL at `POST /graphql`.

### Get my customer profile

Requires the `customer.read.own` permission (automatically granted to `CUSTOMER` role).

```graphql
query MeCustomer {
  meCustomer {
    id
    customerNumber
    status
    firstName
    lastName
    email
    phone
    createdAt
    updatedAt
  }
}
```

**Response**

```json
{
  "data": {
    "meCustomer": {
      "id": "a1b2c3d4-...",
      "customerNumber": "CUS-000042",
      "status": "ACTIVE",
      "firstName": "Ada",
      "lastName": "Lovelace",
      "email": "ada@example.com",
      "phone": null,
      "createdAt": "2026-01-15T09:00:00Z",
      "updatedAt": "2026-09-08T12:00:00Z"
    }
  }
}
```

Returns `null` (not an error) if the user has no customer record (e.g. a SELLER or ADMIN).

---

## 5. Self-Service — Update My Profile (REST)

### Update my customer profile

```
PATCH /v1/customers/me
Authorization: Bearer <accessToken>
Content-Type: application/json
```

All fields are **optional** — only send fields you want to change. Omitted fields are left unchanged.

**Request**

```json
{
  "firstName": "Ada",
  "lastName":  "Lovelace",
  "phone":     "+233201234567"
}
```

**Field constraints**

| Field | Constraint |
|---|---|
| `firstName` | max 100 characters |
| `lastName` | max 100 characters |
| `phone` | max 30 characters |

**Response `200`**

```json
{
  "success": true,
  "message": "Customer profile updated",
  "data": {
    "id":             "a1b2c3d4-...",
    "customerNumber": "CUS-000042",
    "status":         "ACTIVE",
    "firstName":      "Ada",
    "lastName":       "Lovelace",
    "email":          "ada@example.com",
    "phone":          "+233201234567",
    "createdAt":      "2026-01-15T09:00:00Z",
    "updatedAt":      "2026-09-09T08:30:00Z"
  }
}
```

---

## 6. Self-Service — Preferences (GraphQL + REST)

### Read my preferences (GraphQL)

Requires `customer.read.own` permission.

```graphql
query MyCustomerPreferences {
  myCustomerPreferences {
    id
    language
    currency
    marketingOptIn
    emailNotifications
    smsNotifications
    pushNotifications
    updatedAt
  }
}
```

Preferences are auto-created with defaults when the customer profile is provisioned. This query never returns `null` for an active customer.

---

### Update my preferences (REST)

```
PATCH /v1/customers/me/preferences
Authorization: Bearer <accessToken>
Content-Type: application/json
```

All fields are optional.

**Request**

```json
{
  "language":           "en",
  "currency":           "GHS",
  "marketingOptIn":     false,
  "emailNotifications": true,
  "smsNotifications":   false,
  "pushNotifications":  true
}
```

**Field constraints**

| Field | Constraint |
|---|---|
| `language` | 2–10 characters (ISO 639, e.g. `en`, `fr`) |
| `currency` | exactly 3 characters (ISO 4217, e.g. `GHS`, `USD`) |
| `marketingOptIn` | boolean |
| `emailNotifications` | boolean |
| `smsNotifications` | boolean |
| `pushNotifications` | boolean |

**Response `200`**

```json
{
  "success": true,
  "message": "Preferences updated",
  "data": {
    "id":                 "b2c3d4e5-...",
    "language":           "en",
    "currency":           "GHS",
    "marketingOptIn":     false,
    "emailNotifications": true,
    "smsNotifications":   false,
    "pushNotifications":  true,
    "updatedAt":          "2026-09-09T08:30:00Z"
  }
}
```

---

## 7. Self-Service — Addresses (GraphQL + REST)

### Read my addresses (GraphQL)

Requires `address.read.own` permission.

```graphql
query MyCustomerAddresses {
  myCustomerAddresses {
    id
    recipientName
    phoneNumber
    addressLine1
    addressLine2
    city
    region
    country
    postalCode
    latitude
    longitude
    addressType
    isDefault
    createdAt
    updatedAt
  }
}
```

Returns an empty array `[]` if no addresses exist. The default address (if any) will have `isDefault: true` — there is at most one per customer.

---

### Add an address (REST)

```
POST /v1/customers/me/addresses
Authorization: Bearer <accessToken>
Content-Type: application/json
```

A customer may have a **maximum of 10 active addresses**. Adding an 11th returns `400`.

**Request**

```json
{
  "recipientName": "Ada Lovelace",
  "phoneNumber":   "+233201234567",
  "addressLine1":  "12 Cantonments Road",
  "addressLine2":  "Flat 3B",
  "city":          "Accra",
  "region":        "Greater Accra",
  "country":       "Ghana",
  "postalCode":    "GA-123",
  "latitude":      5.6037,
  "longitude":     -0.1870,
  "addressType":   "HOME",
  "isDefault":     true
}
```

**Required fields**: `recipientName`, `addressLine1`, `city`, `country`.

**Field constraints**

| Field | Constraint |
|---|---|
| `recipientName` | Required · max 150 characters |
| `phoneNumber` | max 30 characters |
| `addressLine1` | Required · max 255 characters |
| `addressLine2` | max 255 characters |
| `city` | Required · max 100 characters |
| `region` | max 100 characters |
| `country` | Required · max 100 characters |
| `postalCode` | max 20 characters |
| `latitude` / `longitude` | decimal number (optional) |
| `addressType` | `HOME` \| `OFFICE` \| `OTHER` (defaults to `OTHER` if omitted) |
| `isDefault` | boolean — if `true`, the previous default is cleared automatically |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Address added",
  "data": {
    "id":            "c3d4e5f6-...",
    "recipientName": "Ada Lovelace",
    "addressLine1":  "12 Cantonments Road",
    "city":          "Accra",
    "country":       "Ghana",
    "addressType":   "HOME",
    "isDefault":     true,
    "createdAt":     "2026-09-09T08:35:00Z",
    "updatedAt":     "2026-09-09T08:35:00Z"
  }
}
```

---

### Update an address (REST)

```
PATCH /v1/customers/me/addresses/{addressId}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Path parameter**

| Parameter | Type | Description |
|---|---|---|
| `addressId` | UUID | The address's public ID |

Send only the fields you want to change. Same field constraints as add.

**Response `200`**

```json
{
  "success": true,
  "message": "Address updated",
  "data": { ...CustomerAddress }
}
```

**`403 Forbidden`** if the address belongs to a different customer.

---

### Delete an address (REST)

```
DELETE /v1/customers/me/addresses/{addressId}
Authorization: Bearer <accessToken>
```

Soft-deletes the address. If the deleted address was the default, no other address is automatically promoted — the customer will have no default until they designate one.

**Response `200`**

```json
{
  "success": true,
  "message": "Address deleted",
  "data": null
}
```

**`403 Forbidden`** if the address belongs to a different customer.  
**`404 Not Found`** if the address does not exist.

---

### Set default address (REST)

```
POST /v1/customers/me/addresses/{addressId}/default
Authorization: Bearer <accessToken>
```

Marks the given address as the default, clearing the previous default (if any). No request body needed.

**Response `200`**

```json
{
  "success": true,
  "message": "Default address set",
  "data": { ...CustomerAddress (isDefault: true) }
}
```

---

## 8. Admin — Read Customers (GraphQL)

All admin reads go through GraphQL. The caller must hold the `customer.read` permission.

### Get a customer by ID

```graphql
query GetCustomer($id: ID!) {
  customer(id: $id) {
    id
    customerNumber
    status
    firstName
    lastName
    email
    phone
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

### Search / list customers

```graphql
query ListCustomers($pagination: PageInput, $filter: CustomerFilterInput) {
  customers(pagination: $pagination, filter: $filter) {
    content {
      id
      customerNumber
      status
      firstName
      lastName
      email
      createdAt
    }
    pageInfo {
      currentPage
      pageSize
      totalElements
      totalPages
      hasNextPage
      hasPreviousPage
    }
  }
}
```

**Variables**

```json
{
  "pagination": { "page": 0, "size": 20 },
  "filter": {
    "query":          "ada",
    "status":         "ACTIVE",
    "customerNumber": "CUS-000042"
  }
}
```

All filter fields are optional. `query` matches against name and email. Omit `filter` entirely to return all customers.

---

### Get customer status history

Returns the full transition audit trail for a customer, newest first.

```graphql
query CustomerStatusHistory($id: ID!) {
  customerStatusHistory(id: $id) {
    id
    previousStatus
    newStatus
    reason
    changedBy
    createdAt
    expiresAt
  }
}
```

**Variables**

```json
{ "id": "a1b2c3d4-..." }
```

---

## 9. Admin — Lifecycle Actions (REST)

All admin mutations require the caller to hold the corresponding permission (see [§11](#11-permission-reference)).

### Suspend a customer

```
PATCH /v1/admin/customers/{id}/suspend
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Moves the customer from `ACTIVE` (or `INACTIVE`) to `SUSPENDED`. An optional `expiresAt` time triggers automatic reactivation by a background job.

**Path parameter**: `id` — customer's public UUID.

**Request**

```json
{
  "reason":    "Suspicious payment activity",
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
  "message": "Customer suspended",
  "data": { ...CustomerProfile (status: "SUSPENDED") }
}
```

**`422 Unprocessable Entity`** if the transition is invalid (e.g. suspending an already-BLOCKED customer).

---

### Activate a customer

```
PATCH /v1/admin/customers/{id}/activate
Authorization: Bearer <accessToken>
```

Moves the customer to `ACTIVE`. Valid from `SUSPENDED` or `INACTIVE`. No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Customer activated",
  "data": { ...CustomerProfile (status: "ACTIVE") }
}
```

---

### Block a customer

```
PATCH /v1/admin/customers/{id}/block
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Moves the customer to `BLOCKED`. This is a stronger restriction than suspension — blocked customers cannot be reactivated via the suspend/activate flow. Only `DELETED` is reachable from `BLOCKED`.

**Request**

```json
{
  "reason": "Repeated policy violations — account permanently restricted"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Customer blocked",
  "data": { ...CustomerProfile (status: "BLOCKED") }
}
```

---

## 10. Status Transition Rules

Only the following transitions are permitted. Any other transition returns `422 Unprocessable Entity`.

```
PROSPECT  ──→  ACTIVE
ACTIVE    ──→  INACTIVE | SUSPENDED | BLOCKED | DELETED
INACTIVE  ──→  ACTIVE | DELETED
SUSPENDED ──→  ACTIVE | BLOCKED | DELETED
BLOCKED   ──→  DELETED
DELETED   ──→  (none — terminal state)
```

**What this means for your UI**:
- A `BLOCKED` customer cannot be directly reactivated — show a different UI path if you need to handle this case.
- Only show the "Activate" button when `status` is `SUSPENDED` or `INACTIVE`.
- Only show the "Suspend" button when `status` is `ACTIVE`.
- Only show the "Block" button when `status` is `ACTIVE`, `INACTIVE`, or `SUSPENDED`.

---

## 11. Permission Reference

| Permission | Who holds it | Required for |
|---|---|---|
| `customer.read.own` | `CUSTOMER` role | Read own profile, preferences |
| `address.read.own` | `CUSTOMER` role | Read own addresses |
| `customer.update.own` | `CUSTOMER` role | Update own profile and preferences |
| `address.create.own` | `CUSTOMER` role | Add own address |
| `address.update.own` | `CUSTOMER` role | Update own address, set default |
| `address.delete.own` | `CUSTOMER` role | Delete own address |
| `customer.read` | `ADMIN` | Admin GraphQL reads (search, get, history) |
| `customer.suspend` | `ADMIN` | `PATCH .../suspend` |
| `customer.activate` | `ADMIN` | `PATCH .../activate` |
| `customer.block` | `ADMIN` | `PATCH .../block` |
| `customer.manage` | `ADMIN` | Any of the above admin mutations |

`customer.manage` is a super-permission — a user with it can perform all admin mutations.

---

## 12. Error Reference

| HTTP / GraphQL status | When |
|---|---|
| `400 Bad Request` | Validation failure (missing required field, field too long, max-10-address limit reached) |
| `401 Unauthorized` | Missing or expired access token |
| `403 Forbidden` | Authenticated but missing required permission, or attempting to access another customer's address |
| `404 Not Found` | Customer or address UUID does not exist |
| `422 Unprocessable Entity` | Invalid status transition (e.g. suspending a BLOCKED customer) |

For `400` validation errors, the `data` object may contain a field-error map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "recipientName": "must not be blank",
    "country":       "must not be blank"
  }
}
```

**On `401`**: attempt a token refresh (`POST /v1/auth/refresh-token`). On failure, redirect to login.  
**On `403` for address endpoints**: the address UUID does not belong to the authenticated customer — do not expose the address ID in client-visible URLs without verifying ownership first.  
**On `422`**: read `message` and reflect the invalid transition in your UI by disabling the action button based on the current `status` (see [§10](#10-status-transition-rules)).

---

## 13. Quick Reference

```
Self-service mutations (requires CUSTOMER role + specific permission)
─────────────────────────────────────────────────────────────────────
PATCH   /v1/customers/me                               🔒  Update profile
PATCH   /v1/customers/me/preferences                   🔒  Update preferences
POST    /v1/customers/me/addresses                     🔒  Add address
PATCH   /v1/customers/me/addresses/{addressId}         🔒  Update address
DELETE  /v1/customers/me/addresses/{addressId}         🔒  Delete address
POST    /v1/customers/me/addresses/{addressId}/default 🔒  Set default address

Self-service reads → GraphQL (requires customer.read.own / address.read.own)
─────────────────────────────────────────────────────────────────────
query meCustomer              Get my customer profile
query myCustomerPreferences   Get my preferences
query myCustomerAddresses     Get my addresses

Admin mutations (requires customer.suspend / .activate / .block)
──────────────────────────────────────────────────────────────────
PATCH   /v1/admin/customers/{id}/suspend               🔒  Suspend customer
PATCH   /v1/admin/customers/{id}/activate              🔒  Reactivate customer
PATCH   /v1/admin/customers/{id}/block                 🔒  Block customer

Admin reads → GraphQL (requires customer.read)
──────────────────────────────────────────────────────────────────
query customer(id)                        Get customer by ID
query customers(pagination, filter)       Search / list customers
query customerStatusHistory(id)           Get status audit trail

🔒 = requires Authorization: Bearer <accessToken>
```
