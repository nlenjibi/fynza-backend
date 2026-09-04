# 05 — Seller Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Seller Management  
**Module ID:** `SELLER-05`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Authentication, User Management, Authorization, Customer Management

---

# 1. Overview

The Seller Management module manages businesses and users that sell products through Fynza.

A seller is a business participant in the Fynza marketplace.

The module manages:

- Seller accounts
- Seller businesses
- Seller onboarding
- Seller verification
- Seller status
- Seller business information
- Seller ownership
- Seller staff foundations
- Seller compliance information
- Seller administrative management
- Seller lifecycle
- Seller settings

The module does **not** own:

- Products
- Inventory
- Orders
- Payments
- Shipping
- Storefront presentation

Those belong to their respective business modules.

---

# 2. Goals

The module should:

1. Allow users to become sellers.
2. Represent seller businesses independently from users.
3. Support seller onboarding.
4. Support seller verification.
5. Support seller lifecycle management.
6. Support multiple users associated with a seller.
7. Establish seller ownership.
8. Provide a foundation for seller stores.
9. Support seller administration.
10. Support seller compliance.
11. Provide secure seller-scoped authorization.
12. Provide seller information to other business modules.

---

# 3. Non-Goals

Seller Management does not own:

- Authentication.
- Passwords.
- User credentials.
- Generic user profiles.
- Products.
- Categories.
- Inventory.
- Orders.
- Payments.
- Payout execution.
- Shipping.
- Reviews.

For example:

```text
Seller
  │
  ├── Products       → Product Module
  ├── Inventory      → Inventory Module
  ├── Orders         → Order Module
  ├── Payments       → Payment Module
  └── Store          → Store Module
```

Seller Management owns the **seller relationship and seller business identity**.

---

# 4. Seller Domain Model

The recommended model is:

```text
User
 │
 ▼
Seller
 │
 ▼
Business
 │
 ├── Store
 ├── Staff
 ├── Verification
 └── Settings
```

However, `Store` should eventually be its own module.

Therefore:

```text
User
 │
 ▼
Seller
 │
 └──────────────► Store
```

The Seller module owns the relationship, while the Store module owns storefront behavior.

---

# 5. Seller vs User

Do not make `User` the seller.

Instead:

```text
User
 │
 ├── Customer
 │
 └── Seller
```

This allows one person to participate in multiple Fynza domains.

Example:

```text
User
 ├── Customer
 │
 └── Seller
       └── Business
```

The same user may purchase products and operate a business.

---

# 6. Seller Types

Fynza may support:

```text
INDIVIDUAL
BUSINESS
ORGANIZATION
```

### Individual Seller

A person selling products independently.

### Business Seller

A registered business selling through Fynza.

### Organization

A larger organization with multiple staff members and potentially multiple stores.

The initial implementation may support `INDIVIDUAL` and `BUSINESS`, while keeping the model extensible.

---

# 7. Seller Lifecycle

Recommended states:

```text
DRAFT
PENDING_VERIFICATION
UNDER_REVIEW
ACTIVE
SUSPENDED
REJECTED
BLOCKED
CLOSED
```

Typical flow:

```text
DRAFT
  │
  ▼
PENDING_VERIFICATION
  │
  ▼
UNDER_REVIEW
  │
  ├────────► REJECTED
  │
  ▼
ACTIVE
  │
  ├────────► SUSPENDED
  │               │
  │               ▼
  │             ACTIVE
  │
  ├────────► BLOCKED
  │
  └────────► CLOSED
```

---

# 8. Seller Status vs User Status

Seller status must remain separate from authentication status.

Example:

```text
User Status     = ACTIVE
Seller Status   = SUSPENDED
```

The user may still log into Fynza while being prevented from selling.

This is intentional.

---

# 9. SELLER-FR-001 — Seller Registration

A user should be able to initiate seller onboarding.

```text
User
 │
 ▼
Become a Seller
 │
 ▼
Seller Application
 │
 ▼
DRAFT
```

The user should provide the required seller information.

---

# 10. SELLER-FR-002 — Seller Onboarding

Seller onboarding may include:

```text
Step 1 → Personal Information
Step 2 → Business Information
Step 3 → Contact Information
Step 4 → Verification
Step 5 → Seller Agreement
Step 6 → Review
Step 7 → Activation
```

Example:

```text
Seller Onboarding
       │
       ├── Identity
       ├── Business
       ├── Contact
       ├── Verification
       └── Agreement
```

---

# 11. SELLER-FR-003 — Business Information

Business sellers should provide:

```text
businessName
legalName
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
```

Not every field is mandatory for every seller type.

---

# 12. SELLER-FR-004 — Seller Number

Each seller should receive a unique business identifier.

Example:

```text
SEL-000001
SEL-000002
SEL-000003
```

This is separate from:

```text
userId
businessId
databaseId
```

---

# 13. SELLER-FR-005 — Seller Profile

A seller profile may include:

```text
sellerNumber
displayName
businessName
description
logo
website
contactInformation
sellerType
status
memberSince
```

Seller profile information should be separated from generic User Profile data.

---

# 14. SELLER-FR-006 — Seller Verification

Seller verification is required before certain seller capabilities become available.

Verification may include:

```text
Identity Verification
Business Verification
Contact Verification
Tax Verification
Bank/Payout Verification
```

The exact requirements depend on jurisdiction and Fynza's business model.

---

# 15. Verification Architecture

Verification should be modeled separately from the seller itself.

```text
Seller
 │
 └── Verification
      │
      ├── Identity
      ├── Business
      ├── Contact
      └── Payout
```

Example statuses:

```text
NOT_STARTED
PENDING
IN_REVIEW
VERIFIED
REJECTED
EXPIRED
```

---

# 16. SELLER-FR-007 — Verification Review

Authorized staff should be able to review seller applications.

Review actions:

```text
Approve
Reject
Request More Information
```

Every review should be audited.

---

# 17. SELLER-FR-008 — Seller Activation

A seller becomes active only after required onboarding conditions are satisfied.

Example:

```text
Seller Application
       │
       ▼
Required Verification
       │
       ▼
Approved
       │
       ▼
ACTIVE
```

The activation policy should be configurable.

---

# 18. SELLER-FR-009 — Seller Suspension

Authorized staff may suspend sellers.

Possible reasons:

```text
Policy Violation
Fraud Investigation
Product Abuse
Customer Complaints
Compliance Issue
Payment Issue
Operational Issue
```

Suspension should record:

```text
reason
performedBy
createdAt
expiresAt
```

---

# 19. SELLER-FR-010 — Seller Reactivation

Authorized administrators can reactivate suspended sellers.

```text
SUSPENDED
    │
    ▼
ACTIVE
```

The action must be audited.

---

# 20. SELLER-FR-011 — Seller Blocking

Blocking should be used for severe cases.

```text
ACTIVE
   │
   ▼
BLOCKED
```

A blocked seller should not be able to perform seller operations.

Existing orders should be handled according to Order and Operations policies.

---

# 21. SELLER-FR-012 — Seller Closure

A seller may request closure.

```text
ACTIVE
   │
   ▼
CLOSED
```

Closure should not automatically destroy historical business data.

Historical:

- Orders
- Payments
- Reviews
- Transactions
- Audit records

must remain available according to retention policies.

---

# 22. Seller Ownership

A seller must have an owner.

```text
Seller
  │
  └── Owner
       │
       ▼
      User
```

Example:

```text
Seller: SEL-001
Owner: USER-100
```

Ownership is important for authorization.

---

# 23. Multiple Seller Staff

A seller may eventually have multiple staff members.

```text
Seller
 │
 ├── Owner
 ├── Manager
 ├── Inventory Staff
 ├── Order Staff
 └── Finance Staff
```

The Seller module should establish the seller-to-user relationship.

Authorization determines what each staff member can do.

---

# 24. Seller Staff Architecture

Recommended:

```text
Seller
 │
 └── SellerMembership
       │
       ├── userId
       ├── role
       ├── scope
       └── status
```

Example:

```text
User A → Owner
User B → Manager
User C → Inventory Staff
```

Detailed permissions remain under Authorization.

---

# 25. SELLER-FR-013 — Add Seller Staff

Authorized seller users should be able to invite staff.

```text
Seller Owner
     │
     ▼
Invite User
     │
     ▼
Seller Membership
     │
     ▼
Staff Member
```

The invitation should expire after a configured period.

---

# 26. SELLER-FR-014 — Remove Seller Staff

Authorized users should be able to remove or deactivate seller staff.

The system must prevent an owner from accidentally leaving the business without an authorized owner unless ownership transfer is explicitly handled.

---

# 27. SELLER-FR-015 — Ownership Transfer

Ownership transfer should be an explicit workflow.

```text
Current Owner
     │
     ▼
Select New Owner
     │
     ▼
Confirm Transfer
     │
     ▼
New Owner
```

The operation should require strong authorization and auditing.

---

# 28. Seller Membership Status

Recommended:

```text
INVITED
ACTIVE
SUSPENDED
REMOVED
```

Example:

```text
Seller
 │
 ├── User A → ACTIVE
 ├── User B → ACTIVE
 └── User C → SUSPENDED
```

---

# 29. Seller Settings

Seller-specific settings may include:

```text
businessDisplayName
defaultCurrency
timezone
orderNotifications
customerNotifications
marketingSettings
storeSettings
```

Settings that are specific to storefront behavior should belong to the Store module.

---

# 30. Seller Data Model

## Seller

```text
Seller
-------------------------
id
publicId
sellerNumber
ownerUserId
sellerType
status
displayName
createdAt
updatedAt
```

Constraints:

```text
sellerNumber → UNIQUE
publicId      → UNIQUE
```

---

# 31. Seller Business

```text
SellerBusiness
-------------------------
id
sellerId
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
createdAt
updatedAt
```

---

# 32. Seller Membership

```text
SellerMembership
-------------------------
id
sellerId
userId
membershipType
status
invitedBy
joinedAt
createdAt
updatedAt
```

Recommended membership types:

```text
OWNER
ADMIN
MANAGER
STAFF
```

Fine-grained capabilities should come from Authorization.

---

# 33. Seller Verification

```text
SellerVerification
-------------------------
id
sellerId
verificationType
status
submittedAt
reviewedAt
reviewedBy
rejectionReason
expiresAt
createdAt
updatedAt
```

---

# 34. Seller Status History

```text
SellerStatusHistory
-------------------------
id
sellerId
previousStatus
newStatus
reason
changedBy
createdAt
expiresAt
```

---

# 35. Seller API

## REST

```text
GET    /sellers/me
PATCH  /sellers/me

POST   /sellers
GET    /sellers/{id}

POST   /sellers/onboarding
PATCH  /sellers/onboarding

GET    /sellers/me/verification
POST   /sellers/me/verification

GET    /sellers/me/staff
POST   /sellers/me/staff
DELETE /sellers/me/staff/{id}
```

Administrative:

```text
GET   /admin/sellers
GET   /admin/sellers/{id}

POST  /admin/sellers/{id}/approve
POST  /admin/sellers/{id}/reject
POST  /admin/sellers/{id}/suspend
POST  /admin/sellers/{id}/activate
POST  /admin/sellers/{id}/block
```

---

# 36. GraphQL

## Queries

```graphql
meSeller
seller(id: ID!)
sellers(filter: SellerFilterInput)
sellerVerification
sellerStaff
```

## Mutations

```graphql
createSeller
updateSeller
submitSellerApplication

submitSellerVerification
updateSellerVerification

inviteSellerStaff
removeSellerStaff
transferSellerOwnership

approveSeller
rejectSeller
suspendSeller
activateSeller
blockSeller
```

---

# 37. GraphQL Structure

```text
seller/
└── graphql/
    ├── resolver/
    │   ├── SellerResolver
    │   ├── SellerVerificationResolver
    │   └── SellerStaffResolver
    │
    ├── input/
    │   ├── CreateSellerInput
    │   ├── UpdateSellerInput
    │   ├── SellerFilterInput
    │   └── SellerVerificationInput
    │
    └── payload/
        ├── SellerPayload
        ├── SellerVerificationPayload
        └── SellerListPayload
```

---

# 38. Authorization

Seller operations require seller-scoped permissions.

Examples:

```text
seller.read
seller.update

seller.staff.read
seller.staff.invite
seller.staff.remove

seller.verification.read
seller.verification.submit

seller.store.manage
seller.product.manage
seller.order.manage
```

The last three permissions may be consumed by other modules.

---

# 39. Seller Scope

Authorization should determine:

```text
User
  │
  ▼
Seller Membership
  │
  ▼
Seller Scope
  │
  ▼
Resource
```

Example:

```text
User A
  │
  └── Seller A
        │
        └── Product A
```

User A should not automatically access:

```text
Seller B
Product B
Order B
```

---

# 40. Seller and Product

Products belong to the Product module.

Relationship:

```text
Seller
  │
  ▼
Product
```

The Product module should store the seller reference:

```text
Product
----------------
id
sellerId
...
```

Seller Management should not own product records.

---

# 41. Seller and Store

Store should eventually be its own module.

```text
Seller
 │
 └── Store
      │
      ├── Storefront
      ├── Branding
      ├── Store Settings
      └── Catalog Presentation
```

This prevents the Seller entity from becoming a large aggregate.

---

# 42. Seller and Order

Orders belong to Order Management.

```text
Seller
   │
   ▼
Order
   │
   └── Order Items
```

A marketplace order may contain products from multiple sellers.

Therefore, the Order architecture should support seller-level fulfillment/order views.

---

# 43. Seller and Payment

Payments belong to Payment Management.

Seller Management should not process payments directly.

Instead:

```text
Order
 │
 ▼
Payment
 │
 ▼
Seller Payout
```

Seller payout functionality should eventually belong to a dedicated Payment/Payout module.

---

# 44. Seller Events

Recommended domain events:

```text
SELLER_CREATED
SELLER_APPLICATION_SUBMITTED
SELLER_VERIFICATION_SUBMITTED
SELLER_VERIFIED
SELLER_REJECTED
SELLER_ACTIVATED
SELLER_SUSPENDED
SELLER_BLOCKED
SELLER_CLOSED

SELLER_STAFF_INVITED
SELLER_STAFF_JOINED
SELLER_STAFF_REMOVED

SELLER_OWNERSHIP_TRANSFERRED
SELLER_UPDATED
```

Example:

```text
Seller
  │
  ▼
SELLER_VERIFIED
  │
  ├──► Authorization
  ├──► Notification
  ├──► Store
  └──► Audit
```

---

# 45. Seller Verification Events

Verification events should be separate where useful:

```text
SELLER_VERIFICATION_STARTED
SELLER_VERIFICATION_SUBMITTED
SELLER_VERIFICATION_APPROVED
SELLER_VERIFICATION_REJECTED
SELLER_VERIFICATION_EXPIRED
```

---

# 46. Audit

The following actions must be auditable:

- Seller creation.
- Application submission.
- Verification.
- Approval.
- Rejection.
- Suspension.
- Activation.
- Blocking.
- Closure.
- Staff invitation.
- Staff removal.
- Ownership transfer.

Audit record:

```text
performedBy
sellerId
action
reason
timestamp
metadata
```

---

# 47. Security Requirements

## SELLER-NFR-001

Seller resources must be protected using seller ownership or membership.

## SELLER-NFR-002

Seller staff must only access resources permitted by their role.

## SELLER-NFR-003

Seller verification data must be protected.

## SELLER-NFR-004

Administrative seller actions must be audited.

## SELLER-NFR-005

Internal database IDs should not unnecessarily be exposed.

## SELLER-NFR-006

Seller status changes require explicit authorization.

## SELLER-NFR-007

Ownership transfer requires elevated authorization.

---

# 48. Data Privacy

Seller information may contain sensitive business data.

The system must protect:

```text
Registration Information
Tax Information
Business Documents
Identity Information
Payout Information
Contact Information
```

Do not expose verification documents through ordinary seller profile queries.

---

# 49. Performance

Recommended indexes:

```text
Seller
├── sellerNumber UNIQUE
├── publicId UNIQUE
├── ownerUserId
├── status
└── createdAt

SellerMembership
├── sellerId
├── userId
├── status
└── membershipType

SellerVerification
├── sellerId
├── verificationType
└── status
```

---

# 50. Database Views and Read Models

For complex seller dashboards, avoid creating giant JPA entities.

For example:

```text
Seller
 │
 ├── Products
 ├── Orders
 ├── Payments
 ├── Reviews
 └── Inventory
```

A dashboard may require all of these.

Instead, use a read model:

```text
Seller Dashboard
       │
       ▼
SellerSummaryView
       │
 ┌─────┼─────────┐
 ▼     ▼         ▼
Order Product Payment
```

Database views or projections should be treated as **read-only query models**, not domain entities.

Recommended structure:

```text
seller/
├── entity/
├── repository/
├── query/
│   ├── projection/
│   └── view/
├── service/
└── ...
```

This aligns with Fynza's use of database views for complex joins while keeping write/domain models clean.

---

# 51. Module Structure

```text
seller/
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
└── SellerModule.java
```

Tests:

```text
test/
└── seller/
    ├── service/
    ├── repository/
    ├── policy/
    ├── controller/
    ├── graphql/
    ├── integration/
    └── security/
```

---

# 52. Dependencies

```text
Seller
│
├── Authentication
├── User Management
├── Authorization
├── Common
├── Notification
├── Storage
└── Audit/Event Infrastructure
```

Future integrations:

```text
Seller
 │
 ├── Store
 ├── Product
 ├── Inventory
 ├── Order
 ├── Payment
 ├── Review
 └── Analytics
```

These should communicate through clear interfaces/events rather than tightly coupling entities.

---

# 53. User Stories

## US-001 — Become Seller

> As a user, I want to become a seller so that I can sell products on Fynza.

## US-002 — Seller Onboarding

> As a seller, I want to complete onboarding so that my business can be reviewed and activated.

## US-003 — Business Verification

> As a seller, I want to submit my business information for verification so that I can become an approved seller.

## US-004 — Seller Profile

> As a seller, I want to manage my business information so that customers see accurate information.

## US-005 — Staff

> As a seller owner, I want to invite staff so that other people can help manage my business.

## US-006 — Staff Removal

> As a seller owner, I want to remove staff so that former employees no longer have access.

## US-007 — Ownership Transfer

> As a seller owner, I want to transfer ownership so that another authorized person can manage the business.

## US-008 — Seller Suspension

> As an administrator, I want to suspend sellers so that I can protect the marketplace from policy violations.

## US-009 — Seller Search

> As an administrator, I want to search sellers so that I can efficiently manage the marketplace.

---

# 54. Milestones

## M1 — Seller Foundation

- [ ] Seller entity.
- [ ] Seller number.
- [ ] User-to-seller relationship.
- [ ] Seller lifecycle.
- [ ] Seller profile.
- [ ] Seller creation.

## M2 — Seller Onboarding

- [ ] Seller application.
- [ ] Business information.
- [ ] Onboarding workflow.
- [ ] Application submission.
- [ ] Application status.

## M3 — Verification

- [ ] Verification model.
- [ ] Verification workflow.
- [ ] Admin review.
- [ ] Approval.
- [ ] Rejection.
- [ ] Verification events.

## M4 — Seller Staff

- [ ] Seller membership.
- [ ] Staff invitation.
- [ ] Staff activation.
- [ ] Staff removal.
- [ ] Ownership transfer.
- [ ] Seller-scoped authorization.

## M5 — Administration

- [ ] Seller search.
- [ ] Seller details.
- [ ] Seller suspension.
- [ ] Seller activation.
- [ ] Seller blocking.
- [ ] Seller closure.
- [ ] Audit.

## M6 — Business Integration

- [ ] Store integration.
- [ ] Product integration.
- [ ] Inventory integration.
- [ ] Order integration.
- [ ] Payment/payout integration.
- [ ] Review integration.
- [ ] Analytics integration.

---

# 55. Testing Strategy

## Unit Tests

Test:

- Seller creation.
- Seller number generation.
- Status transitions.
- Onboarding validation.
- Verification rules.
- Membership rules.
- Ownership transfer.
- Seller policies.

## Integration Tests

Test:

```text
User
 ↓
Seller
 ↓
SellerBusiness
 ↓
SellerMembership
 ↓
SellerVerification
```

## Security Tests

Test:

- Seller A accessing Seller B.
- Staff accessing unauthorized resources.
- Removed staff accessing seller resources.
- Suspended seller performing restricted operations.
- Unauthorized ownership transfer.
- Unauthorized seller approval.
- IDOR.
- Privilege escalation.

## E2E Tests

### Seller Onboarding

```text
User
 ↓
Start Seller Application
 ↓
Complete Business Information
 ↓
Submit Verification
 ↓
Admin Review
 ↓
Approve
 ↓
Seller ACTIVE
```

### Staff Management

```text
Owner
 ↓
Invite Staff
 ↓
Staff Accepts
 ↓
Membership ACTIVE
 ↓
Staff Performs Authorized Action
 ↓
Owner Removes Staff
 ↓
Access Revoked
```

---

# 56. Definition of Done

- [ ] Seller entity implemented.
- [ ] Seller number implemented.
- [ ] Seller profile implemented.
- [ ] Seller business information implemented.
- [ ] Seller lifecycle implemented.
- [ ] Seller onboarding implemented.
- [ ] Verification workflow implemented.
- [ ] Seller membership implemented.
- [ ] Staff invitation implemented.
- [ ] Ownership transfer implemented.
- [ ] Seller administration implemented.
- [ ] Seller suspension implemented.
- [ ] Seller blocking implemented.
- [ ] Audit implemented.
- [ ] Domain events implemented.
- [ ] REST API implemented.
- [ ] GraphQL API implemented.
- [ ] Seller-scoped authorization implemented.
- [ ] Database indexes implemented.
- [ ] Query/read models implemented where required.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] E2E tests pass.
- [ ] Documentation completed.

---

# 57. Architectural Principle

The Seller module should **not become the entire marketplace module**.

Keep the boundary:

```text
┌───────────────────────────────────────┐
│               User                    │
│       Identity / Profile              │
└───────────────────┬───────────────────┘
                    │
                    ▼
┌───────────────────────────────────────┐
│              Seller                   │
│                                       │
│ Business Identity                     │
│ Onboarding                            │
│ Verification                          │
│ Membership                            │
│ Seller Lifecycle                      │
└───────────────────┬───────────────────┘
                    │
       ┌────────────┼────────────┐
       ▼            ▼            ▼
     Store        Product      Inventory
       │            │            │
       └────────────┼────────────┘
                    ▼
                  Order
                    │
                    ▼
                 Payment
```

### Core Rule

> **Seller Management owns who the seller is and their relationship with Fynza. It does not own everything the seller does.**

This separation will make the later **Store, Product, Inventory, Order, Payment, Shipping, Review, and Payout** modules much easier to scale independently.