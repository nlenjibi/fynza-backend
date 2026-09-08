# User API — Frontend Integration Guide

Base paths: `/v1/users/me` (self-service) · `/v1/admin/users` (admin)  
All requests and responses use `application/json`.

> **Auth**: Every endpoint in this guide requires `Authorization: Bearer <accessToken>`.  
> Admin endpoints additionally require the caller to hold `ROLE_ADMIN`.  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shape](#1-standard-response-shape)
2. [Reference Types](#2-reference-types)
3. [Get My Profile](#3-get-my-profile)
4. [Update My Profile](#4-update-my-profile)
5. [Deactivate My Account](#5-deactivate-my-account)
6. [Request Account Deletion](#6-request-account-deletion)
7. [Admin — Search Users](#7-admin--search-users)
8. [Admin — Get User](#8-admin--get-user)
9. [Admin — Suspend User](#9-admin--suspend-user)
10. [Admin — Activate User](#10-admin--activate-user)
11. [Admin — Disable User](#11-admin--disable-user)
12. [Error Reference](#12-error-reference)
13. [Quick Reference](#13-quick-reference)

---

## 1. Standard Response Shape

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

`data` is `null` for void endpoints (deactivate, delete-request).

---

## 2. Reference Types

### UserStatus

| Value | Meaning |
|---|---|
| `PENDING_VERIFICATION` | Registered but email not yet verified |
| `ACTIVE` | Normal, fully usable account |
| `SUSPENDED` | Temporarily blocked by an admin (optional expiry) |
| `LOCKED` | Temporarily locked after too many failed login attempts |
| `DISABLED` | Permanently disabled by an admin |
| `DELETED` | Account has been permanently removed |

### Role

| Value | Description |
|---|---|
| `CUSTOMER` | Regular buyer |
| `SELLER` | Marketplace seller |
| `ADMIN` | Platform administrator |

### UserProfileResponse (shared by all endpoints)

```json
{
  "id": "uuid",
  "email": "ada@example.com",
  "username": "ada",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "displayName": "Ada L.",
  "phone": "+233201234567",
  "avatarUrl": "https://...",
  "dateOfBirth": "1995-12-10",
  "language": "en",
  "timezone": "Africa/Accra",
  "currency": "GHS",
  "role": "CUSTOMER",
  "status": "ACTIVE",
  "emailVerified": true,
  "mfaEnabled": false,
  "createdAt": "2026-01-15T09:00:00Z",
  "updatedAt": "2026-09-08T12:00:00Z",
  "deletionRequestedAt": null
}
```

All timestamps are UTC ISO-8601. `dateOfBirth` is `yyyy-MM-dd`. Null fields are omitted from the JSON response (`@JsonInclude(NON_NULL)`).

---

## 3. Get My Profile

```
GET /v1/users/me
Authorization: Bearer <accessToken>
```

Returns the full profile for the authenticated user.

### Response `200`

```json
{
  "success": true,
  "message": "Profile retrieved",
  "data": { ...UserProfileResponse }
}
```

**When to call**: on app load / after login to hydrate the user session. Cache the result — call again after any profile update.

---

## 4. Update My Profile

```
PATCH /v1/users/me
Authorization: Bearer <accessToken>
Content-Type: application/json
```

All fields are **optional**. Only send the fields you want to change. Omitted fields are left unchanged.

### Request

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "displayName": "Ada L.",
  "phone": "+233201234567",
  "avatarUrl": "https://cdn.example.com/avatars/ada.jpg",
  "dateOfBirth": "1995-12-10",
  "language": "en",
  "timezone": "Africa/Accra",
  "currency": "GHS"
}
```

### Field constraints

| Field | Constraint |
|---|---|
| `firstName` | 1–100 characters |
| `lastName` | 1–100 characters |
| `displayName` | max 100 characters |
| `phone` | E.164 format (`+` followed by 7–15 digits), e.g. `+233201234567` |
| `avatarUrl` | max 500 characters |
| `dateOfBirth` | `yyyy-MM-dd` |
| `language` | 2–8 lowercase letters (ISO 639), e.g. `en`, `fr` |
| `timezone` | max 50 characters, IANA tz name, e.g. `Africa/Accra` |
| `currency` | exactly 3 uppercase letters (ISO 4217), e.g. `GHS` |

### Response `200`

```json
{
  "success": true,
  "message": "Profile updated",
  "data": { ...UserProfileResponse }
}
```

### Response `400` — validation failure

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "phone": "Invalid phone number format",
    "currency": "Currency must be a 3-letter ISO code"
  }
}
```

The `data` object contains field-level error messages. Display them inline next to the relevant input.

---

## 5. Deactivate My Account

```
POST /v1/users/me/deactivate
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Deactivates the user's own account and **revokes all active sessions immediately**. The account can be reactivated by contacting support or by the user logging in again (implementation-dependent).

### Request

```json
{
  "reason": "Taking a break from the platform",
  "confirm": true
}
```

| Field | Required | Notes |
|---|---|---|
| `reason` | Yes | Non-empty string |
| `confirm` | No | Explicit confirmation flag — include `true` to reduce accidental calls |

### Response `200`

```json
{
  "success": true,
  "message": "Account deactivated",
  "data": null
}
```

**After success**: clear all tokens client-side and redirect to the login screen. The user's session is immediately invalidated server-side.

---

## 6. Request Account Deletion

```
POST /v1/users/me/delete-request
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Schedules permanent deletion of the account. The account enters `PENDING_DELETION` status. **Deletion is permanent and irreversible after the 30-day grace period.**

### Request

```json
{
  "reason": "I no longer need this account",
  "confirm": true
}
```

| Field | Required | Notes |
|---|---|---|
| `reason` | Yes | Non-empty string |
| `confirm` | No | Strongly recommended — add a confirmation checkbox in your UI |

### Response `200`

```json
{
  "success": true,
  "message": "Deletion request recorded. Your account will be removed in 30 days.",
  "data": null
}
```

**After success**: log the user out and display a confirmation screen. The `deletionRequestedAt` timestamp will appear on the profile if you fetch it before deletion completes.

> **Grace period**: during the 30-day window the user may be able to cancel by contacting support. Do not show a "cancel deletion" UI unless this is explicitly enabled in the backend.

---

## 7. Admin — Search Users

```
GET /v1/admin/users
Authorization: Bearer <accessToken>   (ADMIN role required)
```

Returns a paginated list of users, optionally filtered.

### Query parameters

| Parameter | Type | Description |
|---|---|---|
| `query` | `string` | Free-text search against email, first name, last name |
| `status` | `UserStatus` | Filter by account status |
| `role` | `Role` | Filter by role |
| `emailVerified` | `boolean` | Filter by email verification state |
| `page` | `integer` | Zero-based page number (default `0`) |
| `size` | `integer` | Page size (default `20`) |
| `sort` | `string` | Sort field and direction, e.g. `createdAt,desc` |

**Example**: `GET /v1/admin/users?query=ada&status=ACTIVE&role=CUSTOMER&page=0&size=20&sort=createdAt,desc`

### Response `200`

```json
{
  "success": true,
  "message": "Users retrieved",
  "data": {
    "content": [
      { ...UserProfileResponse },
      { ...UserProfileResponse }
    ],
    "pagination": {
      "currentPage": 0,
      "pageSize": 20,
      "totalElements": 142,
      "totalPages": 8,
      "hasNextPage": true,
      "hasPreviousPage": false
    }
  }
}
```

---

## 8. Admin — Get User

```
GET /v1/admin/users/{id}
Authorization: Bearer <accessToken>   (ADMIN role required)
```

Returns the full profile for a specific user by UUID.

### Path parameter

| Parameter | Type | Description |
|---|---|---|
| `id` | `UUID` | The user's public ID |

### Response `200`

```json
{
  "success": true,
  "message": "User retrieved",
  "data": { ...UserProfileResponse }
}
```

### Response `404`

```json
{
  "success": false,
  "message": "User not found",
  "data": null
}
```

---

## 9. Admin — Suspend User

```
PATCH /v1/admin/users/{id}/suspend
Authorization: Bearer <accessToken>   (ADMIN role required)
Content-Type: application/json
```

Temporarily suspends a user account, revoking all active sessions. An optional duration can be set; once the duration elapses the account is automatically reinstated.

### Path parameter

| Parameter | Type | Description |
|---|---|---|
| `id` | `UUID` | The user's public ID |

### Request

```json
{
  "reason": "Violation of community guidelines",
  "durationDays": 7
}
```

| Field | Required | Notes |
|---|---|---|
| `reason` | Yes | Non-empty string — stored in audit log |
| `durationDays` | No | Suspension length in days. Omit or `null` for indefinite |

### Response `200`

```json
{
  "success": true,
  "message": "User suspended",
  "data": { ...UserProfileResponse }
}
```

The returned profile will show `"status": "SUSPENDED"`.

---

## 10. Admin — Activate User

```
PATCH /v1/admin/users/{id}/activate
Authorization: Bearer <accessToken>   (ADMIN role required)
```

Reactivates a suspended or disabled user account. No request body is required.

### Path parameter

| Parameter | Type | Description |
|---|---|---|
| `id` | `UUID` | The user's public ID |

### Response `200`

```json
{
  "success": true,
  "message": "User activated",
  "data": { ...UserProfileResponse }
}
```

The returned profile will show `"status": "ACTIVE"`.

---

## 11. Admin — Disable User

```
PATCH /v1/admin/users/{id}/disable?reason=<reason>
Authorization: Bearer <accessToken>   (ADMIN role required)
```

Permanently disables a user account. Unlike suspension, there is no automatic reinstatement — the account must be manually activated by an admin.

### Path parameter

| Parameter | Type | Description |
|---|---|---|
| `id` | `UUID` | The user's public ID |

### Query parameter

| Parameter | Required | Description |
|---|---|---|
| `reason` | No | Human-readable reason, stored in audit log |

**Example**: `PATCH /v1/admin/users/abc123.../disable?reason=Fraud+detected`

### Response `200`

```json
{
  "success": true,
  "message": "User disabled",
  "data": { ...UserProfileResponse }
}
```

The returned profile will show `"status": "DISABLED"`.

---

## 12. Error Reference

| HTTP Status | Meaning | Common causes |
|---|---|---|
| `400` | Bad request | Validation failure (missing `reason`, invalid field format) |
| `401` | Unauthenticated | Missing, expired, or invalid access token |
| `403` | Forbidden | Authenticated but insufficient role (e.g. `CUSTOMER` calling admin endpoint) |
| `404` | Not found | User UUID does not exist |

All error bodies follow the standard shape. Display `message` directly to the user — it is already human-readable. For `400` validation errors, `data` may contain a field-error map (see [Update My Profile](#4-update-my-profile)).

**On `401`**: attempt a token refresh via `POST /v1/auth/refresh-token`. If the refresh also fails, redirect to login.

---

## 13. Quick Reference

```
Self-service (any authenticated user)
─────────────────────────────────────
GET    /v1/users/me                        🔒  Get my profile
PATCH  /v1/users/me                        🔒  Update my profile
POST   /v1/users/me/deactivate             🔒  Deactivate my account
POST   /v1/users/me/delete-request         🔒  Request permanent deletion

Admin — requires ROLE_ADMIN
────────────────────────────────────────────
GET    /v1/admin/users                     🔒  Search / list users
GET    /v1/admin/users/{id}                🔒  Get user by ID
PATCH  /v1/admin/users/{id}/suspend        🔒  Suspend user
PATCH  /v1/admin/users/{id}/activate       🔒  Activate user
PATCH  /v1/admin/users/{id}/disable        🔒  Disable user

🔒 = requires Authorization: Bearer <accessToken>
```
