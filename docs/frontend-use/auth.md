# Auth API — Frontend Integration Guide

Base path: `/v1/auth`  
All requests and responses use `application/json`.

---

## Table of Contents

1. [Transport & Tokens](#1-transport--tokens)
2. [Standard Response Shape](#2-standard-response-shape)
3. [Error Handling](#3-error-handling)
4. [Registration](#4-registration)
5. [Login](#5-login)
6. [Token Refresh](#6-token-refresh)
7. [Logout](#7-logout)
8. [Email Verification](#8-email-verification)
9. [Password Recovery](#9-password-recovery)
10. [Change Password](#10-change-password)
11. [Session Management](#11-session-management)
12. [MFA — Setup & Enable](#12-mfa--setup--enable)
13. [MFA — Login Flow](#13-mfa--login-flow)
14. [MFA — Disable](#14-mfa--disable)
15. [OAuth2 / Social Login](#15-oauth2--social-login)
16. [Linked Social Accounts](#16-linked-social-accounts)
17. [Full Flow Diagrams](#17-full-flow-diagrams)

---

## 1. Transport & Tokens

### How JWT tokens are delivered

| Scenario | How |
|---|---|
| Email/password login | Response body (`accessToken`, `refreshToken`) |
| OAuth2 login | `HttpOnly` cookies (`access_token`, `refresh_token`) |

For email/password flows, store the tokens however suits your setup (memory, localStorage, or cookies). For every authenticated request, send:

```
Authorization: Bearer <accessToken>
```

> **Cookie-based clients** (OAuth2 or if you set cookies yourself): include `credentials: 'include'` in every fetch call. The browser then sends the `HttpOnly` cookie automatically.

### Token lifetimes

| Token | Lifetime |
|---|---|
| Access token | 15 minutes |
| Refresh token | 7 days |
| Email verification token | 24 hours |
| Password reset token | 15 minutes |
| MFA challenge token | 5 minutes |

### When to refresh

Refresh proactively before the access token expires, or on any `401` response. Call `POST /v1/auth/refresh-token` with the current refresh token. You get a brand-new pair — the old refresh token is immediately invalidated (rotation).

---

## 2. Standard Response Shape

Every endpoint returns:

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

`data` is `null` for void endpoints (logout, verify, etc.).

---

## 3. Error Handling

| HTTP Status | Meaning | Common causes |
|---|---|---|
| `400` | Bad request | Validation failure, wrong password, token already used |
| `401` | Unauthenticated | Missing or expired access token |
| `403` | Forbidden | Authenticated but insufficient role |
| `409` | Conflict | Email already registered |
| `422` | Unprocessable | Token expired, invalid TOTP code |

All error bodies follow the standard shape above. Display `message` directly to the user — it is already human-readable.

---

## 4. Registration

```
POST /v1/auth/register
```

### Request

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "password": "MinLength8!",
  "phone": "+233201234567",
  "role": "CUSTOMER"
}
```

`role` is optional. Accepted values: `"CUSTOMER"` (default), `"SELLER"`.

### Response `200`

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "userId": "uuid",
    "email": "ada@example.com",
    "firstName": "Ada",
    "lastName": "Lovelace",
    "role": "CUSTOMER",
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900
  }
}
```

> The account is created immediately and tokens are returned. A verification email is sent in the background. The user can use the app straight away but some features may prompt them to verify their email first.

---

## 5. Login

```
POST /v1/auth/login
```

### Request

```json
{
  "email": "ada@example.com",
  "password": "MinLength8!"
}
```

### Response — normal `200`

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": "uuid",
    "email": "ada@example.com",
    "firstName": "Ada",
    "lastName": "Lovelace",
    "role": "CUSTOMER",
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900,
    "mfaRequired": false,
    "mfaChallengeToken": null
  }
}
```

### Response — MFA required `200`

When the account has MFA enabled, **no real tokens are issued**. Instead:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "mfaRequired": true,
    "mfaChallengeToken": "a3f9c2...",
    "accessToken": null,
    "refreshToken": null
  }
}
```

Check `data.mfaRequired` after every login. If `true`, show the TOTP input and proceed to [MFA — Login Flow](#13-mfa--login-flow).

### Brute-force protection

After 5 failed attempts the account is locked for 15 minutes. The `400` message will say `"Account is locked..."`. Do not retry automatically — show the message and a countdown.

---

## 6. Token Refresh

```
POST /v1/auth/refresh-token
```

### Request

```json
{
  "refreshToken": "eyJ..."
}
```

### Response `200`

Same shape as login. Store the new `accessToken` and `refreshToken` and discard the old pair.

---

## 7. Logout

```
POST /v1/auth/logout
Authorization: Bearer <accessToken>
```

### Request

```json
{
  "refreshToken": "eyJ..."
}
```

### Response `200`

```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

Discard both tokens client-side after a successful logout. If using cookies, clear them too.

---

## 8. Email Verification

### Verify email with token

```
POST /v1/auth/verify-email
```

```json
{ "token": "<token from email link>" }
```

The link you embed in the email: `https://yourapp.com/verify-email?token=<token>`

Parse the `token` query param on the verification page and POST it here.

### Resend verification email

```
POST /v1/auth/resend-verification
```

```json
{ "email": "ada@example.com" }
```

Both endpoints return `{ "success": true, "data": null }` on success.

---

## 9. Password Recovery

### Step 1 — Request reset email

```
POST /v1/auth/forgot-password
```

```json
{ "email": "ada@example.com" }
```

Always returns `200` with a generic message regardless of whether the email exists — this prevents user enumeration. Show the same success UI whether or not you find the account.

### Step 2 — Submit new password

```
POST /v1/auth/reset-password
```

```json
{
  "token": "<token from reset link>",
  "newPassword": "NewPassword1!",
  "confirmPassword": "NewPassword1!"
}
```

The reset link in the email: `https://yourapp.com/reset-password?token=<token>`

On success, all active sessions are invalidated. Redirect the user to login.

---

## 10. Change Password

```
POST /v1/auth/change-password
Authorization: Bearer <accessToken>
```

```json
{
  "currentPassword": "OldPassword1!",
  "newPassword": "NewPassword1!",
  "confirmPassword": "NewPassword1!"
}
```

On success, all active sessions are invalidated including the current one. Clear tokens and redirect to login.

---

## 11. Session Management

All three endpoints require authentication.

### List active sessions

```
GET /v1/auth/sessions
Authorization: Bearer <accessToken>
X-Refresh-Token: <refreshToken>   ← optional, marks the current session
```

Send the `X-Refresh-Token` header so the API can flag which entry is the current device.

```json
{
  "success": true,
  "data": [
    {
      "sessionId": "uuid",
      "deviceName": "Desktop",
      "ipAddress": "196.0.0.1",
      "createdAt": "2026-09-05T10:00:00Z",
      "lastActivityAt": "2026-09-05T11:30:00Z",
      "expiresAt": "2026-09-12T10:00:00Z",
      "current": true
    }
  ]
}
```

### Revoke a specific session

```
DELETE /v1/auth/sessions/{sessionId}
Authorization: Bearer <accessToken>
```

### Revoke all other sessions (stay logged in here)

```
DELETE /v1/auth/sessions/others
Authorization: Bearer <accessToken>
```

```json
{ "refreshToken": "<current refresh token>" }
```

The current session is kept alive; all others are immediately invalidated.

---

## 12. MFA — Setup & Enable

MFA uses TOTP (Google Authenticator, Authy, 1Password, etc.).

### Step 1 — Generate secret

```
POST /v1/auth/mfa/setup
Authorization: Bearer <accessToken>
```

```json
{
  "success": true,
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qrCodeUri": "otpauth://totp/Fynza:ada%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=Fynza&digits=6&period=30",
    "manualEntryCode": "JBSWY3DPEHPK3PXP"
  }
}
```

Render the `qrCodeUri` as a QR code using any QR library (e.g. `qrcode`, `react-qr-code`). Show `manualEntryCode` as a fallback for users who can't scan.

> The secret is saved server-side but MFA is **not yet active** until the user confirms it.

### Step 2 — Confirm with first code

```
POST /v1/auth/mfa/enable
Authorization: Bearer <accessToken>
```

```json
{ "totpCode": "123456" }
```

MFA is now active. The user will be challenged on every subsequent login.

---

## 13. MFA — Login Flow

When `login` returns `mfaRequired: true`:

1. Show a TOTP code input field.
2. POST the challenge token + the 6-digit code:

```
POST /v1/auth/mfa/verify
```

```json
{
  "challengeToken": "a3f9c2...",
  "totpCode": "123456"
}
```

### Response `200`

Full auth response with real tokens — same shape as a normal login.

The challenge token is single-use and expires in 5 minutes. If it expires, the user must log in again.

---

## 14. MFA — Disable

```
POST /v1/auth/mfa/disable
Authorization: Bearer <accessToken>
```

```json
{ "totpCode": "123456" }
```

Requires a valid TOTP code from the authenticator app to prevent accidental or malicious disabling. On success, the secret is erased and MFA is off.

---

## 15. OAuth2 / Social Login

OAuth2 is a **browser redirect flow**, not a fetch/XHR call.

### Supported providers

| Provider | Initiation URL |
|---|---|
| Google | `/oauth2/authorization/google` |
| GitHub | `/oauth2/authorization/github` |
| Facebook | `/oauth2/authorization/facebook` |

### Flow

```
1. User clicks "Sign in with Google"
2. Frontend navigates to: https://api.yourapp.com/oauth2/authorization/google
3. User authenticates with Google
4. API redirects back to: https://yourapp.com  (configured via app.frontend.success-redirect)
   → access_token and refresh_token are set as HttpOnly cookies
5. Frontend reads the user's session via an authenticated call (e.g. GET /v1/users/me)
```

On failure the user is redirected to `https://yourapp.com/auth/login?error=oauth2_failure`.

### Cookie setup for cross-origin

If your frontend runs on a different domain from the API (e.g. `app.fynza.com` vs `api.fynza.com`), the cookies are `SameSite=None; Secure`. Make sure:

- All API calls use `credentials: 'include'`
- The site is served over HTTPS
- The API origin is in the CORS allowlist

For local development over HTTP, the cookies use `SameSite=Lax` — use a reverse proxy so both frontend and API share the same hostname and port.

---

## 16. Linked Social Accounts

### List connected providers

```
GET /v1/auth/social-accounts
Authorization: Bearer <accessToken>
```

```json
{
  "success": true,
  "data": [
    {
      "provider": "google",
      "displayName": "Ada Lovelace",
      "email": "ada@gmail.com",
      "avatarUrl": "https://lh3.googleusercontent.com/...",
      "linkedAt": "2026-09-05T10:00:00Z"
    }
  ]
}
```

### Unlink a provider

```
DELETE /v1/auth/social-accounts/{provider}
Authorization: Bearer <accessToken>
```

`{provider}` is lowercase: `google`, `github`, or `facebook`.

**Guard:** the API rejects the request if the user only has one linked provider and no password-based login. Show the error message — it will tell the user to set a password first via the forgot-password flow.

---

## 17. Full Flow Diagrams

### Standard login

```
POST /login
  ├─ mfaRequired: false  →  store tokens  →  done
  └─ mfaRequired: true   →  show TOTP input
                              └─ POST /mfa/verify  →  store tokens  →  done
```

### Registration + email verification

```
POST /register  →  tokens returned immediately (user can continue)
                    └─ verification email sent in background
                         └─ user clicks link  →  POST /verify-email  →  verified
```

### Password reset

```
POST /forgot-password  →  always shows "check your email" UI
                           └─ email received  →  user clicks link
                                └─ POST /reset-password  →  all sessions invalidated
                                     └─ redirect to login
```

### MFA setup

```
POST /mfa/setup   →  show QR code
                       └─ user scans with authenticator app
                            └─ POST /mfa/enable  (first code)  →  MFA active
```

### OAuth2

```
navigate to /oauth2/authorization/google
  └─ Google auth  →  API callback
       └─ HttpOnly cookies set
            └─ redirect to frontend  →  call /users/me to hydrate session
```

---

## Quick Reference

```
POST   /v1/auth/register
POST   /v1/auth/login
POST   /v1/auth/refresh-token
POST   /v1/auth/logout                    🔒
POST   /v1/auth/verify-email
POST   /v1/auth/resend-verification
POST   /v1/auth/forgot-password
POST   /v1/auth/reset-password
POST   /v1/auth/change-password           🔒
GET    /v1/auth/sessions                  🔒
DELETE /v1/auth/sessions/{sessionId}      🔒
DELETE /v1/auth/sessions/others           🔒
POST   /v1/auth/mfa/setup                 🔒
POST   /v1/auth/mfa/enable                🔒
POST   /v1/auth/mfa/disable               🔒
POST   /v1/auth/mfa/verify
GET    /v1/auth/social-accounts           🔒
DELETE /v1/auth/social-accounts/{prov}    🔒

GET    /oauth2/authorization/google       (browser redirect)
GET    /oauth2/authorization/github       (browser redirect)
GET    /oauth2/authorization/facebook     (browser redirect)

🔒 = requires Authorization: Bearer <accessToken>
```
