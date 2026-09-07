# 04 — Customer Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Customer Management  
**Module ID:** `CUSTOMER-04`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Authentication, User Management, Authorization

---

# 1. Overview

The Customer Management module manages the customer domain of the Fynza e-commerce platform.

It represents the relationship between a platform user and their identity as a customer.

The module is responsible for:

- Customer profiles
- Customer numbers
- Customer lifecycle
- Customer preferences
- Customer addresses
- Customer account information
- Customer segmentation foundations
- Customer statistics
- Customer status
- Customer administration
- Customer activity summaries

The module does **not** own authentication.

Authentication answers:

> Who are you?

User Management answers:

> What information do we maintain about you?

Customer Management answers:

> How does this user participate in Fynza as a customer?

---

# 2. Goals

## 2.1 Primary Goals

1. Represent customers independently from authentication.
2. Allow users to become customers.
3. Maintain customer-specific information.
4. Manage customer lifecycle.
5. Support multiple addresses.
6. Support customer preferences.
7. Provide customer information to other business modules.
8. Support customer administration.
9. Provide a foundation for customer analytics.
10. Maintain clean separation between customer and seller domains.

---

# 3. Non-Goals

Customer Management does not own:

- Authentication.
- Passwords.
- Sessions.
- Roles.
- Permissions.
- Products.
- Orders.
- Payments.
- Inventory.
- Shipping execution.
- Reviews.

These belong to their respective modules.

---

# 4. Customer Architecture

The recommended relationship is:

```text
User
 │
 ▼
Customer
 │
 ├── Profile
 ├── Addresses
 ├── Preferences
 ├── Statistics
 └── Activity
```

A user may exist without being an active customer.

```text
User
 │
 ├── Customer
 │
 └── Seller
```

The same identity may eventually participate in multiple Fynza domains.

---

# 5. Customer Lifecycle

Recommended states:

```text
PROSPECT
ACTIVE
INACTIVE
SUSPENDED
BLOCKED
DELETED
```

Typical lifecycle:

```text
User
 │
 ▼
PROSPECT
 │
 ▼
ACTIVE
 │
 ├──► INACTIVE
 │
 ├──► SUSPENDED
 │
 └──► BLOCKED
```

The customer lifecycle should be separate from authentication status.

For example:

```text
Authentication Status = ACTIVE
Customer Status       = SUSPENDED
```

The user can still authenticate but may be prevented from placing orders.

---

# 6. Functional Requirements

## CUSTOMER-FR-001 — Create Customer

A verified or eligible user should be able to become a customer.

### Flow

```text
User
 │
 ▼
Customer Registration / First Purchase
 │
 ▼
Validate User
 │
 ▼
Create Customer
 │
 ▼
Assign Customer Number
 │
 ▼
ACTIVE
```

The system must prevent duplicate customer records for the same user.

---

# 7. CUSTOMER-FR-002 — Customer Number

Every customer should receive a unique customer identifier.

Example:

```text
CUS-000001
CUS-000002
CUS-000003
```

The customer number should be separate from the internal database ID.

```text
id           → internal identifier
customerNo   → public/business identifier
```

---

# 8. CUSTOMER-FR-003 — View Customer Profile

Customers should be able to view their customer profile.

Information may include:

```text
customerNumber
name
email
phone
customerStatus
memberSince
defaultAddress
preferences
```

Business-sensitive information should not be exposed unnecessarily.

---

# 9. CUSTOMER-FR-004 — Customer Preferences

Customers should be able to manage preferences.

Examples:

```text
preferredLanguage
preferredCurrency
preferredDeliveryMethod
marketingOptIn
emailNotifications
smsNotifications
pushNotifications
```

Preferences should be stored independently from authentication credentials.

---

# 10. CUSTOMER-FR-005 — Customer Addresses

Customers may have multiple addresses.

Examples:

```text
Home
Office
Work
Other
```

Each address may contain:

```text
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
```

---

# 11. Address Model

Recommended:

```text
Customer
   │
   └── Addresses
          │
          ├── Home
          ├── Office
          └── Other
```

Example:

```text
Customer
│
├── Address A
│    └── DEFAULT
│
├── Address B
│
└── Address C
```

Only one address should normally be the default for a particular address category/use case.

---

# 12. CUSTOMER-FR-006 — Add Address

Customers shall be able to add addresses.

### Requirements

- Validate required fields.
- Validate country/region.
- Normalize where possible.
- Support setting default address.
- Prevent invalid address records.

---

# 13. CUSTOMER-FR-007 — Update Address

Customers shall be able to update their own addresses.

The system must verify ownership:

```text
Current User
     │
     ▼
Customer
     │
     ▼
Address
     │
     ▼
Ownership Check
     │
     ▼
ALLOW / DENY
```

---

# 14. CUSTOMER-FR-008 — Delete Address

Customers can delete addresses that are no longer needed.

The system should prevent deletion when an address is required by an active workflow.

For example:

```text
Address
  │
  ├── Active order dependency → Restrict deletion
  │
  └── No dependency → Delete
```

Historical orders should preserve the delivery address snapshot used at the time of purchase.

---

# 15. CUSTOMER-FR-009 — Default Address

Customers can select a default address.

```text
Address A
Address B
Address C
    │
    ▼
Set Address B as Default
    │
    ▼
Address B = DEFAULT
```

Changing the default should automatically remove the default designation from the previous address.

---

# 16. CUSTOMER-FR-010 — Customer Status

Authorized staff can change customer status.

```text
ACTIVE
SUSPENDED
BLOCKED
INACTIVE
```

Example:

```text
Customer
   │
   ▼
SUSPENDED
   │
   ├── Cannot place orders
   ├── Cannot perform restricted actions
   └── May still authenticate
```

Authentication and customer status must remain separate.

---

# 17. CUSTOMER-FR-011 — Customer Suspension

Authorized administrators can suspend customers.

Reasons may include:

```text
Policy Violation
Fraud Investigation
Abuse
Chargeback Investigation
Administrative Action
```

Suspension must record:

```text
reason
performedBy
createdAt
expiresAt
```

---

# 18. CUSTOMER-FR-012 — Customer Reactivation

Authorized administrators can reactivate suspended customers.

```text
SUSPENDED
    │
    ▼
ACTIVE
```

The action must be audited.

---

# 19. CUSTOMER-FR-013 — Customer Blocking

Blocking should be reserved for severe cases.

```text
ACTIVE
   │
   ▼
BLOCKED
```

A blocked customer should be prevented from performing restricted customer operations.

The system must retain an audit record explaining the decision.

---

# 20. CUSTOMER-FR-014 — Customer Search

Authorized staff should be able to search customers.

Search criteria:

```text
customerNumber
userId
name
email
phone
status
createdAt
```

Search results should support:

- Pagination.
- Sorting.
- Filtering.
- Search.
- Export where authorized.

---

# 21. CUSTOMER-FR-015 — Customer Details

Authorized staff can view customer details.

Example:

```text
Customer
│
├── Profile
├── Status
├── Addresses
├── Orders Summary
├── Payment Summary
├── Reviews Summary
└── Activity Summary
```

The Customer module should not directly own the order/payment records.

Instead, it should consume information from those modules.

---

# 22. CUSTOMER-FR-016 — Customer Statistics

The platform may maintain customer-level statistics.

Examples:

```text
totalOrders
completedOrders
cancelledOrders
totalSpent
averageOrderValue
lastOrderDate
firstOrderDate
```

These values should preferably be derived from the Order/Payment modules rather than becoming duplicated sources of truth.

---

# 23. Customer Analytics Principle

Avoid putting business transaction data directly into the customer entity.

Bad:

```text
Customer
├── totalSpent
├── totalOrders
├── lastOrder
├── productsPurchased
└── paymentHistory
```

Better:

```text
Customer
     │
     ├── Identity
     └── Customer Profile

Order ──────────────┐
Payment ────────────┼──► Customer Analytics
Review ─────────────┤
                     │
                     ▼
              Customer Summary
```

The analytics layer can calculate or cache derived metrics.

---

# 24. Data Model

## 24.1 Customer

```text
Customer
-------------------------
id
publicId
userId
customerNumber
status
createdAt
updatedAt
```

Constraints:

```text
userId → UNIQUE
customerNumber → UNIQUE
```

---

# 25. Customer Profile

```text
CustomerProfile
-------------------------
id
customerId
firstName
lastName
displayName
createdAt
updatedAt
```

If the profile information is already managed by User Management, do not duplicate it.

The exact ownership should be finalized before implementation.

---

# 26. Customer Preference

```text
CustomerPreference
-------------------------
id
customerId
language
currency
marketingOptIn
emailNotifications
smsNotifications
pushNotifications
createdAt
updatedAt
```

---

# 27. Customer Address

```text
CustomerAddress
-------------------------
id
customerId
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
```

---

# 28. Customer Status History

Status changes should be auditable.

```text
CustomerStatusHistory
-------------------------
id
customerId
previousStatus
newStatus
reason
changedBy
createdAt
expiresAt
```

Example:

```text
ACTIVE
  ↓
SUSPENDED
  ↓
Reason: Fraud investigation
  ↓
Changed by: ADMIN-001
```

---

# 29. Customer Service

The module should expose application services such as:

```text
CustomerService
CustomerProfileService
CustomerAddressService
CustomerPreferenceService
CustomerStatusService
CustomerQueryService
```

Example operations:

```text
createCustomer()
getCustomer()
getCurrentCustomer()
updateCustomer()
addAddress()
updateAddress()
deleteAddress()
setDefaultAddress()
suspendCustomer()
activateCustomer()
blockCustomer()
```

---

# 30. API Design

## REST

```text
GET    /customers/me
PATCH  /customers/me

GET    /customers/me/addresses
POST   /customers/me/addresses
PATCH  /customers/me/addresses/{id}
DELETE /customers/me/addresses/{id}

POST   /customers/me/addresses/{id}/default

GET    /customers/me/preferences
PATCH  /customers/me/preferences
```

Administrative:

```text
GET   /admin/customers
GET   /admin/customers/{id}

PATCH /admin/customers/{id}/suspend
PATCH /admin/customers/{id}/activate
PATCH /admin/customers/{id}/block
```

---

# 31. GraphQL

## Queries

```graphql
meCustomer
customer(id: ID!)
customers(filter: CustomerFilterInput)
customerAddresses
customerPreferences
```

## Mutations

```graphql
createCustomer

updateCustomer
updateCustomerPreferences

addCustomerAddress
updateCustomerAddress
deleteCustomerAddress
setDefaultCustomerAddress

suspendCustomer
activateCustomer
blockCustomer
```

---

# 32. GraphQL Structure

```text
customer/
└── graphql/
    ├── resolver/
    │   ├── CustomerResolver
    │   ├── CustomerAddressResolver
    │   └── CustomerPreferenceResolver
    │
    ├── input/
    │   ├── UpdateCustomerInput
    │   ├── AddressInput
    │   └── CustomerFilterInput
    │
    └── payload/
        ├── CustomerPayload
        ├── AddressPayload
        └── CustomerListPayload
```

---

# 33. Authorization

Customer resources must use ownership-based authorization.

Example:

```text
customer.read.own
customer.update.own

address.read.own
address.create.own
address.update.own
address.delete.own
```

Administrative permissions:

```text
customer.read
customer.manage
customer.suspend
customer.activate
customer.block
```

---

# 34. Customer Ownership

A customer must only access their own private resources.

```text
Customer A
   │
   └── Address A
```

Customer A:

```text
Address A → ALLOW
Address B → DENY
```

This must be enforced server-side.

---

# 35. Order Integration

Orders belong to the Order module.

Relationship:

```text
Customer
   │
   └── customerId
          │
          ▼
        Order
```

Customer Management should not own the Order entity.

Example:

```text
Customer
id = CUS-001

Order
customerId = CUS-001
```

---

# 36. Payment Integration

Payments belong to the Payment module.

```text
Customer
   │
   ▼
Order
   │
   ▼
Payment
```

Customer Management can display payment summaries when authorized but should not become the payment source of truth.

---

# 37. Review Integration

Reviews belong to the Review module.

```text
Customer
   │
   ▼
Review
   │
   ▼
Product
```

The Review module validates customer ownership and purchase eligibility as required.

---

# 38. Wishlist Integration

If Fynza supports wishlists:

```text
Customer
   │
   ▼
Wishlist
   │
   └── WishlistItems
```

The Wishlist module should own wishlist behavior.

---

# 39. Notification Integration

Customer preferences may be consumed by Notification.

```text
Customer Preferences
        │
        ▼
Notification Module
        │
        ├── Email
        ├── SMS
        └── Push
```

The Customer module should not send notifications directly.

It publishes or updates the relevant preference information.

---

# 40. Events

The Customer module should publish domain events.

Examples:

```text
CUSTOMER_CREATED
CUSTOMER_UPDATED
CUSTOMER_SUSPENDED
CUSTOMER_ACTIVATED
CUSTOMER_BLOCKED
CUSTOMER_DELETED
CUSTOMER_ADDRESS_ADDED
CUSTOMER_ADDRESS_UPDATED
CUSTOMER_ADDRESS_DELETED
CUSTOMER_DEFAULT_ADDRESS_CHANGED
CUSTOMER_PREFERENCES_UPDATED
```

Example:

```text
CustomerService
      │
      ▼
Customer Suspended
      │
      ▼
Domain Event
      │
 ┌────┼─────────────┐
 ▼    ▼             ▼
Audit Notification Analytics
```

---

# 41. Audit Requirements

Record:

```text
CUSTOMER_CREATED
CUSTOMER_UPDATED
CUSTOMER_SUSPENDED
CUSTOMER_ACTIVATED
CUSTOMER_BLOCKED
CUSTOMER_DELETED
ADDRESS_CREATED
ADDRESS_UPDATED
ADDRESS_DELETED
DEFAULT_ADDRESS_CHANGED
PREFERENCE_CHANGED
```

Administrative actions should record:

```text
performedBy
targetCustomer
action
reason
timestamp
metadata
```

---

# 42. Security Requirements

## CUSTOMER-NFR-001

Customers can only access their own private information.

## CUSTOMER-NFR-002

Administrators require explicit permissions.

## CUSTOMER-NFR-003

Customer identifiers must not expose internal database IDs unnecessarily.

## CUSTOMER-NFR-004

Address access must be ownership-checked.

## CUSTOMER-NFR-005

Sensitive personal information must not be unnecessarily logged.

## CUSTOMER-NFR-006

Administrative status changes must be audited.

## CUSTOMER-NFR-007

Customer data must be protected both in transit and at rest.

---

# 43. Performance Requirements

The module should support:

- Paginated customer queries.
- Indexed customer numbers.
- Indexed user IDs.
- Indexed email/phone references where applicable.
- Efficient address retrieval.
- Efficient administrative search.

Recommended indexes:

```text
Customer
├── userId UNIQUE
├── customerNumber UNIQUE
├── status
└── createdAt

CustomerAddress
├── customerId
├── country
├── region
└── isDefault
```

---

# 44. Testing

## 44.1 Unit Tests

Test:

- Customer creation.
- Customer status transitions.
- Profile validation.
- Address validation.
- Default address logic.
- Preference management.
- Ownership rules.
- Customer number generation.

---

## 44.2 Integration Tests

Test:

```text
Customer Service
      ↓
Repository
      ↓
Database
```

Test relationships with:

```text
User
Customer
CustomerAddress
CustomerPreference
```

---

## 44.3 Security Tests

Test:

- Customer A accessing Customer B.
- Customer A modifying Customer B's address.
- Unauthorized customer suspension.
- Unauthorized customer blocking.
- Administrative privilege escalation.
- PII exposure.
- IDOR vulnerabilities.

---

## 44.4 E2E Tests

### Customer Creation

```text
Register User
     ↓
Verify Account
     ↓
Create Customer
     ↓
Customer Active
```

### Address Management

```text
Login
 ↓
Add Address
 ↓
Set Default
 ↓
Update Address
 ↓
Delete Address
```

---

# 45. Architecture

Recommended module structure:

```text
customer/
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
├── graphql/
│   ├── resolver/
│   ├── input/
│   └── payload/
└── CustomerModule.java
```

Tests:

```text
test/
└── customer/
    ├── service/
    ├── repository/
    ├── policy/
    ├── controller/
    ├── graphql/
    ├── integration/
    └── security/
```

---

# 46. Module Dependencies

```text
Customer
│
├── Authentication
├── User Management
├── Authorization
├── Common
├── Storage
└── Event System
```

Other modules consume Customer:

```text
                 Authentication
                       │
                       ▼
                 User Management
                       │
                       ▼
                    Customer
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
      Order          Review        Wishlist
        │
        ▼
     Payment
```

---

# 47. User Stories

## US-001 — Customer Account

> As a user, I want to have a customer account so that I can purchase products from Fynza.

## US-002 — Customer Profile

> As a customer, I want to view my customer information so that I know what information Fynza maintains about me.

## US-003 — Address

> As a customer, I want to save multiple addresses so that I can easily select where my orders should be delivered.

## US-004 — Default Address

> As a customer, I want to select a default address so that I do not have to enter my address repeatedly.

## US-005 — Address Management

> As a customer, I want to update or delete my addresses so that my delivery information remains accurate.

## US-006 — Preferences

> As a customer, I want to manage my communication preferences so that I control how Fynza contacts me.

## US-007 — Customer Suspension

> As an administrator, I want to suspend a customer so that I can restrict accounts that violate platform policies.

---

# 48. Milestones

## M1 — Customer Foundation

- [ ] Customer entity.
- [ ] Customer number.
- [ ] User-to-customer relationship.
- [ ] Customer creation.
- [ ] Customer status.

## M2 — Customer Profile

- [ ] Profile retrieval.
- [ ] Profile updates.
- [ ] Customer preferences.
- [ ] Customer settings.

## M3 — Address Management

- [ ] Add address.
- [ ] Update address.
- [ ] Delete address.
- [ ] Default address.
- [ ] Address validation.

## M4 — Administration

- [ ] Customer search.
- [ ] Customer details.
- [ ] Customer suspension.
- [ ] Customer activation.
- [ ] Customer blocking.
- [ ] Administrative audit.

## M5 — Integration

- [ ] Order integration.
- [ ] Payment integration.
- [ ] Review integration.
- [ ] Wishlist integration.
- [ ] Notification integration.
- [ ] Customer events.

## M6 — Analytics Foundation

- [ ] Customer statistics.
- [ ] Customer activity summary.
- [ ] Customer segmentation foundation.
- [ ] Customer analytics events.

---

# 49. Definition of Done

- [ ] Customer entity implemented.
- [ ] User-to-customer relationship implemented.
- [ ] Customer number implemented.
- [ ] Customer lifecycle implemented.
- [ ] Customer profile implemented.
- [ ] Customer preferences implemented.
- [ ] Address management implemented.
- [ ] Default address implemented.
- [ ] Ownership authorization implemented.
- [ ] Customer administration implemented.
- [ ] Suspension/blocking implemented.
- [ ] Audit events implemented.
- [ ] Domain events implemented.
- [ ] Order integration implemented.
- [ ] Payment integration implemented.
- [ ] GraphQL implemented.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] E2E tests pass.
- [ ] API documentation completed.

---

# 50. Architectural Principle

Customer Management should remain a focused business module:

```text
┌──────────────────────────────────────────┐
│            Authentication                │
│                                          │
│  Identity / Credentials / Sessions       │
└─────────────────────┬────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────┐
│           User Management                │
│                                          │
│  Generic User Profile / Account          │
└─────────────────────┬────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────┐
│          Customer Management             │
│                                          │
│  Customer Profile / Addresses /          │
│  Preferences / Customer Lifecycle        │
└─────────────────────┬────────────────────┘
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       Orders      Wishlist     Reviews
```

### Core Rule

> **Customer Management owns the customer's relationship with Fynza. It does not own authentication or unrelated business transactions.**

This gives the platform a clean foundation for the next major domain: **Seller Management**, where seller accounts, businesses, stores, seller staff, onboarding, verification, and store ownership can be modeled independently from Customer Management.