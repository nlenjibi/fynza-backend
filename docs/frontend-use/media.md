# Media & Image Storage API — Frontend Integration Guide

Base paths: `/v1/media` (upload lifecycle + asset management) · `/v1/products/{productId}/media` (product attachment mutations) · `POST /graphql` (all reads)  
All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**  
> Use REST only to upload, delete, or reorder media. Use GraphQL for all reads (asset details, product galleries, storage quota).  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [Upload Flow Overview](#3-upload-flow-overview)
4. [Initiate an Upload (REST)](#4-initiate-an-upload-rest)
5. [PUT the File to the Presigned URL](#5-put-the-file-to-the-presigned-url)
6. [Complete an Upload (REST)](#6-complete-an-upload-rest)
7. [Cancel an Upload (REST)](#7-cancel-an-upload-rest)
8. [Delete a Media Asset (REST)](#8-delete-a-media-asset-rest)
9. [Admin — Delete Any Asset (REST)](#9-admin--delete-any-asset-rest)
10. [Generate a Signed Download URL (REST)](#10-generate-a-signed-download-url-rest)
11. [Attach Media to a Product (REST)](#11-attach-media-to-a-product-rest)
12. [Detach Media from a Product (REST)](#12-detach-media-from-a-product-rest)
13. [Reorder Product Media (REST)](#13-reorder-product-media-rest)
14. [Error Reference](#14-error-reference)
15. [Quick Reference](#15-quick-reference)

---

## 1. Standard Response Shapes

### REST envelope

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

`data` is `null` for void endpoints (cancel, delete, detach, reorder). `message` is always safe to display.

---

## 2. Reference Types

### `MediaStatus`

| Value | Meaning |
|---|---|
| `UPLOADING` | Session authorised; waiting for the browser to PUT the file |
| `READY` | File received and confirmed — safe to use |
| `FAILED` | Upload verification failed |
| `EXPIRED` | Session timed out before the file arrived (scheduler cleans these up) |
| `DELETING` | Soft-delete in progress |
| `DELETED` | Removed — hidden from all surfaces |

### `MediaVisibility`

| Value | Who can access the CDN URL without signing |
|---|---|
| `PUBLIC` | Everyone — CDN serves directly |
| `PRIVATE` | No public access — always requires a signed URL |
| `AUTHENTICATED` | Any authenticated user |
| `OWNER_ONLY` | Only the uploader |

### `MediaType`

| Value | Use case |
|---|---|
| `PRODUCT_IMAGE` | Product gallery image |
| `PRODUCT_VIDEO` | Product video |
| `SELLER_AVATAR` | Seller profile picture |
| `SELLER_BANNER` | Seller store banner |
| `SELLER_DOCUMENT` | Verification documents |
| `USER_AVATAR` | Buyer profile picture |
| `REVIEW_IMAGE` | Image attached to a review |
| `CATEGORY_BANNER` | Category hero image |
| `STORE_BANNER` | Store hero image |
| `PROMOTIONAL_BANNER` | Promotional campaign image |

### `MediaOwnerType`

`PRODUCT` · `VARIANT` · `SELLER` · `USER` · `REVIEW` · `CATEGORY` · `STORE` · `BANNER` · `DOCUMENT`

### TypeScript interfaces

```typescript
interface UploadSession {
  uploadId:   string;              // UUID — use for complete/cancel
  mediaId:    string;              // UUID — the media asset's public ID
  provider:   "R2" | "S3";
  method:     "PRESIGNED_PUT";
  uploadUrl:  string;              // PUT your file binary here directly
  expiresAt:  string;              // ISO-8601 UTC — session expires after this
  headers:    Record<string, string>; // Required headers for the PUT request
  objectKey:  string;
}

interface MediaAsset {
  publicId:          string;       // UUID
  ownerId:           string;       // UUID of the entity that owns this asset
  ownerType:         string;       // MediaOwnerType
  provider:          string;       // "R2" | "S3"
  originalFilename:  string;
  mimeType:          string;
  mediaType:         string;       // MediaType
  fileSize:          number;       // bytes
  width:             number | null;
  height:            number | null;
  visibility:        string;       // MediaVisibility
  status:            string;       // MediaStatus
  cdnUrl:            string | null; // null when visibility !== PUBLIC or not yet ready
  createdAt:         string;       // ISO-8601 UTC
}

interface ProductMediaAttachment {
  id:                 number;
  productId:          string;      // UUID
  mediaAssetPublicId: string;      // UUID
  sortOrder:          number;
  isPrimary:          boolean;
  altText:            string | null;
  asset:              MediaAsset;
  createdAt:          string;
}
```

---

## 3. Upload Flow Overview

Media never passes through the Fynza API server. The file goes directly from the browser to object storage (Cloudflare R2 / AWS S3) via a presigned PUT URL.

```
1. POST /v1/media/uploads         ← tell the API what you're about to upload
        │
        │  returns uploadId + presigned uploadUrl + required headers
        ▼
2. PUT  <uploadUrl>               ← browser sends the binary directly to R2/S3
        │                            (no Fynza token needed — URL carries its own auth)
        ▼
3. POST /v1/media/uploads/{uploadId}/complete  ← confirm the upload succeeded
        │
        │  returns the final MediaAsset (status: READY, cdnUrl populated)
        ▼
4. (optional) POST /v1/products/{productId}/media  ← attach the asset to a product
```

The presigned URL expires — default 10 minutes, max 60 minutes. If the browser does not PUT within that window, the scheduler marks the session `EXPIRED` and soft-deletes the associated asset automatically. Always complete the upload before showing the asset to users.

---

## 4. Initiate an Upload (REST)

Creates an upload session and returns a presigned PUT URL. The asset is created in `UPLOADING` status.

```
POST /v1/media/uploads
Authorization: Bearer <accessToken>   ← permission: media.upload
Content-Type: application/json
```

**Request body**

```json
{
  "filename":    "product-front.jpg",
  "contentType": "image/jpeg",
  "size":        204800,
  "width":       1200,
  "height":      1200,
  "mediaType":   "PRODUCT_IMAGE",
  "ownerType":   "PRODUCT",
  "ownerId":     "9a3c1d2e-...",
  "visibility":  "PUBLIC"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `filename` | Yes | non-blank | Original filename; used for content-disposition |
| `contentType` | Yes | non-blank | MIME type — e.g. `image/jpeg`, `image/webp`, `video/mp4` |
| `size` | Yes | positive integer | File size in bytes; validated against quota |
| `width` | No | 1–8000 | Image pixel width |
| `height` | No | 1–8000 | Image pixel height |
| `mediaType` | Yes | `MediaType` enum | See §2 |
| `ownerType` | Yes | `MediaOwnerType` enum | See §2 |
| `ownerId` | Yes | UUID | The entity this media belongs to (product ID, seller ID, etc.) |
| `visibility` | No | `MediaVisibility` enum | Defaults to `PUBLIC` if omitted |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Upload session created",
  "data": {
    "uploadId":  "b3a8f2c1-...",
    "mediaId":   "d7e9f0a2-...",
    "provider":  "R2",
    "method":    "PRESIGNED_PUT",
    "uploadUrl": "https://r2.example.com/fynza-media/product/uuid/original.jpg?X-Amz-Signature=...",
    "expiresAt": "2026-09-14T10:10:00Z",
    "headers": {
      "Content-Type": "image/jpeg"
    },
    "objectKey": "product/9a3c1d2e/b3a8f2c1/original.jpg"
  }
}
```

Store `uploadId` and `mediaId`. You need `uploadId` for the complete/cancel calls, and `mediaId` to reference the asset in product-attach requests.

**`402 Payment Required`** — seller has exceeded their storage quota.  
**`400 Bad Request`** — missing or invalid fields; `data` contains field errors.

---

## 5. PUT the File to the Presigned URL

This is a direct browser-to-storage request — **not** a call to the Fynza API.

```
PUT <uploadUrl>
Content-Type: <value from headers["Content-Type"]>
```

Send the raw file binary as the request body. Include all headers returned in the `headers` map — omitting them will cause R2/S3 to reject the request with `403`.

**JavaScript example**

```javascript
async function uploadFile(session: UploadSession, file: File): Promise<void> {
  const response = await fetch(session.uploadUrl, {
    method: "PUT",
    headers: session.headers,   // includes Content-Type
    body: file,
  });

  if (!response.ok) {
    throw new Error(`Storage upload failed: ${response.status}`);
  }
}
```

A `200` or `204` from the storage endpoint means the binary was received. You must still call the complete endpoint to transition the asset to `READY`.

---

## 6. Complete an Upload (REST)

Verifies the upload succeeded and transitions the asset from `UPLOADING` → `READY`. The CDN URL is populated after this call.

```
POST /v1/media/uploads/{uploadId}/complete
Authorization: Bearer <accessToken>   ← permission: media.upload
Content-Type: application/json
```

**Path parameter**

| Parameter | Type | Notes |
|---|---|---|
| `uploadId` | UUID | The `uploadId` returned from initiate |

**Request body**

```json
{
  "size":     204800,
  "etag":     "\"abc123\"",
  "checksum": "sha256:abc..."
}
```

| Field | Required | Notes |
|---|---|---|
| `size` | Yes | Actual received file size in bytes |
| `etag` | No | ETag header returned by R2/S3 after the PUT |
| `checksum` | No | Optional integrity check |

**Response `200`**

```json
{
  "success": true,
  "message": "Upload completed",
  "data": {
    "publicId":         "d7e9f0a2-...",
    "ownerId":          "9a3c1d2e-...",
    "ownerType":        "PRODUCT",
    "provider":         "R2",
    "originalFilename": "product-front.jpg",
    "mimeType":         "image/jpeg",
    "mediaType":        "PRODUCT_IMAGE",
    "fileSize":         204800,
    "width":            1200,
    "height":           1200,
    "visibility":       "PUBLIC",
    "status":           "READY",
    "cdnUrl":           "https://cdn.fynza.com/product/9a3c1d2e/b3a8f2c1/original.jpg",
    "createdAt":        "2026-09-14T10:00:00Z"
  }
}
```

`cdnUrl` is non-null for `PUBLIC` assets when a CDN base URL is configured. For `PRIVATE` assets, use the [signed URL endpoint](#10-generate-a-signed-download-url-rest) to access the file.

**`404`** — session not found or already expired.  
**`403`** — you do not own this upload session.

---

## 7. Cancel an Upload (REST)

Cancels an in-progress session. The associated asset is soft-deleted.

```
POST /v1/media/uploads/{uploadId}/cancel
Authorization: Bearer <accessToken>   ← permission: media.session.cancel
```

No request body.

**Response `200`**

```json
{
  "success": true,
  "message": "Upload cancelled",
  "data": null
}
```

**`404`** — session not found.  
**`403`** — you do not own this session.

---

## 8. Delete a Media Asset (REST)

Soft-deletes an asset the authenticated user owns. The asset is removed from CDN and its record marked `DELETED`.

```
DELETE /v1/media/{publicId}
Authorization: Bearer <accessToken>   ← permission: media.delete
```

**Path parameter**

| Parameter | Type | Notes |
|---|---|---|
| `publicId` | UUID | The asset's `publicId` from the initiate/complete response |

**Response `200`**

```json
{
  "success": true,
  "message": "Media asset deleted",
  "data": null
}
```

**`404`** — asset not found.  
**`403`** — asset belongs to another user; use the admin endpoint instead.

---

## 9. Admin — Delete Any Asset (REST)

Deletes any asset regardless of ownership. Requires the `media.delete.any` permission (ADMIN role).

```
DELETE /v1/media/{publicId}/admin
Authorization: Bearer <accessToken>   ← ADMIN role required
```

**Response `200`**

```json
{
  "success": true,
  "message": "Media asset deleted by admin",
  "data": null
}
```

---

## 10. Generate a Signed Download URL (REST)

Creates a time-limited signed URL to access a private asset. Use this instead of `cdnUrl` when `visibility` is `PRIVATE`, `AUTHENTICATED`, or `OWNER_ONLY`.

```
POST /v1/media/{publicId}/signed-url
Authorization: Bearer <accessToken>   ← permission: media.url.read
Content-Type: application/json
```

**Request body**

```json
{
  "expirySeconds": 300
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `expirySeconds` | No | 1–3600 | Defaults to 600 (10 min) if omitted |

**Response `200`**

```json
{
  "success": true,
  "message": "Signed URL generated",
  "data": {
    "url":       "https://r2.example.com/fynza-media/product/...?X-Amz-Expires=300&X-Amz-Signature=...",
    "expiresAt": "2026-09-14T10:05:00Z"
  }
}
```

Do not cache the signed URL beyond its `expiresAt`. For `PUBLIC` assets with a CDN URL configured, the `cdnUrl` field on the asset is already publicly accessible — you do not need a signed URL for those.

---

## 11. Attach Media to a Product (REST)

Links an uploaded asset (status `READY`) to a product. The asset must already exist — complete the upload first.

```
POST /v1/products/{productId}/media
Authorization: Bearer <accessToken>   ← permission: media.product.attach
Content-Type: application/json
```

**Path parameter**

| Parameter | Type | Notes |
|---|---|---|
| `productId` | UUID | The product's public ID |

**Request body**

```json
{
  "mediaAssetId": "d7e9f0a2-...",
  "sortOrder":    0,
  "isPrimary":    true,
  "altText":      "Front view of the product"
}
```

| Field | Required | Constraints | Notes |
|---|---|---|---|
| `mediaAssetId` | Yes | UUID | The `publicId` / `mediaId` returned from the upload flow |
| `sortOrder` | No | integer | Display order; lower = earlier; defaults to `0` |
| `isPrimary` | No | boolean | If `true`, clears the existing primary flag on the product; defaults to `false` |
| `altText` | No | string | Accessibility description |

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Media attached to product",
  "data": {
    "id":                 42,
    "productId":          "9a3c1d2e-...",
    "mediaAssetPublicId": "d7e9f0a2-...",
    "sortOrder":          0,
    "isPrimary":          true,
    "altText":            "Front view of the product",
    "asset": {
      "publicId":   "d7e9f0a2-...",
      "mimeType":   "image/jpeg",
      "mediaType":  "PRODUCT_IMAGE",
      "status":     "READY",
      "cdnUrl":     "https://cdn.fynza.com/...",
      "createdAt":  "2026-09-14T10:00:00Z"
    },
    "createdAt": "2026-09-14T10:01:00Z"
  }
}
```

Setting a new primary image does not require a separate call — just pass `isPrimary: true` and the service clears the old primary automatically.

---

## 12. Detach Media from a Product (REST)

Removes the link between a media asset and a product. The asset itself is **not** deleted — only the product association is removed.

```
DELETE /v1/products/{productId}/media/{mediaAssetPublicId}
Authorization: Bearer <accessToken>   ← permission: media.product.detach
```

**Path parameters**

| Parameter | Type | Notes |
|---|---|---|
| `productId` | UUID | The product's public ID |
| `mediaAssetPublicId` | UUID | The asset's `publicId` |

**Response `200`**

```json
{
  "success": true,
  "message": "Media detached from product",
  "data": null
}
```

**`404`** — the product–asset association does not exist.

---

## 13. Reorder Product Media (REST)

Sets the display order of all media attached to a product in a single call. Pass the complete ordered list — any asset not included retains its existing `sortOrder`.

```
PUT /v1/products/{productId}/media/reorder
Authorization: Bearer <accessToken>   ← permission: media.product.reorder
Content-Type: application/json
```

**Request body**

```json
{
  "orderedMediaIds": [
    "d7e9f0a2-...",
    "a1b2c3d4-...",
    "e5f6a7b8-..."
  ]
}
```

The position in the array becomes the `sortOrder` (index 0 = first image shown).

**Response `200`**

```json
{
  "success": true,
  "message": "Media reordered",
  "data": null
}
```

After reordering, query `productMedia(productId: "...")` via GraphQL to refresh the UI.

---

## 14. Error Reference

| Status | When |
|---|---|
| `400 Bad Request` | Validation failure — missing required field, invalid MIME type, size exceeds limit |
| `401 Unauthorized` | Missing or expired access token — refresh and retry |
| `402 Payment Required` | Seller storage quota exceeded |
| `403 Forbidden` | Asset belongs to another user, or missing required permission |
| `404 Not Found` | Asset, session, or product–media association does not exist |

For `400` errors, `data` may contain a field-error map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "size": "must be greater than 0"
  }
}
```

**On `401`**: call `POST /v1/auth/refresh-token`. On failure, redirect to login.  
**On `402`**: display the seller's current quota usage from `myStorageQuota` (GraphQL) and prompt them to delete unused assets.  
**On `403`** from delete: the caller does not own the asset. Do not surface the admin delete endpoint to non-admins.

---

## 15. Quick Reference

```
Upload lifecycle — REST  (requires media.upload permission)
────────────────────────────────────────────────────────────
POST   /v1/media/uploads                          🔒  Initiate upload → presigned URL
POST   /v1/media/uploads/{uploadId}/complete      🔒  Confirm upload → asset READY
POST   /v1/media/uploads/{uploadId}/cancel        🔒  Cancel in-progress upload

Asset management — REST
────────────────────────────────────────────────────────────
DELETE /v1/media/{publicId}                       🔒  Delete own asset   (media.delete)
DELETE /v1/media/{publicId}/admin                 🔒  Admin: delete any   (media.delete.any)
POST   /v1/media/{publicId}/signed-url            🔒  Signed URL          (media.url.read)

Product media — REST  (seller actions)
────────────────────────────────────────────────────────────
POST   /v1/products/{productId}/media             🔒  Attach → product   (media.product.attach)
DELETE /v1/products/{productId}/media/{assetId}   🔒  Detach             (media.product.detach)
PUT    /v1/products/{productId}/media/reorder     🔒  Reorder            (media.product.reorder)

Reads — GraphQL  (see docs/query/media.md)
────────────────────────────────────────────────────────────
query mediaAsset(publicId)                        🔒  Single asset by UUID
query productMedia(productId)                         Product gallery (public)
query myStorageQuota                              🔒  Quota + usage summary
query myMediaAssets(page, size)                   🔒  Paginated asset library
query myStorageUsage                              🔒  Detailed bandwidth + storage stats
query adminMediaAssets(status, page, size)        🔒  Admin: all assets with status filter

🔒 = requires Authorization: Bearer <accessToken>
```
