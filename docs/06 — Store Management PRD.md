# 06 — Store Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Store Management  
**Module ID:** `STORE-06`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Authentication, User Management, Authorization, Seller Management

---

# 1. Overview

The Store Management module manages the storefront through which a seller presents their business and products to customers.

A **Seller** represents the business participant.

A **Store** represents the seller's commercial presence on Fynza.

```text
User
 │
 ▼
Seller
 │
 ▼
Store
 │
 ├── Store Profile
 ├── Branding
 ├── Store Settings
 ├── Store Policies
 └── Store Status
```

The Store module provides the foundation for:

- Store creation.
- Store profiles.
- Store names.
- Store slugs.
- Store branding.
- Store descriptions.
- Store contact information.
- Store policies.
- Store operating status.
- Store visibility.
- Store settings.
- Store storefront configuration.

It does **not** own products, inventory, orders, payments, or seller verification.

---

# 2. Goals

The Store module should:

1. Allow sellers to create stores.
2. Associate stores with sellers.
3. Provide public storefronts.
4. Provide unique store URLs/slugs.
5. Manage store branding.
6. Manage store settings.
7. Manage store visibility.
8. Support store lifecycle.
9. Support store policies.
10. Support seller-scoped store management.
11. Provide a foundation for storefront discovery.
12. Allow customers to browse stores independently of products.

---

# 3. Non-Goals

Store Management does not own:

- Seller identity.
- Seller verification.
- Products.
- Product categories.
- Inventory.
- Orders.
- Payments.
- Payouts.
- Shipping execution.
- Reviews.
- Customer accounts.
- Search infrastructure.

Instead:

```text
Seller       → Seller Management
Store        → Store Management
Product      → Product Management
Inventory    → Inventory Management
Order        → Order Management
Payment      → Payment Management
Review       → Review Management
```

---

# 4. Seller vs Store

This distinction is fundamental.

```text
Seller
 │
 ├── Legal/business identity
 ├── Verification
 ├── Ownership
 ├── Staff
 └── Seller lifecycle
          │
          ▼
        Store
          │
          ├── Public name
          ├── Logo
          ├── Banner
          ├── Description
          ├── Policies
          ├── Store settings
          └── Storefront
```

Example:

```text
Seller:
  ABC Trading Ltd

Store:
  ABC Fashion Store
```

A seller may eventually operate multiple stores if Fynza's business rules allow it.

Therefore, do not hard-code:

```text
Seller 1 → exactly one Store
```

unless the product requirement explicitly requires it.

---

# 5. Store Lifecycle

Recommended statuses:

```text
DRAFT
PENDING_REVIEW
ACTIVE
PAUSED
SUSPENDED
CLOSED
ARCHIVED
```

Typical lifecycle:

```text
DRAFT
  │
  ▼
PENDING_REVIEW
  │
  ▼
ACTIVE
  │
  ├──► PAUSED
  │      │
  │      ▼
  │    ACTIVE
  │
  ├──► SUSPENDED
  │
  └──► CLOSED
          │
          ▼
       ARCHIVED
```

---

# 6. Store Visibility

Store status and visibility should be separate concepts.

Example:

```text
status      = ACTIVE
visibility  = PRIVATE
```

Possible visibility:

```text
PUBLIC
PRIVATE
UNLISTED
```

### PUBLIC

Customers can discover and browse the store.

### PRIVATE

The store is not publicly available.

### UNLISTED

Customers may access the store through a direct URL, but it does not appear in normal discovery.

---

# 7. STORE-FR-001 — Create Store

An authorized seller should be able to create a store.

```text
Seller
 │
 ▼
Create Store
 │
 ▼
Validate Seller
 │
 ▼
Create Store
 │
 ▼
DRAFT
```

The system must verify that the requesting user has permission to create a store for the seller.

---

# 8. STORE-FR-002 — Store Name

Every store must have a display name.

Example:

```text
Fynza Fashion
Accra Electronics
Tech World Ghana
Home Essentials
```

Store names should:

- Be validated.
- Have configurable length limits.
- Reject prohibited content.
- Support future localization if required.

---

# 9. STORE-FR-003 — Store Slug

Every public store should have a unique URL-safe slug.

Examples:

```text
fynza-fashion
accra-electronics
tech-world-ghana
home-essentials
```

Conceptually:

```text
/store/fynza-fashion
/store/accra-electronics
```

The slug should be unique across the relevant Fynza namespace.

---

# 10. Slug Rules

The system should:

- Normalize case.
- Remove unsupported characters.
- Prevent duplicate slugs.
- Reserve protected words.
- Support slug changes.
- Preserve old slugs where redirects are required.

Reserved examples:

```text
admin
api
login
register
settings
help
support
stores
products
orders
```

---

# 11. STORE-FR-004 — Store Description

Sellers should be able to provide a description.

Example:

```text
ABC Fashion provides affordable and
quality fashion products across Ghana.
```

The description should support appropriate formatting while preventing unsafe content.

---

# 12. STORE-FR-005 — Store Logo

Sellers should be able to upload a store logo.

Recommended architecture:

```text
Seller
 │
 ▼
Store
 │
 ▼
Storage Service
 │
 ▼
logoUrl
```

The Store module should store references to media rather than binary files.

---

# 13. STORE-FR-006 — Store Banner

A store may have:

- Cover image.
- Banner.
- Promotional image.

Media should be managed through the Storage/Media module.

```text
Store
 │
 ├── logoMediaId
 ├── bannerMediaId
 └── galleryMediaIds
```

The Store module should not become a file-storage system.

---

# 14. STORE-FR-007 — Store Contact Information

A store may have public contact information:

```text
businessEmail
businessPhone
website
socialLinks
```

Private seller information should not automatically become public store information.

---

# 15. STORE-FR-008 — Store Address

A store may have a business location.

Possible information:

```text
addressLine1
addressLine2
city
region
country
postalCode
latitude
longitude
```

If Fynza later introduces a dedicated Address/Location module, Store should reference that module rather than duplicating address infrastructure.

---

# 16. STORE-FR-009 — Store Policies

Stores may define customer-facing policies.

Examples:

```text
Return Policy
Refund Policy
Shipping Policy
Cancellation Policy
Warranty Policy
```

The policies should be versionable if they have legal/business significance.

---

# 17. Store Policy Model

```text
Store
 │
 └── StorePolicy
      │
      ├── RETURN
      ├── REFUND
      ├── SHIPPING
      ├── CANCELLATION
      └── WARRANTY
```

Example:

```text
StorePolicy
-------------------------
id
storeId
type
title
content
version
status
effectiveFrom
createdAt
updatedAt
```

---

# 18. STORE-FR-010 — Store Settings

Store settings may include:

```text
defaultCurrency
timezone
language
orderNotifications
customerMessages
storeVisibility
```

Product-specific settings should remain in Product Management.

Inventory settings should remain in Inventory Management.

---

# 19. STORE-FR-011 — Store Operating Status

A seller should be able to temporarily pause a store.

Example:

```text
ACTIVE
  │
  ▼
PAUSED
```

A paused store may remain visible but should clearly indicate that ordering is temporarily unavailable.

---

# 20. Store Pause

Pause information should include:

```text
pausedAt
pausedBy
reason
resumeAt
```

Example:

```text
Store
 │
 ├── status = PAUSED
 ├── reason = Holiday
 └── resumeAt = 2026-12-20
```

---

# 21. STORE-FR-012 — Store Suspension

Fynza administrators may suspend a store.

Suspension is different from seller suspension.

```text
Seller = ACTIVE
Store  = SUSPENDED
```

This can be useful when the seller remains on the platform but a specific storefront violates marketplace rules.

---

# 22. STORE-FR-013 — Store Closure

A seller may request store closure.

Closure should not destroy historical records.

```text
Store
 │
 ├── Products
 ├── Orders
 ├── Reviews
 └── Historical Data
```

Historical relationships should remain intact.

---

# 23. STORE-FR-014 — Public Storefront

Customers should be able to view a public store.

Example:

```text
Store
 │
 ├── Store Name
 ├── Logo
 ├── Description
 ├── Rating
 ├── Policies
 └── Products
```

Products are retrieved from Product Management.

The Store module should not duplicate Product entities.

---

# 24. Storefront Architecture

```text
Customer
   │
   ▼
Storefront
   │
   ▼
Store Query
   │
   ├── Store Information
   ├── Product Query
   ├── Review Query
   └── Store Statistics
```

A storefront response may combine multiple domains.

This is an ideal use case for read models/database views.

---

# 25. Database Views

For a public store page, the frontend may need:

```text
Store
+
Seller summary
+
Product count
+
Average rating
+
Review count
+
Sales summary
```

Do not create a giant Store entity containing all of this.

Instead:

```text
Store
 │
 ▼
StoreSummaryView
 │
 ├── Store
 ├── Product
 ├── Review
 └── Order-derived metrics
```

Recommended:

```text
store/
├── entity/
├── repository/
├── query/
│   ├── projection/
│   └── view/
└── ...
```

The database view should be read-only.

---

# 26. STORE-FR-015 — Store Statistics

Store statistics may include:

```text
productCount
activeProductCount
reviewCount
averageRating
orderCount
completedOrderCount
```

These are derived metrics.

The authoritative transaction data remains in the respective modules.

---

# 27. Store Rating

If Fynza supports store ratings:

```text
Customer
   │
   ▼
Review
   │
   ▼
Store
```

The Review module should own review records.

Store Management can expose the aggregate rating through a read model.

---

# 28. STORE-FR-016 — Store Search

Customers should be able to discover stores.

Possible search fields:

```text
storeName
category
location
rating
sellerType
```

However, a dedicated Search/Discovery module should eventually own advanced search.

Store Management should expose the data required for indexing.

---

# 29. Store Categories

Do not tightly couple Store to Product Categories.

Possible model:

```text
Store
 │
 └── StoreCategory
```

But if Fynza requires marketplace taxonomy, a separate Category module should own taxonomy.

Store can reference category IDs.

---

# 30. Store and Product

Product Management owns products.

Relationship:

```text
Store
 │
 ▼
Product
```

Example:

```text
Product
----------------
id
storeId
sellerId
name
price
...
```

Whether both `storeId` and `sellerId` are stored should depend on the final marketplace model.

Avoid redundant ownership unless there is a clear consistency strategy.

---

# 31. Store and Inventory

Inventory belongs to Inventory Management.

```text
Store
 │
 ▼
Product
 │
 ▼
Inventory
```

Store may expose inventory availability through a query/read model.

---

# 32. Store and Order

Orders belong to Order Management.

```text
Customer
 │
 ▼
Order
 │
 └── OrderItem
       │
       ▼
     Product
       │
       ▼
      Store
```

A marketplace order can contain products from multiple stores.

Therefore, store-level order data should be derived from Order Management.

---

# 33. Store and Seller

The Store should reference the seller that owns it.

```text
Seller
 │
 └── Store
```

Recommended:

```text
Store
----------------
id
sellerId
storeName
slug
status
visibility
...
```

`sellerId` should be indexed.

---

# 34. Store Data Model

## Store

```text
Store
-------------------------
id
publicId
sellerId
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
```

---

# 35. Store Settings

```text
StoreSetting
-------------------------
id
storeId
currency
timezone
language
orderNotifications
customerNotifications
createdAt
updatedAt
```

---

# 36. Store Policy

```text
StorePolicy
-------------------------
id
storeId
type
title
content
version
status
effectiveFrom
createdAt
updatedAt
```

---

# 37. Store Status History

```text
StoreStatusHistory
-------------------------
id
storeId
previousStatus
newStatus
reason
changedBy
createdAt
expiresAt
```

---

# 38. Store Slug History

If slugs can change:

```text
StoreSlugHistory
-------------------------
id
storeId
slug
createdAt
expiresAt
```

This allows:

```text
old-store-name
       │
       ▼
redirect
       │
       ▼
new-store-name
```

---

# 39. API Design

## REST

```text
GET    /stores/{slug}
GET    /stores/{id}

POST   /sellers/me/stores
GET    /sellers/me/stores
GET    /sellers/me/stores/{id}

PATCH  /sellers/me/stores/{id}
DELETE /sellers/me/stores/{id}

PATCH  /sellers/me/stores/{id}/status
PATCH  /sellers/me/stores/{id}/visibility

GET    /sellers/me/stores/{id}/settings
PATCH  /sellers/me/stores/{id}/settings

GET    /sellers/me/stores/{id}/policies
POST   /sellers/me/stores/{id}/policies
PATCH  /sellers/me/stores/{id}/policies/{policyId}
```

Administrative:

```text
GET  /admin/stores
GET  /admin/stores/{id}

POST /admin/stores/{id}/suspend
POST /admin/stores/{id}/activate
POST /admin/stores/{id}/close
```

---

# 40. GraphQL

## Queries

```graphql
store(id: ID!)
storeBySlug(slug: String!)
stores(filter: StoreFilterInput)
myStores
storeStatistics(storeId: ID!)
```

## Mutations

```graphql
createStore
updateStore
updateStoreSettings

publishStore
pauseStore
resumeStore

updateStoreVisibility

createStorePolicy
updateStorePolicy
deleteStorePolicy

suspendStore
activateStore
closeStore
```

---

# 41. GraphQL Structure

```text
store/
└── graphql/
    ├── resolver/
    │   ├── StoreResolver
    │   ├── StorePolicyResolver
    │   └── StoreSettingsResolver
    │
    ├── input/
    │   ├── CreateStoreInput
    │   ├── UpdateStoreInput
    │   ├── StoreFilterInput
    │   ├── StorePolicyInput
    │   └── StoreSettingsInput
    │
    └── payload/
        ├── StorePayload
        ├── StoreListPayload
        ├── StorePolicyPayload
        └── StoreStatisticsPayload
```

---

# 42. Authorization

Seller-scoped permissions:

```text
store.read.own
store.create.own
store.update.own
store.delete.own
store.publish.own
store.pause.own
store.manage.own
```

Administrative permissions:

```text
store.read
store.manage
store.suspend
store.activate
store.close
```

---

# 43. Ownership Check

Every seller store mutation must verify:

```text
Authenticated User
       │
       ▼
Seller Membership
       │
       ▼
Store Ownership / Scope
       │
       ▼
Permission
       │
       ▼
Business Policy
       │
       ▼
ALLOW
```

Do not rely only on:

```text
storeId
```

provided by the client.

---

# 44. Public Store Access

Public access may not require authentication.

```text
Anonymous User
      │
      ▼
GET /stores/{slug}
      │
      ▼
Visibility Check
      │
      ▼
PUBLIC?
      │
      ▼
Return Store
```

Private or suspended stores must be handled according to marketplace policy.

---

# 45. Store Events

Recommended events:

```text
STORE_CREATED
STORE_UPDATED
STORE_PUBLISHED
STORE_PAUSED
STORE_RESUMED
STORE_SUSPENDED
STORE_ACTIVATED
STORE_CLOSED
STORE_ARCHIVED

STORE_VISIBILITY_CHANGED

STORE_POLICY_CREATED
STORE_POLICY_UPDATED
STORE_POLICY_DELETED

STORE_SLUG_CHANGED
```

---

# 46. Event Example

```text
Seller
 │
 ▼
Create Store
 │
 ▼
STORE_CREATED
 │
 ├──► Search Index
 ├──► Analytics
 ├──► Notification
 └──► Audit
```

Publishing events allows future services to react without tightly coupling the Store module.

---

# 47. Security Requirements

## STORE-NFR-001

Users must only manage stores they own or are authorized to manage.

## STORE-NFR-002

Store administrative actions require explicit permissions.

## STORE-NFR-003

Private seller information must not leak through public store APIs.

## STORE-NFR-004

Store media must use controlled storage access.

## STORE-NFR-005

Store policies must be protected against unauthorized modification.

## STORE-NFR-006

Store status changes must be audited.

---

# 48. Performance

Recommended indexes:

```text
Store
├── sellerId
├── slug UNIQUE
├── status
├── visibility
└── createdAt

StorePolicy
├── storeId
├── type
└── status

StoreStatusHistory
├── storeId
└── createdAt
```

Public store lookup by slug should be highly optimized.

---

# 49. Caching

Public store information is a strong caching candidate.

Possible cache:

```text
store:{slug}
```

Cache:

- Store name.
- Description.
- Logo.
- Banner.
- Public settings.
- Store status.
- Public policies.

Invalidate cache on:

```text
STORE_UPDATED
STORE_VISIBILITY_CHANGED
STORE_SUSPENDED
STORE_ACTIVATED
STORE_CLOSED
```

---

# 50. Testing

## 50.1 Unit Tests

Test:

- Store creation.
- Store name validation.
- Slug generation.
- Slug uniqueness.
- Status transitions.
- Visibility rules.
- Policy management.
- Default settings.
- Ownership rules.

---

# 51. Integration Tests

Test:

```text
Seller
 │
 ▼
Store
 │
 ├── StoreSetting
 ├── StorePolicy
 └── StoreStatusHistory
```

Also test integration with:

- Storage.
- Seller.
- Authorization.
- Search.
- Event infrastructure.

---

# 52. Security Tests

Test:

- Seller A accessing Seller B's store.
- Seller A modifying Seller B's store.
- Staff without permission modifying a store.
- Suspended seller modifying store.
- Unauthorized store publication.
- Unauthorized status changes.
- Private store information exposure.
- Slug-based IDOR.

---

# 53. E2E Tests

### Store Creation

```text
Seller
 │
 ▼
Create Store
 │
 ▼
Enter Store Information
 │
 ▼
Save
 │
 ▼
DRAFT
```

### Store Publishing

```text
DRAFT
 │
 ▼
Complete Required Information
 │
 ▼
Publish
 │
 ▼
ACTIVE + PUBLIC
```

### Store Pause

```text
ACTIVE
 │
 ▼
Pause Store
 │
 ▼
PAUSED
 │
 ▼
Resume
 │
 ▼
ACTIVE
```

---

# 54. Architecture

Recommended module structure:

```text
store/
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
└── StoreModule.java
```

Tests:

```text
test/
└── store/
    ├── service/
    ├── repository/
    ├── policy/
    ├── controller/
    ├── graphql/
    ├── integration/
    └── security/
```

---

# 55. Dependencies

```text
Store
│
├── Authentication
├── User Management
├── Authorization
├── Seller Management
├── Common
├── Storage / Media
└── Event Infrastructure
```

Future consumers:

```text
Store
 │
 ├── Product
 ├── Inventory
 ├── Order
 ├── Review
 ├── Search
 ├── Analytics
 └── Notification
```

---

# 56. User Stories

## US-001 — Create Store

> As a seller, I want to create a store so that customers can discover my business.

## US-002 — Store Profile

> As a seller, I want to manage my store profile so that customers see accurate information.

## US-003 — Store URL

> As a seller, I want my store to have a unique URL so that customers can easily access it.

## US-004 — Store Branding

> As a seller, I want to add my logo and banner so that my storefront reflects my brand.

## US-005 — Store Policies

> As a seller, I want to define my store policies so that customers understand my business rules.

## US-006 — Pause Store

> As a seller, I want to temporarily pause my store so that customers know when I am unavailable.

## US-007 — Public Store

> As a customer, I want to browse a seller's store so that I can discover their products.

## US-008 — Store Administration

> As an administrator, I want to suspend stores so that I can enforce marketplace policies.

---

# 57. Milestones

## M1 — Store Foundation

- [ ] Store entity.
- [ ] Seller relationship.
- [ ] Store number/public ID.
- [ ] Store name.
- [ ] Store slug.
- [ ] Store lifecycle.

## M2 — Store Profile

- [ ] Description.
- [ ] Logo.
- [ ] Banner.
- [ ] Contact information.
- [ ] Public profile.

## M3 — Store Configuration

- [ ] Store settings.
- [ ] Store visibility.
- [ ] Store policies.
- [ ] Store pause/resume.

## M4 — Storefront

- [ ] Public store page.
- [ ] Store query.
- [ ] Product integration.
- [ ] Review integration.
- [ ] Store statistics.
- [ ] Read models/database views.

## M5 — Administration

- [ ] Store search.
- [ ] Store management.
- [ ] Store suspension.
- [ ] Store activation.
- [ ] Store closure.
- [ ] Audit.

## M6 — Optimization

- [ ] Store caching.
- [ ] Search indexing.
- [ ] Domain events.
- [ ] Analytics integration.
- [ ] Performance optimization.

---

# 58. Definition of Done

- [ ] Store entity implemented.
- [ ] Seller relationship implemented.
- [ ] Store creation implemented.
- [ ] Store name implemented.
- [ ] Store slug implemented.
- [ ] Store lifecycle implemented.
- [ ] Store visibility implemented.
- [ ] Store profile implemented.
- [ ] Store branding implemented.
- [ ] Store settings implemented.
- [ ] Store policies implemented.
- [ ] Public storefront implemented.
- [ ] Seller ownership authorization implemented.
- [ ] Administrative controls implemented.
- [ ] Store events implemented.
- [ ] Audit implemented.
- [ ] Read models implemented where required.
- [ ] REST API implemented.
- [ ] GraphQL API implemented.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] E2E tests pass.
- [ ] API documentation completed.

---

# 59. Recommended Domain Relationship

The first six modules now form a clean identity-to-business foundation:

```text
01 Authentication
       │
       ▼
02 User Management
       │
       ├───────────────┐
       ▼               ▼
03 Authorization    04 Customer
                       │
                       │
                       └─────────── Customer Domain

02 User Management
       │
       ▼
05 Seller Management
       │
       ▼
06 Store Management
       │
       ├── Product
       ├── Inventory
       ├── Orders
       ├── Reviews
       └── Analytics
```

---

# 60. Architectural Principle

The Store module represents the **commercial storefront**, not the seller.

Keep these boundaries:

```text
┌─────────────────────────────────────────┐
│                 USER                    │
│ Identity / Generic Profile              │
└───────────────────┬─────────────────────┘
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
     CUSTOMER              SELLER
                              │
                              ▼
                            STORE
                              │
               ┌──────────────┼──────────────┐
               ▼              ▼              ▼
            PRODUCT       INVENTORY         ORDER
               │                             │
               └──────────────┬──────────────┘
                              ▼
                           PAYMENT
```

### Core Rule

> **Seller Management answers "who is selling?" Store Management answers "where and how are they presented to customers?"**

This distinction prevents the Seller entity from becoming a **God Object** and gives Fynza a clean foundation for the next domain: **`07 — Product Management PRD`**.