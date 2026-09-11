# 02 — User Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** User Management  
**Module ID:** `USER-02`  
**Priority:** Critical  
**Status:** Planned  
**Dependency:** Authentication

---

# 1. Overview

The User Management module manages user information and account profiles after authentication has established the user's identity.

It is responsible for:

- User profiles
- Personal information
- Contact information
- Profile preferences
- Account lifecycle management
- Customer profile management
- Staff profile management
- Seller profile association
- User search and administration
- User roles and status visibility
- Account deletion requests
- Privacy-related user operations

Authentication answers:

> **Who are you?**

User Management answers:

> **What information do we maintain about you?**

---

# 2. Goals

## 2.1 Primary Goals

1. Provide users with a complete profile.
2. Allow users to manage their personal information.
3. Separate identity data from business profile data.
4. Support different user types.
5. Allow administrators to manage users.
6. Provide account lifecycle management.
7. Support privacy and account deletion workflows.
8. Provide a consistent user identity across Fynza applications.

---

# 3. Non-Goals

User Management does not own:

- Authentication credentials
- Password hashing
- Login
- Sessions
- Products
- Orders
- Payments
- Inventory
- Seller business operations
- Customer addresses as an independent domain
- Marketing campaigns

These belong to their respective modules.

---

# 4. User Architecture

The recommended structure is:

```text
User
│
├── Authentication Identity
│
├── Profile
│
├── Customer Profile
│
├── Seller Profile
│
└── Staff Profile
```

A single user can potentially have multiple platform relationships.

Example:

```text
User
 ├── Customer
 └── Seller
```

This avoids creating duplicate identities.

---

# 5. Functional Requirements

## USER-FR-001 — View Profile

Authenticated users shall be able to view their profile.

Profile information may include:

```text
firstName
lastName
displayName
email
phoneNumber
avatar
dateOfBirth
gender
language
timezone
accountStatus
createdAt
```

Sensitive authentication information must not be returned.

---

# 6. USER-FR-002 — Update Profile

Users shall be able to update editable profile information.

Example:

```text
firstName
lastName
displayName
avatar
dateOfBirth
language
timezone
```

Changes to verified email or phone numbers must use the appropriate verification workflow.

---

# 7. USER-FR-003 — Profile Picture

Users may upload a profile image.

### Requirements

- Validate file type.
- Validate file size.
- Resize/optimize images.
- Store images using object storage.
- Do not store large binary files directly in the relational database.
- Generate appropriate thumbnails.

Recommended storage:

```text
User
  │
  └── avatarUrl
          │
          ▼
       S3/Object Storage
```

---

# 8. USER-FR-004 — Display Name

Users may have:

```text
firstName
lastName
displayName
```

`displayName` can be used in areas where the full legal/personal name is unnecessary.

---

# 9. USER-FR-005 — Contact Information

Users can manage:

```text
Email
Phone Number
```

Email and phone ownership must be verified before being considered trusted.

Changes should follow:

```text
Request Change
      ↓
Verify Existing Account
      ↓
Verify New Contact
      ↓
Update
      ↓
Audit Event
```

---

# 10. USER-FR-006 — User Preferences

Users shall be able to manage preferences.

Examples:

```text
language
timezone
currency
theme
notificationPreferences
marketingPreferences
```

Preferences should be separated from the core profile where appropriate.

---

# 11. USER-FR-007 — Account Information

Users should be able to view:

```text
Account ID
Account Status
Registration Date
Email Verification Status
Phone Verification Status
```

Authentication security information should remain within Authentication.

---

# 12. USER-FR-008 — Account Deactivation

Users may request account deactivation.

### Flow

```text
Request Deactivation
        ↓
Confirm Identity
        ↓
Confirm Action
        ↓
Deactivate Account
        ↓
Revoke Sessions
        ↓
Record Audit Event
```

Deactivation should not necessarily immediately delete historical business records.

---

# 13. USER-FR-009 — Account Deletion

Users may request permanent account deletion subject to platform policies and legal/business requirements.

### Recommended flow

```text
Deletion Request
       ↓
Identity Verification
       ↓
Confirmation
       ↓
Grace Period
       ↓
Data Processing
       ↓
Anonymization / Deletion
```

Business records that must legally or operationally be retained should be anonymized where possible.

---

# 14. USER-FR-010 — User Search

Authorized administrators should be able to search users.

Search criteria:

```text
userId
email
phoneNumber
name
status
role
createdAt
```

Search must be permission-controlled.

---

# 15. USER-FR-011 — User Administration

Authorized staff can:

- View users.
- Suspend users.
- Reactivate users.
- Disable accounts.
- View account status.
- Assign roles where authorized.
- Review user activity.
- Initiate account recovery processes where appropriate.

Administrative actions must be audited.

---

# 16. USER-FR-012 — Account Suspension

Authorized administrators may suspend an account.

Example:

```text
ACTIVE
  ↓
SUSPENDED
```

A suspended user should not be able to perform restricted platform operations.

The system should record:

```text
reason
performedBy
timestamp
duration
```

---

# 17. USER-FR-013 — Account Reactivation

Authorized administrators may reactivate suspended accounts.

```text
SUSPENDED
    ↓
ACTIVE
```

The action must be audited.

---

# 18. User Types

The module should support relationships such as:

```text
Customer
Seller
Seller Staff
Admin
Support Agent
Finance Staff
Operations Staff
```

The user entity itself should remain generic.

---

# 19. Data Model

## 19.1 User

The Authentication module owns authentication-related identity fields.

User Management owns profile-related information.

Recommended conceptual model:

```text
User
-------------------------
id
publicId
email
phoneNumber
status
createdAt
updatedAt
```

Depending on the final architecture, email and phone may remain exclusively under Authentication.

---

# 20. User Profile

```text
UserProfile
-------------------------
id
userId
firstName
lastName
displayName
avatarUrl
dateOfBirth
language
timezone
currency
createdAt
updatedAt
```

The profile should not contain passwords or authentication tokens.

---

# 21. Customer Profile

Customer-specific information belongs to the Customer module.

```text
CustomerProfile
-------------------------
id
userId
customerNumber
createdAt
updatedAt
```

The Customer module can then own:

```text
Addresses
Orders
Wishlist
Cart
Reviews
Customer Preferences
```

---

# 22. Seller Profile

Seller-specific information belongs to the Seller module.

```text
SellerProfile
-------------------------
id
userId
sellerNumber
businessId
status
createdAt
updatedAt
```

Seller business data should not be placed inside `UserProfile`.

---

# 23. Staff Profile

Staff information can be represented separately.

```text
StaffProfile
-------------------------
id
userId
employeeNumber
department
position
createdAt
updatedAt
```

---

# 24. User Status

User Management may expose the account lifecycle state maintained by the appropriate identity/security component.

```text
PENDING_VERIFICATION
ACTIVE
SUSPENDED
LOCKED
DISABLED
DELETED
```

The ownership of authentication/security status should remain clearly defined to avoid conflicting sources of truth.

---

# 25. API Design

## REST

```text
GET    /users/me
PATCH  /users/me

GET    /users/me/profile
PATCH  /users/me/profile

POST   /users/me/deactivate
POST   /users/me/delete-request

GET    /admin/users
GET    /admin/users/{id}

PATCH  /admin/users/{id}/suspend
PATCH  /admin/users/{id}/activate
PATCH  /admin/users/{id}/disable
```

---

# 26. GraphQL

## Queries

```graphql
me
userProfile
user(id: ID!)
users(filter: UserFilterInput)
```

## Mutations

```graphql
updateProfile
updateContactInformation

requestAccountDeactivation
requestAccountDeletion

suspendUser
activateUser
disableUser
```

---

# 27. Authorization

Every administrative operation must require explicit permissions.

Example:

```text
user.read
user.read.own
user.update.own
user.manage
user.suspend
user.activate
user.delete
user.assign_role
```

Example:

```text
Customer
 └── user.read.own
 └── user.update.own

Support Agent
 └── user.read

Admin
 └── user.manage

Super Admin
 └── user.manage
 └── user.delete
 └── user.assign_role
```

---

# 28. Privacy Requirements

Users must have control over their personal information.

The platform should support:

- Profile updates.
- Account deactivation.
- Account deletion requests.
- Data export where required.
- Privacy preferences.
- Marketing preferences.
- Consent tracking where applicable.

---

# 29. Audit Requirements

Record important user-management actions.

```text
PROFILE_CREATED
PROFILE_UPDATED
EMAIL_CHANGED
PHONE_CHANGED
AVATAR_CHANGED
ACCOUNT_DEACTIVATED
ACCOUNT_DELETION_REQUESTED
ACCOUNT_DELETED
ACCOUNT_SUSPENDED
ACCOUNT_REACTIVATED
ACCOUNT_DISABLED
ROLE_CHANGED
```

Administrative actions should include:

```text
performedBy
targetUser
action
reason
timestamp
metadata
```

---

# 30. Security Requirements

## USER-NFR-001

Users may only update their own profile unless they possess administrative permissions.

## USER-NFR-002

Administrative user operations must require authorization.

## USER-NFR-003

Sensitive profile changes may require re-authentication or MFA.

## USER-NFR-004

PII must not appear unnecessarily in logs.

## USER-NFR-005

Profile images must be validated before storage.

## USER-NFR-006

User enumeration must be prevented where applicable.

## USER-NFR-007

Deletion must not bypass legal/business retention requirements.

---

# 31. Testing

## Unit Tests

Test:

- Profile validation.
- Profile update rules.
- Account lifecycle rules.
- Permission checks.
- Data transformation.
- Deletion logic.

## Integration Tests

Test:

```text
Service
   ↓
Repository
   ↓
Database
```

Use Testcontainers for database integration.

## Security Tests

Test:

- Unauthorized profile modification.
- Horizontal privilege escalation.
- Administrative privilege escalation.
- PII exposure.
- Account enumeration.
- Unauthorized deletion.
- Unauthorized suspension.

## E2E Tests

### Profile Update

```text
Login
  ↓
View Profile
  ↓
Update Profile
  ↓
Save
  ↓
Verify Updated Profile
```

### Account Deactivation

```text
Login
  ↓
Request Deactivation
  ↓
Confirm
  ↓
Account Deactivated
  ↓
Session Revoked
```

---

# 32. Architecture

Recommended module structure:

```text
user/
├── config/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
├── validator/
├── exception/
├── graphql/
│   ├── resolver/
│   ├── input/
│   └── payload/
├── policy/
└── UserModule.java
```

Tests:

```text
test/
└── user/
    ├── service/
    ├── repository/
    ├── controller/
    ├── graphql/
    ├── policy/
    └── integration/
```

---

# 33. Module Dependencies

```text
User Management
│
├── Authentication
├── Common
├── Storage
├── Notification
└── Authorization
```

Business modules should depend on User Management when they need user profile information.

```text
                    Authentication
                          │
                          ▼
                  User Management
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
         Customer       Seller       Staff
             │            │            │
             ▼            ▼            ▼
           Order       Product      Admin
```

---

# 34. Recommended Separation

A major architectural rule for Fynza should be:

```text
Authentication
    │
    └── Identity + Credentials + Sessions

User Management
    │
    └── User Profile + Account Management

Customer
    │
    └── Customer-specific behavior

Seller
    │
    └── Seller-specific behavior

Staff
    │
    └── Staff-specific behavior
```

This prevents the common mistake of creating:

```text
User
├── password
├── customerOrders
├── sellerProducts
├── addresses
├── cart
├── wishlist
├── paymentMethods
├── employeeData
└── everything else
```

The result should instead be a set of focused modules connected through IDs and well-defined application services.

---

# 35. User Stories

## US-001 — View Profile

> As a user, I want to view my profile so that I can see the information associated with my account.

## US-002 — Update Profile

> As a user, I want to update my profile so that my information remains current.

## US-003 — Change Contact Information

> As a user, I want to update my contact information so that Fynza can communicate with me.

## US-004 — Manage Account

> As a user, I want to deactivate my account so that I can stop using the platform.

## US-005 — Delete Account

> As a user, I want to request account deletion so that I can control my personal data.

## US-006 — Manage Users

> As an administrator, I want to search and manage users so that I can operate the platform effectively.

## US-007 — Suspend User

> As an authorized administrator, I want to suspend an account so that abusive or policy-violating accounts can be restricted.

---

# 36. Milestones

## M1 — User Profile

- [ ] User profile entity.
- [ ] View profile.
- [ ] Update profile.
- [ ] Profile validation.
- [ ] Avatar support.

## M2 — Contact Management

- [ ] Email change workflow.
- [ ] Phone change workflow.
- [ ] Verification integration.
- [ ] Contact audit events.

## M3 — Account Management

- [ ] Account deactivation.
- [ ] Account deletion request.
- [ ] Data retention handling.
- [ ] Account recovery integration.

## M4 — Administration

- [ ] User search.
- [ ] User details.
- [ ] Suspend user.
- [ ] Reactivate user.
- [ ] Disable user.
- [ ] Administrative audit trail.

## M5 — Privacy

- [ ] Data export.
- [ ] Privacy preferences.
- [ ] Consent management.
- [ ] Data deletion/anonymization.

---

# 37. Definition of Done

- [ ] User profile can be created.
- [ ] User can view profile.
- [ ] User can update profile.
- [ ] Profile validation is implemented.
- [ ] Avatar upload works securely.
- [ ] Email/phone changes use verification.
- [ ] Account deactivation works.
- [ ] Account deletion workflow works.
- [ ] User administration works.
- [ ] Authorization is enforced.
- [ ] Audit events are implemented.
- [ ] Privacy requirements are addressed.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] GraphQL tests pass.
- [ ] E2E tests pass.
- [ ] API documentation is complete.

---

# 38. Architectural Principle

The Fynza identity architecture should follow:

```text
                    ┌──────────────────┐
                    │  Authentication   │
                    │                  │
                    │ Who are you?     │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │ User Management  │
                    │                  │
                    │ Who is this user?│
                    │ Profile/account  │
                    └────────┬─────────┘
                             │
             ┌───────────────┼────────────────┐
             ▼               ▼                ▼
       ┌───────────┐   ┌───────────┐   ┌───────────┐
       │ Customer  │   │  Seller   │   │   Staff   │
       │           │   │           │   │           │
       └───────────┘   └───────────┘   └───────────┘
```

**Core rule:**

> Authentication owns identity security. User Management owns the user profile and account lifecycle. Business modules own business-specific user relationships and behavior.