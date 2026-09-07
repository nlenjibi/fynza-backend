# 01 — Authentication PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Authentication  
**Module ID:** `AUTH-01`  
**Priority:** Critical  
**Status:** Planned

---

## 1. Overview

The Authentication module provides secure identity and access management for the Fynza platform.

It is responsible for:

- User registration
- Login and logout
- Session management
- Email verification
- Phone verification
- Password management
- Password recovery
- Multi-factor authentication
- OAuth/social authentication
- Account lifecycle management
- Authentication auditing
- Authorization foundations
- Protection against authentication attacks

Authentication is a **shared platform module**. Business modules must not implement their own authentication logic.

---

## 2. Goals

### 2.1 Primary Goals

1. Provide secure account creation and login.
2. Protect customer and business data.
3. Support multiple user types.
4. Provide reliable session/token management.
5. Support account recovery.
6. Support email and phone verification.
7. Support MFA for privileged and sensitive accounts.
8. Provide centralized authorization foundations.
9. Maintain an audit trail of security events.
10. Make authentication reusable across the entire platform.

### 2.2 Non-Goals

Authentication does not own:

- Products
- Orders
- Payments
- Inventory
- Shipping
- Reviews
- Seller operations
- Customer business profiles
- Business-specific permissions

These belong to their respective modules.

---

# 3. User Types

The authentication system should support different platform identities.

```text
User
│
├── Customer
├── Seller
├── Seller Staff
├── Admin
├── Support Agent
├── Finance Staff
├── Operations Staff
└── Super Admin
```

Authentication determines:

> **Who is this user?**

Authorization determines:

> **What is this user allowed to do?**

---

# 4. Authentication Flow

## 4.1 Standard Login

```text
Client
  │
  ▼
Email + Password
  │
  ▼
Validate Input
  │
  ▼
Find User
  │
  ▼
Verify Password
  │
  ▼
Check Account Status
  │
  ▼
Check MFA
  │
  ▼
Create Session
  │
  ▼
Issue Access + Refresh Tokens
```

---

# 5. Functional Requirements

## AUTH-FR-001 — User Registration

Users shall be able to create an account.

### Registration Fields

```text
firstName
lastName
email
phoneNumber
password
confirmPassword
acceptTerms
```

Optional:

```text
referralCode
```

### Requirements

- Email must be unique.
- Phone number must be unique when provided.
- Password must satisfy security requirements.
- Terms and privacy policy must be accepted.
- Account must be created with an appropriate initial status.
- Email verification must be initiated.
- Registration attempts must be rate limited.

### Registration Flow

```text
Register
   │
   ▼
Validate Input
   │
   ▼
Check Duplicate Account
   │
   ▼
Hash Password
   │
   ▼
Create User
   │
   ▼
Create Verification Token
   │
   ▼
Send Verification
   │
   ▼
Return Registration Result
```

---

## AUTH-FR-002 — Login

Users shall be able to authenticate using:

```text
Email + Password
```

Future authentication methods may include:

```text
Phone + OTP
Google
Apple
Facebook
Passkey
```

### Requirements

- Validate credentials securely.
- Do not reveal whether an email exists.
- Check account status.
- Check MFA requirements.
- Create an authenticated session.
- Generate access and refresh tokens.
- Record successful login.
- Record failed login attempts.

### Failed Login

The system should return a generic error such as:

```text
Invalid credentials.
```

It must not disclose:

```text
Email does not exist.
```

or:

```text
Password is incorrect.
```

---

## AUTH-FR-003 — Logout

Users shall be able to terminate their authenticated session.

The system should support:

```text
Logout Current Session
Logout All Sessions
```

The relevant refresh token/session must be revoked.

---

## AUTH-FR-004 — Email Verification

Users must be able to verify their email address after registration.

### Flow

```text
Register
   │
   ▼
Generate Verification Token
   │
   ▼
Send Email
   │
   ▼
User Opens Link
   │
   ▼
Validate Token
   │
   ▼
Mark Email Verified
```

### Requirements

- Verification tokens must expire.
- Tokens must be single-use.
- Verification emails can be resent.
- Resend requests must be rate limited.
- Used tokens must not work again.
- Already verified emails should not require verification again.

---

## AUTH-FR-005 — Phone Verification

If phone authentication is enabled, users may verify their phone using OTP.

### Flow

```text
Phone Number
     │
     ▼
Generate OTP
     │
     ▼
Send OTP
     │
     ▼
User Enters OTP
     │
     ▼
Validate OTP
     │
     ▼
Phone Verified
```

### Requirements

- OTPs must expire.
- OTP attempts must be limited.
- OTP requests must be rate limited.
- OTPs must not be stored in plaintext.
- Excessive failed attempts should temporarily block further attempts.

---

## AUTH-FR-006 — Forgot Password

Users shall be able to request password recovery.

### Flow

```text
Forgot Password
      │
      ▼
Enter Email
      │
      ▼
Generate Reset Token
      │
      ▼
Send Email
      │
      ▼
Open Reset Link
      │
      ▼
Set New Password
      │
      ▼
Invalidate Relevant Sessions
```

The response must not reveal whether an account exists.

Recommended response:

```text
If an account exists for this email, a password
reset link has been sent.
```

---

## AUTH-FR-007 — Password Reset

Users can create a new password using a valid password-reset token.

### Requirements

- Token must expire.
- Token must be single-use.
- New password must meet security requirements.
- Password reset must invalidate the reset token.
- Existing sessions should be revoked after a successful reset.
- Security event must be recorded.

---

## AUTH-FR-008 — Change Password

Authenticated users shall be able to change their password.

### Input

```text
currentPassword
newPassword
confirmPassword
```

The current password must be verified before changing the password.

Sensitive accounts may additionally require MFA.

---

## AUTH-FR-009 — Multi-Factor Authentication

MFA should be supported, particularly for:

- Super Admins
- Admins
- Finance staff
- Seller administrators
- Other privileged users
- Sensitive account operations

### Recommended MFA

```text
TOTP
+
Recovery Codes
```

Potential future methods:

```text
SMS OTP
Email OTP
Passkeys
Security Keys
```

### MFA Flow

```text
Login
  │
  ▼
Credentials Valid
  │
  ▼
MFA Required?
  │
  ├── No ──► Create Session
  │
  └── Yes
       │
       ▼
    MFA Challenge
       │
       ▼
    Verify Code
       │
       ▼
    Create Session
```

---

## AUTH-FR-010 — Session Management

Users shall be able to view and manage active sessions.

Example:

```text
Current Device
Chrome / Windows
Last Active: 2 minutes ago

Mobile Device
Android
Last Active: 1 hour ago
```

Users should be able to:

- View sessions.
- Revoke a session.
- Logout all sessions.
- Identify the current session.

---

## AUTH-FR-011 — Account Status

Accounts shall support a defined lifecycle.

```text
PENDING_VERIFICATION
ACTIVE
SUSPENDED
LOCKED
DISABLED
DELETED
```

### Example Lifecycle

```text
PENDING_VERIFICATION
        │
        ▼
      ACTIVE
        │
        ├────► SUSPENDED
        │          │
        │          ▼
        │        ACTIVE
        │
        ▼
     DISABLED
```

---

## AUTH-FR-012 — Account Protection

The authentication system must protect against brute-force and credential-stuffing attacks.

Controls should include:

- Rate limiting.
- Progressive delays.
- IP throttling.
- Device/risk detection.
- Temporary account locking where appropriate.
- CAPTCHA/challenge mechanisms where necessary.

Account lockout should not be the only defense because attackers could intentionally lock legitimate users out.

---

## AUTH-FR-013 — OAuth / Social Authentication

The platform may support:

```text
Google
Apple
Facebook
```

### OAuth Flow

```text
OAuth Provider
      │
      ▼
OAuth Callback
      │
      ▼
Validate Identity
      │
      ▼
Find Existing Identity
      │
      ├── Found ──► Login
      │
      └── Not Found
              │
              ▼
          Create/Link Identity
              │
              ▼
            Login
```

OAuth identities should be linked carefully to prevent account-takeover vulnerabilities.

---

# 6. Authorization Foundation

Authentication should provide the foundation for authorization.

Recommended model:

```text
User
  │
  ▼
Roles
  │
  ▼
Permissions
```

Example:

```text
Customer
├── product.read
├── cart.manage
├── order.create
└── order.read.own

Seller
├── product.create
├── product.update
├── inventory.manage
└── order.read.store

Admin
├── user.manage
├── product.manage
├── order.manage
└── system.manage
```

The platform should eventually support:

```text
RBAC
+
Resource Ownership
+
Permission Policies
```

---

# 7. Security Audit

Authentication events must be recorded.

### Event Types

```text
USER_REGISTERED
USER_LOGIN_SUCCESS
USER_LOGIN_FAILED
USER_LOGOUT
PASSWORD_CHANGED
PASSWORD_RESET_REQUESTED
PASSWORD_RESET_COMPLETED
EMAIL_VERIFIED
PHONE_VERIFIED
MFA_ENABLED
MFA_DISABLED
MFA_FAILED
SESSION_CREATED
SESSION_REVOKED
ACCOUNT_LOCKED
ACCOUNT_SUSPENDED
ACCOUNT_DISABLED
```

### Audit Data

```text
id
userId
eventType
timestamp
ipAddress
userAgent
device
success
metadata
```

Passwords, raw tokens, OTPs, and other credentials must never be written to logs.

---

# 8. Data Model

Authentication should separate identity, credentials, sessions, and verification.

```text
User
 │
 ├── Credentials
 │
 ├── Identities
 │
 ├── Sessions
 │
 ├── Verification Tokens
 │
 ├── Password Reset Tokens
 │
 ├── MFA Credentials
 │
 └── Security Events
```

---

## 8.1 User

```text
User
-------------------------
id
publicId
email
emailVerified
phoneNumber
phoneVerified
status
lastLoginAt
createdAt
updatedAt
```

The `User` entity should contain identity information, not business-specific profile data.

---

## 8.2 Credential

```text
Credential
-------------------------
id
userId
type
passwordHash
createdAt
updatedAt
```

Passwords must never be stored as plaintext.

---

## 8.3 Identity

Used for external authentication providers.

```text
Identity
-------------------------
id
userId
provider
providerUserId
createdAt
updatedAt
```

Example:

```text
userId: 123
provider: google
providerUserId: external-provider-id
```

---

## 8.4 Session

```text
Session
-------------------------
id
userId
refreshTokenHash
deviceId
ipAddress
userAgent
createdAt
expiresAt
lastUsedAt
revokedAt
```

---

## 8.5 Verification Token

```text
VerificationToken
-------------------------
id
userId
type
tokenHash
expiresAt
usedAt
createdAt
```

Types:

```text
EMAIL_VERIFICATION
PHONE_VERIFICATION
PASSWORD_RESET
```

---

## 8.6 MFA Credential

```text
MfaCredential
-------------------------
id
userId
type
secretEncrypted
enabled
createdAt
updatedAt
```

MFA secrets must be encrypted at rest.

---

## 8.7 Security Event

```text
SecurityEvent
-------------------------
id
userId
eventType
ipAddress
userAgent
metadata
createdAt
```

---

# 9. Token Strategy

The recommended token strategy is:

```text
Access Token
+
Refresh Token
```

## Access Token

Short-lived.

Recommended range:

```text
10–15 minutes
```

## Refresh Token

Longer-lived.

Recommended configurable range:

```text
7–30 days
```

### Flow

```text
Login
  │
  ├── Access Token
  │
  └── Refresh Token
          │
          ▼
     API Requests
          │
          ▼
 Access Token Expires
          │
          ▼
 Refresh Token
          │
          ▼
 New Access Token
```

Refresh-token rotation should be implemented to reduce replay attacks.

---

# 10. Password Security

Passwords must be hashed using a modern password hashing algorithm.

Recommended:

```text
Argon2id
```

BCrypt may also be used if it is the standardized choice for the Spring Security implementation.

Never use:

```text
MD5
SHA-1
SHA-256(password)
```

as the password-storage mechanism.

### Password Requirements

The system should support:

- Minimum password length.
- Password strength validation.
- Protection against common/breached passwords.
- Password history where required.
- Rate limiting.
- Secure password recovery.

---

# 11. Rate Limiting

Rate limiting should be applied to:

```text
Login
Registration
Password Reset
OTP Requests
Email Verification
MFA Attempts
Token Refresh
```

Example configurable policies:

```text
Login
5 attempts / short time window / account + IP

Password Reset
3 requests / hour / account + IP

OTP
Limited requests / time window
```

Exact values should be configurable and adjustable based on production monitoring.

---

# 12. API Requirements

## REST API

Conceptual endpoints:

```text
POST   /auth/register
POST   /auth/login
POST   /auth/logout

POST   /auth/verify-email
POST   /auth/resend-verification

POST   /auth/forgot-password
POST   /auth/reset-password

POST   /auth/change-password

POST   /auth/mfa/enable
POST   /auth/mfa/verify
POST   /auth/mfa/disable

GET    /auth/sessions
DELETE /auth/sessions/{id}
DELETE /auth/sessions
```

---

# 13. GraphQL Requirements

Because Fynza uses GraphQL, authentication should expose appropriate GraphQL operations.

## Queries

```graphql
currentUser
sessions
```

## Mutations

```graphql
register
login
logout

verifyEmail
resendVerification

requestPasswordReset
resetPassword
changePassword

enableMfa
verifyMfa
disableMfa

revokeSession
revokeAllSessions
```

GraphQL inputs and payloads should be maintained inside the Authentication module.

---

# 14. Architecture

Recommended module structure:

```text
backend/
└── src/
    ├── main/
    │   └── java/com/fynza/
    │       │
    │       ├── common/
    │       │
    │       ├── authentication/
    │       │   ├── config/
    │       │   ├── controller/
    │       │   ├── service/
    │       │   ├── repository/
    │       │   ├── entity/
    │       │   ├── dto/
    │       │   ├── mapper/
    │       │   ├── validator/
    │       │   ├── exception/
    │       │   ├── security/
    │       │   ├── graphql/
    │       │   │   ├── resolver/
    │       │   │   ├── input/
    │       │   │   ├── payload/
    │       │   │   └── ...
    │       │   └── AuthenticationModule.java
    │       │
    │       └── ...
    │
    └── test/
        └── java/com/fynza/
            └── authentication/
                ├── service/
                ├── repository/
                ├── security/
                ├── controller/
                └── graphql/
```

---

# 15. Module Dependencies

Authentication should have minimal dependencies.

```text
Authentication
├── Common
├── Database
├── Security
├── Configuration
└── Notification/Email
```

Business modules depend on Authentication.

```text
                  Authentication
                        ▲
                        │
          ┌─────────────┼─────────────┐
          │             │             │
      Customer        Seller        Admin
          │             │             │
        Order         Product     Management
```

Avoid business dependencies such as:

```text
Authentication
    ├── Order
    ├── Product
    ├── Payment
    └── Inventory
```

Authentication should remain a foundational module.

---

# 16. Recommended Identity Separation

Do not create one giant `User` entity containing all customer, seller, and staff information.

Instead:

```text
User
│
├── Authentication Identity
│
├── Customer Profile
│
├── Seller Profile
└── Staff Profile
```

For example:

```text
User
├── id
├── email
├── status
└── credentials

CustomerProfile
├── userId
├── firstName
├── lastName
└── ...

SellerProfile
├── userId
├── businessId
└── ...

StaffProfile
├── userId
├── employeeId
└── ...
```

This keeps Authentication independent from business domains.

---

# 17. Non-Functional Requirements

## AUTH-NFR-001 — Security

Passwords must never be stored in plaintext.

## AUTH-NFR-002 — Token Security

Authentication tokens must have expiration and secure lifecycle management.

## AUTH-NFR-003 — Rate Limiting

Authentication endpoints must be rate limited.

## AUTH-NFR-004 — Auditability

Security-sensitive authentication events must be auditable.

## AUTH-NFR-005 — Privacy

Authentication errors must not leak account information.

## AUTH-NFR-006 — Transport Security

Production authentication traffic must use HTTPS.

## AUTH-NFR-007 — Cookie Security

When cookies are used, appropriate security attributes must be applied:

```text
HttpOnly
Secure
SameSite
```

## AUTH-NFR-008 — Secrets

Secrets must never be committed to source control.

Use:

```text
Environment Variables
Secret Manager
Vault
Cloud Secret Management
```

as appropriate.

## AUTH-NFR-009 — Availability

Authentication should be highly available because most platform functionality depends on it.

## AUTH-NFR-010 — Observability

Authentication metrics, logs, and security events must be observable without exposing credentials.

---

# 18. Testing Requirements

Authentication requires multiple testing layers.

```text
authentication/
├── unit tests
├── integration tests
├── security tests
├── GraphQL tests
└── end-to-end tests
```

---

## 18.1 Unit Tests

Test:

- Password validation.
- Password hashing.
- Token generation.
- Token expiration.
- Authentication services.
- Account status rules.
- MFA logic.
- Permission evaluation.
- Session lifecycle.

---

## 18.2 Integration Tests

Test:

```text
Service
   ↓
Repository
   ↓
Database
```

Use Testcontainers for database-dependent integration tests.

---

## 18.3 Security Tests

Test:

- Brute-force protection.
- Token replay.
- Expired tokens.
- Invalid tokens.
- Session revocation.
- Authorization bypass.
- MFA bypass.
- Password-reset abuse.
- OTP abuse.
- Account enumeration.
- Privilege escalation.

---

## 18.4 End-to-End Tests

### Registration

```text
Register
  ↓
Verify Email
  ↓
Login
  ↓
Access Account
  ↓
Logout
```

### Password Recovery

```text
Forgot Password
  ↓
Receive Reset
  ↓
Reset Password
  ↓
Login
```

### MFA

```text
Login
  ↓
MFA Challenge
  ↓
Verify MFA
  ↓
Authenticated
```

---

# 19. Observability

Monitor authentication metrics such as:

```text
login_success_rate
login_failure_rate
registration_rate
password_reset_rate
mfa_failure_rate
account_lockouts
token_refresh_failures
active_sessions
suspicious_login_attempts
```

Potential alerts:

```text
Large increase in failed logins
Large increase in password reset requests
Unusual MFA failures
Potential credential-stuffing activity
Unusual authentication traffic
```

---

# 20. User Stories

## US-001 — Registration

> As a customer, I want to create an account so that I can purchase products.

### Acceptance Criteria

- Valid registration creates an account.
- Duplicate email is rejected.
- Password is securely hashed.
- Verification is initiated.
- Terms acceptance is recorded.

---

## US-002 — Login

> As a user, I want to log in securely so that I can access my account.

### Acceptance Criteria

- Valid credentials authenticate successfully.
- Invalid credentials are rejected.
- Session is created.
- Authentication event is recorded.

---

## US-003 — Email Verification

> As a user, I want to verify my email so that my account can be trusted by the platform.

---

## US-004 — Password Recovery

> As a user, I want to reset my forgotten password so that I can regain access to my account.

---

## US-005 — MFA

> As an administrator, I want to enable MFA so that my account has additional protection.

---

## US-006 — Session Management

> As a user, I want to see and revoke active sessions so that I can protect my account.

---

## US-007 — Security Audit

> As a platform administrator, I want authentication events recorded so that suspicious activity can be investigated.

---

# 21. Milestones

## M1 — Core Identity

- [ ] User entity.
- [ ] Credential entity.
- [ ] Registration.
- [ ] Password hashing.
- [ ] Login.
- [ ] Logout.
- [ ] Account status.

---

## M2 — Verification & Recovery

- [ ] Email verification.
- [ ] Phone verification.
- [ ] Forgot password.
- [ ] Password reset.
- [ ] Change password.

---

## M3 — Sessions & Security

- [ ] Access tokens.
- [ ] Refresh tokens.
- [ ] Refresh-token rotation.
- [ ] Session management.
- [ ] Rate limiting.
- [ ] Account protection.
- [ ] Security audit events.

---

## M4 — MFA

- [ ] TOTP.
- [ ] Recovery codes.
- [ ] MFA enrollment.
- [ ] MFA verification.
- [ ] MFA recovery.

---

## M5 — External Identity

- [ ] Google OAuth.
- [ ] Apple OAuth.
- [ ] Additional OAuth providers.

---

## M6 — Authorization

- [ ] Roles.
- [ ] Permissions.
- [ ] Resource ownership.
- [ ] Permission policies.
- [ ] Administrative access controls.

---

# 22. Definition of Done

Authentication is considered complete when:

- [ ] Registration works.
- [ ] Login works.
- [ ] Logout works.
- [ ] Passwords are securely hashed.
- [ ] Email verification works.
- [ ] Password reset works.
- [ ] Sessions can be managed.
- [ ] Access-token lifecycle is secure.
- [ ] Refresh-token rotation is implemented.
- [ ] Rate limiting is implemented.
- [ ] MFA is implemented for privileged users.
- [ ] Authentication events are audited.
- [ ] Authorization foundations exist.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] GraphQL tests pass.
- [ ] E2E authentication flows pass.
- [ ] Credentials are not exposed in logs.
- [ ] Production secrets are securely managed.
- [ ] Monitoring is configured.
- [ ] Authentication documentation is complete.

---

# 23. Architectural Principle

The Fynza platform should maintain a strict separation:

```text
┌──────────────────────────────────────────┐
│           Authentication                 │
│                                          │
│  Who are you?                            │
│  ├── Identity                            │
│  ├── Credentials                         │
│  ├── Sessions                            │
│  ├── MFA                                 │
│  └── Verification                        │
└───────────────────┬──────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────┐
│           Authorization                  │
│                                          │
│  What can you do?                        │
│  ├── Roles                               │
│  ├── Permissions                         │
│  └── Policies                            │
└───────────────────┬──────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────┐
│           Business Modules               │
│                                          │
│  What can you do with Fynza?             │
│                                          │
│  Customer │ Seller │ Product │ Order     │
│  Payment  │ Cart   │ Inventory │ etc.    │
└──────────────────────────────────────────┘
```

This separation should be treated as a **core architectural rule** for all subsequent Fynza modules.