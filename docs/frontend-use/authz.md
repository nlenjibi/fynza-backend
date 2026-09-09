# Authorization & Roles API — Frontend Integration Guide

Base path: `/v1/admin/authz`  
All requests and responses use `application/json`.  
**All endpoints require the `role.manage` permission.** Requests without it receive `403 Forbidden`.

---

## Table of Contents

1. [Standard Response Shape](#1-standard-response-shape)
2. [Reference Types](#2-reference-types)
3. [Permission Codes](#3-permission-codes)
4. [Role Management](#4-role-management)
5. [Role ↔ Permission Management](#5-role--permission-management)
6. [Permission Catalogue](#6-permission-catalogue)
7. [User-Role Assignment](#7-user-role-assignment)
8. [Error Reference](#8-error-reference)
9. [Quick Reference](#9-quick-reference)

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

Paginated responses wrap the collection:

```json
{
  "success": true,
  "message": "...",
  "data": {
    "content": [ ... ],
    "pagination": {
      "currentPage": 0,
      "pageSize": 20,
      "totalElements": 6,
      "totalPages": 1,
      "hasNextPage": false
    }
  }
}
```

---

## 2. Reference Types

### `RoleDto`

```typescript
interface RoleDto {
  id:            string;       // UUID
  code:          string;       // e.g. "ADMIN", "SELLER"
  displayName:   string | null;
  description:   string | null;
  systemDefined: boolean;      // true → cannot be deleted
  active:        boolean;
  permissions:   string[];     // list of permission codes granted to this role
}
```

### `PermissionDto`

```typescript
interface PermissionDto {
  id:          string;   // UUID
  code:        string;   // e.g. "product.create"
  resource:    string;   // e.g. "product"
  action:      string;   // e.g. "create"
  description: string | null;
}
```

### `UserRoleDto`

```typescript
interface UserRoleDto {
  id:              string;          // UUID
  roleCode:        string;          // e.g. "SELLER"
  roleDisplayName: string | null;
  scopeType:       "GLOBAL" | "STORE" | "OWN";
  scopeId:         string | null;   // UUID — null when scopeType is GLOBAL
  assignedBy:      string;          // UUID of the admin who assigned it
  assignedAt:      string;          // ISO-8601 UTC
  expiresAt:       string | null;   // ISO-8601 UTC — null means permanent
  active:          boolean;
}
```

### `ScopeType`

| Value | Meaning |
|---|---|
| `GLOBAL` | Permission applies platform-wide |
| `STORE` | Permission applies to one specific store (`scopeId` = store UUID) |
| `OWN` | Permission applies only to the user's own resources |

---

## 3. Permission Codes

All permission codes follow the `resource.action` pattern and are seeded in the database. The full list is returned by `GET /v1/admin/authz/permissions`.

| Code | Resource | Action |
|---|---|---|
| `product.create` | product | create |
| `product.read` | product | read |
| `product.update` | product | update |
| `product.delete` | product | delete |
| `product.approve` | product | approve |
| `user.view` | user | view |
| `user.suspend` | user | suspend |
| `user.manage` | user | manage |
| `store.view` | store | view |
| `store.manage` | store | manage |
| `order.view` | order | view |
| `order.manage` | order | manage |
| `order.refund` | order | refund |
| `payment.view` | payment | view |
| `payment.refund` | payment | refund |
| `role.assign` | role | assign |
| `role.revoke` | role | revoke |
| `role.manage` | role | manage |
| `permission.grant` | permission | grant |
| `permission.revoke` | permission | revoke |
| `report.view` | report | view |
| `report.export` | report | export |
| `category.manage` | category | manage |
| `inventory.view` | inventory | view |
| `inventory.manage` | inventory | manage |

**Default grants by role:**

| Role | Permissions |
|---|---|
| `ADMIN` | All (`role.manage`, `user.manage`, `product.approve`, ...) |
| `SELLER` | `product.create`, `product.read`, `product.update`, `product.delete`, `store.manage`, `order.view`, `inventory.manage`, `inventory.view` |
| `CUSTOMER` | `product.read`, `order.view` |
| `SELLER_MANAGER` | All SELLER permissions + `product.approve`, `store.view`, `user.view` |
| `SUPPORT_AGENT` | `user.view`, `order.view`, `order.manage`, `payment.view` |
| `FINANCE_STAFF` | `order.view`, `payment.view`, `payment.refund`, `order.refund`, `report.view`, `report.export` |

---

## 4. Role Management

### List roles

```
GET /v1/admin/authz/roles?page=0&size=20
```

**Response `data`:** paginated `RoleDto[]`

---

### Get role

```
GET /v1/admin/authz/roles/{id}
```

**Path:** `id` — role UUID.

**Response `data`:** `RoleDto`

---

### Create role

```
POST /v1/admin/authz/roles
```

**Request body:**

```json
{
  "code":        "CONTENT_MODERATOR",
  "displayName": "Content Moderator",
  "description": "Can review and moderate product listings"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `code` | `string` | Yes | Uppercase letters, digits, underscores only. Must be unique. |
| `displayName` | `string` | No | |
| `description` | `string` | No | |

**Response `201 Created`, `data`:** `RoleDto`

---

### Update role

```
PATCH /v1/admin/authz/roles/{id}
```

All fields are optional — only supplied fields are updated.

**Request body:**

```json
{
  "displayName": "Senior Content Moderator",
  "description": "Updated description",
  "active":      false
}
```

**Response `data`:** `RoleDto`

> Deactivating a role (`active: false`) does not immediately evict existing user sessions. Principal caches expire on their normal TTL.

---

### Delete role

```
DELETE /v1/admin/authz/roles/{id}
```

**Fails with `400 Bad Request`** if the role is system-defined (`systemDefined: true`). System roles (`ADMIN`, `SELLER`, `CUSTOMER`, `SELLER_MANAGER`, `SUPPORT_AGENT`, `FINANCE_STAFF`) cannot be deleted.

**Response `data`:** `null`

---

## 5. Role ↔ Permission Management

### Grant permission to role

```
POST /v1/admin/authz/roles/{id}/permissions
```

Idempotent — re-granting an already-held permission is a no-op.

**Request body:**

```json
{
  "permissionCode": "product.approve"
}
```

**Response `data`:** full `RoleDto` with updated `permissions[]`

---

### Revoke permission from role

```
DELETE /v1/admin/authz/roles/{id}/permissions
```

Idempotent — revoking a permission not held is a no-op.

**Request body:**

```json
{
  "permissionCode": "product.approve"
}
```

**Response `data`:** `null`

> After revoking a permission, affected users' principal caches expire on the next request that triggers re-authentication (token refresh or login). No immediate forced eviction occurs at this level.

---

## 6. Permission Catalogue

### List all permissions

```
GET /v1/admin/authz/permissions
```

Returns every available permission code in the system.

**Response `data`:** `PermissionDto[]`

---

## 7. User-Role Assignment

### Assign role to user

```
POST /v1/admin/authz/users/{userId}/roles
```

**Path:** `userId` — user UUID.

**Request body:**

```json
{
  "roleCode":  "SELLER_MANAGER",
  "scopeType": "GLOBAL",
  "scopeId":   null,
  "expiresAt": null
}
```

To grant a temporary role:

```json
{
  "roleCode":  "SUPPORT_AGENT",
  "scopeType": "GLOBAL",
  "scopeId":   null,
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

To grant a store-scoped role:

```json
{
  "roleCode":  "SELLER",
  "scopeType": "STORE",
  "scopeId":   "b3a4c5d6-e7f8-...",
  "expiresAt": null
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `roleCode` | `string` | Yes | Must match an existing role code |
| `scopeType` | `"GLOBAL"\|"STORE"\|"OWN"` | Yes | |
| `scopeId` | `string` (UUID) | No | Required when `scopeType` is `STORE` |
| `expiresAt` | `string` (ISO-8601 UTC) | No | Null = permanent; set for temporary access |

**Response `201 Created`, `data`:** `UserRoleDto`

> The user's principal is evicted from cache immediately upon assignment, so the new permissions take effect on their next authenticated request.

---

### Revoke role from user

```
DELETE /v1/admin/authz/users/{userId}/roles
```

Marks the active role assignment as inactive. Idempotent — no-op if the assignment does not exist.

**Request body:**

```json
{
  "roleCode":  "SELLER_MANAGER",
  "scopeType": "GLOBAL"
}
```

| Field | Type | Required |
|---|---|---|
| `roleCode` | `string` | Yes |
| `scopeType` | `"GLOBAL"\|"STORE"\|"OWN"` | Yes |

**Response `data`:** `null`

> The user's principal is evicted from cache immediately, so the revocation takes effect on their next authenticated request.

---

### Get user's roles

```
GET /v1/admin/authz/users/{userId}/roles
```

Returns only **active** role assignments.

**Response `data`:** `UserRoleDto[]`

---

## 8. Error Reference

| HTTP Status | When |
|---|---|
| `400 Bad Request` | Validation failure; deleting a system-defined role |
| `401 Unauthorized` | Missing or expired access token |
| `403 Forbidden` | Authenticated but missing `role.manage` permission |
| `404 Not Found` | Role, permission, or user-role assignment not found |
| `409 Conflict` | Role code already exists |

---

## 9. Quick Reference

| Method | Path | Description | Permission |
|---|---|---|---|
| `GET` | `/v1/admin/authz/roles` | List roles (paginated) | `role.manage` |
| `POST` | `/v1/admin/authz/roles` | Create role | `role.manage` |
| `GET` | `/v1/admin/authz/roles/{id}` | Get role by ID | `role.manage` |
| `PATCH` | `/v1/admin/authz/roles/{id}` | Update role | `role.manage` |
| `DELETE` | `/v1/admin/authz/roles/{id}` | Delete role | `role.manage` |
| `POST` | `/v1/admin/authz/roles/{id}/permissions` | Grant permission to role | `role.manage` |
| `DELETE` | `/v1/admin/authz/roles/{id}/permissions` | Revoke permission from role | `role.manage` |
| `GET` | `/v1/admin/authz/permissions` | List all permission codes | `role.manage` |
| `POST` | `/v1/admin/authz/users/{userId}/roles` | Assign role to user | `role.manage` |
| `DELETE` | `/v1/admin/authz/users/{userId}/roles` | Revoke role from user | `role.manage` |
| `GET` | `/v1/admin/authz/users/{userId}/roles` | Get user's active roles | `role.manage` |
