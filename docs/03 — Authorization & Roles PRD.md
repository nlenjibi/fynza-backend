# 03 — Authorization & Roles PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Authorization & Roles  
**Module ID:** `AUTHZ-03`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Authentication, User Management

---

# 1. Overview

The Authorization & Roles module controls what authenticated users are allowed to do within Fynza.

Authentication establishes:

> **Who are you?**

User Management establishes:

> **What user information do we maintain?**

Authorization establishes:

> **What are you allowed to do?**

The module provides:

- Roles
- Permissions
- Role assignment
- Permission evaluation
- Resource ownership
- Access policies
- Administrative access control
- Organization/store-level access
- Permission inheritance where required
- Authorization auditing

---

# 2. Goals

## 2.1 Primary Goals

1. Centralize authorization rules.
2. Prevent unauthorized access to platform resources.
3. Support customers, sellers, staff, and administrators.
4. Support fine-grained permissions.
5. Support resource ownership.
6. Support store/business-level permissions.
7. Make authorization reusable across modules.
8. Provide auditable permission changes.
9. Prevent privilege escalation.
10. Keep business modules independent from authorization implementation details.

---

# 3. Non-Goals

This module does not own:

- Authentication.
- Passwords.
- Login.
- Sessions.
- User profiles.
- Products.
- Orders.
- Payments.
- Inventory.
- Seller business logic.

Business modules define their resources and operations while Authorization determines whether the current actor may perform the requested operation.

---

# 4. Authorization Model

The recommended Fynza model is:

```text
User
 │
 ├── Roles
 │     │
 │     └── Permissions
 │
 └── Direct Permissions (optional)
```

For resource-specific authorization:

```text
User
 │
 ▼
Role
 │
 ▼
Permission
 │
 ▼
Resource
 │
 ▼
Ownership / Scope / Policy
 │
 ▼
ALLOW or DENY
```

---

# 5. Authorization Layers

Fynza should use multiple authorization layers rather than relying only on roles.

```text
Layer 1 — Authentication
        ↓
Layer 2 — Role
        ↓
Layer 3 — Permission
        ↓
Layer 4 — Resource Ownership
        ↓
Layer 5 — Scope
        ↓
Layer 6 — Business Policy
```

Example:

```text
Seller
  ↓
product.update
  ↓
Product #123
  ↓
Does product belong to seller's store?
  ↓
YES → Allow
NO  → Deny
```

---

# 6. Functional Requirements

## AUTHZ-FR-001 — Role Management

The system shall support predefined and configurable roles.

Example:

```text
CUSTOMER
SELLER
SELLER_STAFF
SUPPORT_AGENT
FINANCE_STAFF
OPERATIONS_STAFF
ADMIN
SUPER_ADMIN
```

---

# 7. AUTHZ-FR-002 — Permission Management

Permissions represent individual capabilities.

Recommended naming convention:

```text
resource.action
```

Examples:

```text
product.read
product.create
product.update
product.delete

order.read
order.create
order.update
order.cancel

user.read
user.update
user.suspend

payment.read
payment.refund
```

---

# 8. AUTHZ-FR-003 — Role-Permission Mapping

Roles contain permissions.

Example:

```text
CUSTOMER
├── product.read
├── cart.read
├── cart.manage
├── order.create
└── order.read.own
```

```text
SELLER
├── product.read
├── product.create
├── product.update
├── product.delete
├── inventory.read
├── inventory.manage
└── order.read.store
```

```text
ADMIN
├── user.read
├── user.manage
├── product.manage
├── order.manage
├── payment.read
└── system.read
```

---

# 9. AUTHZ-FR-004 — Role Assignment

Authorized administrators may assign roles to users.

```text
User
 ↓
Assign Role
 ↓
Validate Administrator Permission
 ↓
Create Role Assignment
 ↓
Audit
```

Role assignments must be auditable.

---

# 10. AUTHZ-FR-005 — Role Revocation

Authorized administrators may remove roles.

```text
User
 ↓
Role
 ↓
Revoke
 ↓
Permissions Removed
 ↓
Audit Event
```

The system must prevent an administrator from accidentally removing the only required access needed to manage the platform unless the administrator has appropriate privileges.

---

# 11. AUTHZ-FR-006 — Permission Evaluation

The system must provide a centralized permission-checking mechanism.

Conceptually:

```text
authorize(user, permission, resource)
```

Example:

```text
authorize(
    user,
    "product.update",
    product
)
```

Result:

```text
ALLOW
```

or:

```text
DENY
```

---

# 12. AUTHZ-FR-007 — Resource Ownership

Permissions alone are not sufficient.

A user may have:

```text
product.update
```

but should only update products belonging to their authorized store.

Example:

```text
Seller A
 └── Store A
      └── Product A

Seller B
 └── Store B
      └── Product B
```

Seller A:

```text
Product A → ALLOW
Product B → DENY
```

---

# 13. AUTHZ-FR-008 — Scope-Based Authorization

Fynza should support authorization scopes.

Possible scopes:

```text
GLOBAL
PLATFORM
STORE
ORGANIZATION
OWN
RESOURCE
```

Example:

```text
ADMIN
 └── GLOBAL

SELLER
 └── STORE

CUSTOMER
 └── OWN
```

---

# 14. AUTHZ-FR-009 — Staff Permissions

Seller businesses may have multiple employees.

Example:

```text
Store
│
├── Owner
├── Manager
├── Product Manager
├── Inventory Staff
├── Order Staff
└── Support Staff
```

Each can have different permissions.

Example:

```text
Product Manager
├── product.read
├── product.create
└── product.update
```

but:

```text
Product Manager
└── payment.refund → DENY
```

---

# 15. AUTHZ-FR-010 — Permission Groups

For easier management, permissions can be grouped.

```text
Product Management
├── product.read
├── product.create
├── product.update
└── product.delete
```

```text
Order Management
├── order.read
├── order.update
├── order.cancel
└── order.fulfill
```

Permission groups are an administrative convenience and should not replace individual permissions internally.

---

# 16. AUTHZ-FR-011 — Temporary Permissions

The system may support temporary role assignments.

Example:

```text
Support Agent
    ↓
Temporary Admin Permission
    ↓
Expires: 24 hours
```

Useful for:

- Emergency operations.
- Temporary staff.
- Contractors.
- Incident response.

Expired assignments must automatically become invalid.

---

# 17. AUTHZ-FR-012 — Permission Denial

When authorization fails, the system should return an appropriate response.

REST:

```text
403 Forbidden
```

GraphQL:

```text
FORBIDDEN
```

The response must not expose internal authorization rules or sensitive information.

---

# 18. AUTHZ-FR-013 — Authorization Audit

Record security-sensitive authorization changes.

Events:

```text
ROLE_ASSIGNED
ROLE_REVOKED
PERMISSION_GRANTED
PERMISSION_REVOKED
POLICY_CHANGED
SCOPE_CHANGED
TEMPORARY_ACCESS_GRANTED
TEMPORARY_ACCESS_EXPIRED
ACCESS_DENIED
```

Administrative changes should include:

```text
performedBy
targetUser
role
permission
scope
reason
timestamp
```

---

# 19. Role Model

## 19.1 Role

```text
Role
-------------------------
id
name
code
description
systemRole
active
createdAt
updatedAt
```

Example:

```text
code: SELLER_MANAGER
name: Seller Manager
systemRole: false
```

---

# 20. Permission Model

```text
Permission
-------------------------
id
resource
action
code
description
createdAt
updatedAt
```

Example:

```text
resource: product
action: update
code: product.update
```

---

# 21. Role Permission

```text
RolePermission
-------------------------
id
roleId
permissionId
createdAt
```

Relationship:

```text
Role
 │
 └── RolePermission
        │
        └── Permission
```

---

# 22. User Role

```text
UserRole
-------------------------
id
userId
roleId
scopeType
scopeId
assignedBy
expiresAt
createdAt
```

Example:

```text
userId: USER-001
roleId: SELLER_MANAGER
scopeType: STORE
scopeId: STORE-001
```

This allows the same user to have different roles in different stores.

---

# 23. Policy Model

For advanced authorization, Fynza may introduce policies.

```text
Policy
-------------------------
id
name
resource
action
effect
conditions
active
createdAt
updatedAt
```

Example:

```text
product.update
ALLOW
condition:
product.storeId == user.storeId
```

---

# 24. Authorization Decision

The authorization engine should evaluate requests approximately as follows:

```text
Request
   │
   ▼
Authenticated?
   │
   ├── NO → DENY
   │
   ▼
Find User Roles
   │
   ▼
Find Permissions
   │
   ▼
Check Permission
   │
   ├── NO → DENY
   │
   ▼
Check Scope
   │
   ├── NO → DENY
   │
   ▼
Check Ownership
   │
   ├── NO → DENY
   │
   ▼
Check Business Policy
   │
   ├── NO → DENY
   │
   ▼
ALLOW
```

---

# 25. Authorization API

## REST

Administrative endpoints:

```text
GET    /roles
POST   /roles
PATCH  /roles/{id}
DELETE /roles/{id}

GET    /permissions

GET    /users/{id}/roles
POST   /users/{id}/roles
DELETE /users/{id}/roles/{roleId}

GET    /roles/{id}/permissions
POST   /roles/{id}/permissions
DELETE /roles/{id}/permissions/{permissionId}
```

Authorization should normally be enforced at the service/application layer rather than relying exclusively on controller restrictions.

---

# 26. GraphQL

## Queries

```graphql
roles
role(id: ID!)
permissions
userRoles(userId: ID!)
```

## Mutations

```graphql
createRole
updateRole
deleteRole

assignRole
revokeRole

grantPermission
revokePermission
```

Internal authorization checks should occur before executing protected mutations.

---

# 27. GraphQL Architecture

```text
authorization/
└── graphql/
    ├── resolver/
    │   ├── RoleResolver
    │   ├── PermissionResolver
    │   └── AuthorizationResolver
    │
    ├── input/
    │   ├── CreateRoleInput
    │   ├── UpdateRoleInput
    │   ├── AssignRoleInput
    │   └── PermissionInput
    │
    └── payload/
        ├── RolePayload
        ├── PermissionPayload
        └── AuthorizationPayload
```

---

# 28. Authorization Architecture

Recommended module:

```text
authorization/
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
├── security/
├── graphql/
│   ├── resolver/
│   ├── input/
│   └── payload/
└── AuthorizationModule.java
```

Tests:

```text
test/
└── authorization/
    ├── service/
    ├── repository/
    ├── policy/
    ├── security/
    ├── controller/
    ├── graphql/
    └── integration/
```

---

# 29. Service Design

The central authorization service could expose operations conceptually such as:

```text
isAllowed()
authorize()
hasPermission()
hasRole()
hasAnyRole()
hasAllPermissions()
ownsResource()
hasScope()
```

Example:

```text
authorizationService.authorize(
    currentUser,
    "product.update",
    product
);
```

Business modules should not need to know how roles and permission tables are implemented.

---

# 30. Spring Security Integration

Spring Security should act as one of the enforcement layers.

Conceptually:

```text
Spring Security
       │
       ▼
Authenticated Principal
       │
       ▼
Authorization Service
       │
       ▼
Role / Permission / Policy
       │
       ▼
Business Service
```

For method-level security, Fynza can use mechanisms such as:

```text
@PreAuthorize
```

Example:

```text
@PreAuthorize("hasAuthority('product.update')")
```

However, permission annotations alone should not replace resource ownership checks.

---

# 31. Resource Authorization

A stronger model is:

```text
Permission
+
Resource
+
Scope
+
Ownership
```

Example:

```text
product.update
       +
Product #123
       +
STORE:STORE-001
       +
Product.storeId == STORE-001
```

Result:

```text
ALLOW
```

This prevents a seller from modifying another seller's resources even if both sellers have the same permission.

---

# 32. Role Hierarchy

Role hierarchy may be used carefully.

Example:

```text
SUPER_ADMIN
    │
    ▼
ADMIN
    │
    ▼
SUPPORT_AGENT
```

However, role inheritance should not become overly complex.

Prefer explicit permissions where possible.

---

# 33. Default Roles

New users should receive a controlled default role.

Example:

```text
Registration
    ↓
Customer Account
    ↓
CUSTOMER Role
```

Seller accounts should not automatically become seller administrators merely because a user registers.

Seller authorization should depend on seller onboarding and approval.

---

# 34. Seller Authorization

Recommended seller model:

```text
User
 │
 ▼
Seller Profile
 │
 ▼
Business / Store
 │
 ├── Owner
 ├── Manager
 ├── Product Staff
 ├── Inventory Staff
 └── Order Staff
```

Permissions should be scoped to the relevant store.

Example:

```text
Seller A
 └── Store A
      └── product.update

Seller B
 └── Store B
      └── product.update
```

Both users have the same permission but different resource scopes.

---

# 35. Administrative Authorization

Administrative permissions should be significantly more restricted.

Example:

```text
ADMIN
├── user.read
├── user.manage
├── product.manage
├── order.manage
└── report.read
```

Sensitive operations should require stronger permissions:

```text
payment.refund
payment.settlement
user.delete
role.assign
permission.manage
system.configure
```

Highly sensitive actions may additionally require:

```text
MFA
+
Re-authentication
+
Audit
```

---

# 36. Security Requirements

## AUTHZ-NFR-001

Every protected operation must perform authorization.

## AUTHZ-NFR-002

Authorization must be deny-by-default.

```text
No Permission
      ↓
DENY
```

## AUTHZ-NFR-003

Users must not be able to grant themselves permissions.

## AUTHZ-NFR-004

Users must not be able to modify roles outside their scope.

## AUTHZ-NFR-005

Resource ownership must be verified server-side.

## AUTHZ-NFR-006

Authorization must never rely on frontend controls.

The frontend may hide buttons, but the backend must still enforce permissions.

## AUTHZ-NFR-007

Administrative actions must be audited.

## AUTHZ-NFR-008

Sensitive permissions should require MFA or re-authentication where appropriate.

---

# 37. Testing

## 37.1 Unit Tests

Test:

- Role evaluation.
- Permission evaluation.
- Scope evaluation.
- Ownership checks.
- Policy evaluation.
- Role hierarchy.
- Permission inheritance.
- Temporary role expiration.

---

## 37.2 Integration Tests

Test:

```text
User
 ↓
Role
 ↓
Permission
 ↓
Authorization Service
 ↓
Database
```

---

## 37.3 Security Tests

Test:

- Horizontal privilege escalation.
- Vertical privilege escalation.
- IDOR vulnerabilities.
- Cross-store access.
- Unauthorized role assignment.
- Unauthorized permission changes.
- Privilege persistence after role revocation.
- Expired temporary permissions.
- Admin privilege abuse.

---

# 38. Example Authorization Scenarios

## Scenario 1 — Customer Reads Product

```text
User: Customer
Permission: product.read
Resource: Product
Scope: GLOBAL
```

Result:

```text
ALLOW
```

---

## Scenario 2 — Customer Updates Product

```text
User: Customer
Permission: product.update
```

Result:

```text
DENY
```

---

## Scenario 3 — Seller Updates Own Product

```text
User: Seller A
Permission: product.update
Product.storeId: Store A
User.storeId: Store A
```

Result:

```text
ALLOW
```

---

## Scenario 4 — Seller Updates Another Store's Product

```text
User: Seller A
Permission: product.update
Product.storeId: Store B
User.storeId: Store A
```

Result:

```text
DENY
```

---

## Scenario 5 — Admin Suspends User

```text
User: Admin
Permission: user.suspend
Target: User B
```

Result:

```text
ALLOW
```

---

# 39. User Stories

## US-001 — Permission

> As a platform administrator, I want to define permissions so that access can be controlled precisely.

## US-002 — Role

> As a platform administrator, I want to create roles so that groups of permissions can be managed efficiently.

## US-003 — Assign Role

> As an authorized administrator, I want to assign roles to users so that users receive appropriate access.

## US-004 — Seller Staff

> As a seller owner, I want to assign different permissions to my staff so that employees can perform only their required tasks.

## US-005 — Resource Ownership

> As a seller, I want access restricted to my own store's resources so that other sellers' data remains protected.

## US-006 — Authorization

> As a platform user, I want unauthorized operations to be rejected so that my data and the platform remain secure.

---

# 40. Milestones

## M1 — Permission Foundation

- [ ] Permission entity.
- [ ] Permission definitions.
- [ ] Permission registry.
- [ ] Permission evaluation.
- [ ] Deny-by-default behavior.

## M2 — Roles

- [ ] Role entity.
- [ ] Role-permission mapping.
- [ ] Default customer role.
- [ ] Role assignment.
- [ ] Role revocation.

## M3 — Scoped Authorization

- [ ] Store scope.
- [ ] Organization scope.
- [ ] Ownership checks.
- [ ] Resource authorization.
- [ ] Seller staff permissions.

## M4 — Administration

- [ ] Role management.
- [ ] Permission management.
- [ ] User-role management.
- [ ] Authorization audit.

## M5 — Advanced Authorization

- [ ] Policies.
- [ ] Temporary permissions.
- [ ] Conditional authorization.
- [ ] Fine-grained resource policies.

---

# 41. Definition of Done

- [ ] Roles implemented.
- [ ] Permissions implemented.
- [ ] Role-permission mapping implemented.
- [ ] User-role mapping implemented.
- [ ] Authorization service implemented.
- [ ] Deny-by-default implemented.
- [ ] Resource ownership implemented.
- [ ] Store-level scope implemented.
- [ ] Seller staff authorization implemented.
- [ ] Administrative authorization implemented.
- [ ] Audit logging implemented.
- [ ] GraphQL authorization implemented.
- [ ] Spring Security integration implemented.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] E2E authorization tests pass.
- [ ] Privilege escalation tests pass.
- [ ] API documentation completed.

---

# 42. Architectural Principle

Fynza should enforce authorization using:

```text
                 Authentication
                       │
                       ▼
                      User
                       │
                       ▼
                     Roles
                       │
                       ▼
                  Permissions
                       │
                       ▼
                     Scope
                       │
                       ▼
                   Ownership
                       │
                       ▼
                    Policy
                       │
                       ▼
                  ALLOW / DENY
```

The most important rule is:

> **Never trust the client to enforce authorization.**

The frontend may hide or disable functionality, but **every protected operation must be authorized on the backend**.

---

# 43. Relationship With Previous Modules

The first three modules now form the identity/security foundation:

```text
01 Authentication
        │
        │ Who are you?
        ▼
02 User Management
        │
        │ Who is this user?
        ▼
03 Authorization & Roles
        │
        │ What can they do?
        ▼
04 Business Modules
```

Business modules can now safely build on this foundation:

```text
Customer
Seller
Product
Category
Cart
Order
Payment
Inventory
Shipping
Review
Notification
```

The business modules should consume authorization capabilities rather than implementing independent role systems.