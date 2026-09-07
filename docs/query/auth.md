# Auth — GraphQL Operations Reference

**Endpoint:** `POST /graphql`  
**Content-Type:** `application/json`  
**Auth header:** `Authorization: Bearer <accessToken>` — required on all 🔒 operations.

---

## Table of Contents

1. [Fragments](#1-fragments)
2. [Core Auth](#2-core-auth)
3. [Email Verification](#3-email-verification)
4. [Password Recovery](#4-password-recovery)
5. [Session Management](#5-session-management)
6. [MFA — Setup](#6-mfa--setup)
7. [MFA — Login Challenge](#7-mfa--login-challenge)
8. [Social / OAuth2 Accounts](#8-social--oauth2-accounts)
9. [Usage Notes](#9-usage-notes)

---

## 1. Fragments

Reusable fragments to keep operations DRY.

```graphql
fragment AuthUserFields on AuthUser {
  id
  email
  username
  firstName
  lastName
  role
}

# Full payload — tokens present on normal login / register / refresh
fragment AuthPayloadFull on AuthPayload {
  accessToken
  refreshToken
  tokenType
  expiresIn
  mfaRequired
  mfaChallengeToken
  user {
    ...AuthUserFields
  }
}

# Slim payload — only MFA fields (when mfaRequired = true)
fragment AuthPayloadMfa on AuthPayload {
  mfaRequired
  mfaChallengeToken
}
```

---

## 2. Core Auth

### Register

Creates a new account. Tokens are returned immediately. A verification email is sent in the background.

```graphql
mutation Register($input: RegisterInput!) {
  register(input: $input) {
    ...AuthPayloadFull
  }
}
```

**Variables**

```json
{
  "input": {
    "firstName": "Ada",
    "lastName": "Lovelace",
    "email": "ada@example.com",
    "password": "MinLength8!",
    "phone": "+233201234567",
    "role": "CUSTOMER"
  }
}
```

> `role` is optional — defaults to `CUSTOMER`. Pass `"SELLER"` to register a seller account.

---

### Login

```graphql
mutation Login($input: LoginInput!) {
  login(input: $input) {
    ...AuthPayloadFull
  }
}
```

**Variables**

```json
{
  "input": {
    "email": "ada@example.com",
    "password": "MinLength8!"
  }
}
```

> **Important:** check `data.login.mfaRequired` before storing tokens.  
> If `true`, tokens are `null` — store `mfaChallengeToken` and proceed to [VerifyMfa](#7-mfa--login-challenge).

---

### Refresh Token

Issues a brand-new token pair. The old `refreshToken` is invalidated immediately (rotation).

```graphql
mutation RefreshToken($input: RefreshTokenInput!) {
  refreshToken(input: $input) {
    ...AuthPayloadFull
  }
}
```

**Variables**

```json
{
  "input": {
    "refreshToken": "eyJ..."
  }
}
```

---

### Logout 🔒

Invalidates the session associated with the given refresh token.

```graphql
mutation Logout($input: RefreshTokenInput!) {
  logout(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "refreshToken": "eyJ..."
  }
}
```

---

## 3. Email Verification

### Verify Email

Parse the `token` query param from the verification link and POST it here.

```graphql
mutation VerifyEmail($input: VerifyEmailInput!) {
  verifyEmail(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "token": "<token from ?token= query param>"
  }
}
```

---

### Resend Verification Email

```graphql
mutation ResendVerification($input: ResendVerificationInput!) {
  resendVerification(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "email": "ada@example.com"
  }
}
```

---

## 4. Password Recovery

### Forgot Password

Triggers a reset email. Always returns `true` — the response never reveals whether the email exists.

```graphql
mutation ForgotPassword($input: ForgotPasswordInput!) {
  forgotPassword(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "email": "ada@example.com"
  }
}
```

---

### Reset Password

Consume the token from the reset link and set a new password. All active sessions are invalidated on success — redirect the user to login.

```graphql
mutation ResetPassword($input: ResetPasswordInput!) {
  resetPassword(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "token": "<token from ?token= query param>",
    "newPassword": "NewPassword1!",
    "confirmPassword": "NewPassword1!"
  }
}
```

---

### Change Password 🔒

Changes the password while authenticated. All active sessions are invalidated on success — redirect the user to login.

```graphql
mutation ChangePassword($input: ChangePasswordInput!) {
  changePassword(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "currentPassword": "OldPassword1!",
    "newPassword": "NewPassword1!",
    "confirmPassword": "NewPassword1!"
  }
}
```

---

## 5. Session Management

All operations in this section require authentication 🔒.

### List Active Sessions

Pass `currentRefreshToken` to flag which entry is the current device (`current: true`).

```graphql
query ActiveSessions($currentRefreshToken: String) {
  activeSessions(currentRefreshToken: $currentRefreshToken) {
    sessionId
    deviceName
    ipAddress
    createdAt
    lastActivityAt
    expiresAt
    current
  }
}
```

**Variables**

```json
{
  "currentRefreshToken": "eyJ..."
}
```

**Response**

```json
{
  "data": {
    "activeSessions": [
      {
        "sessionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "deviceName": "Desktop",
        "ipAddress": "196.0.0.1",
        "createdAt": "2026-09-05T10:00:00Z",
        "lastActivityAt": "2026-09-05T11:30:00Z",
        "expiresAt": "2026-09-12T10:00:00Z",
        "current": true
      }
    ]
  }
}
```

---

### Revoke a Session

```graphql
mutation RevokeSession($input: RevokeSessionInput!) {
  revokeSession(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "sessionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  }
}
```

---

### Revoke All Other Sessions

Keeps the current device logged in; invalidates all other sessions.

```graphql
mutation RevokeOtherSessions($input: RevokeOtherSessionsInput!) {
  revokeOtherSessions(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "refreshToken": "eyJ...currentRefreshToken"
  }
}
```

---

## 6. MFA — Setup

All setup operations require authentication 🔒.

### Step 1 — Generate Secret

Generates a TOTP secret and a `qrCodeUri` for Google Authenticator / Authy. MFA is **not** active until Step 2 succeeds.

```graphql
mutation SetupMfa {
  setupMfa {
    secret
    qrCodeUri
    manualEntryCode
  }
}
```

**Response**

```json
{
  "data": {
    "setupMfa": {
      "secret": "JBSWY3DPEHPK3PXP",
      "qrCodeUri": "otpauth://totp/Fynza:ada%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=Fynza&digits=6&period=30",
      "manualEntryCode": "JBSWY3DPEHPK3PXP"
    }
  }
}
```

> Render `qrCodeUri` as a QR image using any QR library (`qrcode`, `react-qr-code`, etc.).  
> Show `manualEntryCode` as a fallback for users who can't scan.

---

### Step 2 — Enable MFA

Confirm setup with the first 6-digit code from the authenticator app. MFA is active after this call.

```graphql
mutation EnableMfa($input: MfaEnableInput!) {
  enableMfa(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "totpCode": "123456"
  }
}
```

---

### Disable MFA 🔒

Requires a valid TOTP code to prevent accidental or malicious disabling. The secret is erased on success.

```graphql
mutation DisableMfa($input: MfaDisableInput!) {
  disableMfa(input: $input)
}
```

**Variables**

```json
{
  "input": {
    "totpCode": "123456"
  }
}
```

---

## 7. MFA — Login Challenge

This mutation is **public** — it is called after `Login` returns `mfaRequired: true`.

### Verify MFA

Exchange the challenge token and a TOTP code for real JWT tokens. The challenge token expires in 5 minutes.

```graphql
mutation VerifyMfa($input: MfaVerifyInput!) {
  verifyMfa(input: $input) {
    ...AuthPayloadFull
  }
}
```

**Variables**

```json
{
  "input": {
    "challengeToken": "<mfaChallengeToken from login response>",
    "totpCode": "123456"
  }
}
```

---

## 8. Social / OAuth2 Accounts

### List Linked Accounts 🔒

```graphql
query LinkedAccounts {
  linkedAccounts {
    provider
    displayName
    email
    avatarUrl
    linkedAt
  }
}
```

**Response**

```json
{
  "data": {
    "linkedAccounts": [
      {
        "provider": "google",
        "displayName": "Ada Lovelace",
        "email": "ada@gmail.com",
        "avatarUrl": "https://lh3.googleusercontent.com/...",
        "linkedAt": "2026-09-05T10:00:00Z"
      }
    ]
  }
}
```

---

### Unlink a Social Account 🔒

```graphql
mutation UnlinkSocialAccount($provider: String!) {
  unlinkSocialAccount(provider: $provider)
}
```

**Variables**

```json
{
  "provider": "google"
}
```

> Accepted values: `"google"` · `"github"` · `"facebook"`  
> Returns an error if this is the user's only linked provider. They must set a password via the forgot-password flow first.

---

## 9. Usage Notes

### MFA Login Flow

```
Login
 ├── mfaRequired: false  →  store accessToken + refreshToken  →  done
 └── mfaRequired: true   →  store mfaChallengeToken
                               └── VerifyMfa  →  store accessToken + refreshToken  →  done
```

### MFA Setup Flow

```
SetupMfa   →  render QR code, show manualEntryCode as fallback
               └── user scans with authenticator app
                    └── EnableMfa (first code)  →  MFA active
```

### Token Refresh Strategy

| Token | Lifetime |
|---|---|
| Access token | 15 minutes |
| Refresh token | 7 days |
| MFA challenge token | 5 minutes |

Proactively refresh before the access token expires. On any `UNAUTHENTICATED` error, attempt `RefreshToken` once. If that also fails, redirect to login. Each refresh issues a brand-new pair — the old refresh token is immediately invalidated.

### OAuth2 — Browser Redirect (Not GraphQL)

OAuth2 social login is a browser redirect flow, not a GraphQL mutation:

```
Navigate browser to → /oauth2/authorization/google
                       /oauth2/authorization/github
                       /oauth2/authorization/facebook

After provider auth → API sets HttpOnly cookies and redirects back to frontend
                      Use linkedAccounts query to see which providers are connected
```

### Error Shape

GraphQL errors appear in the top-level `errors` array with an `extensions` object:

```json
{
  "errors": [
    {
      "message": "Invalid TOTP code",
      "extensions": {
        "status": 400,
        "reason": "BAD_REQUEST"
      }
    }
  ]
}
```

Display `errors[0].message` directly to the user — it is always human-readable.
