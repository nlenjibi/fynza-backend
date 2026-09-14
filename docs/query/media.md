# Media — GraphQL Operations Reference

**Endpoint:** `POST /graphql`  
**Content-Type:** `application/json`  
**Auth header:** `Authorization: Bearer <accessToken>` — required on all 🔒 operations.

> All data fetching for media goes through GraphQL. Use REST only to upload, delete, or reorder media.  
> See [docs/frontend-use/media.md](../frontend-use/media.md) for mutation (REST) endpoints.

---

## Table of Contents

1. [Fragments](#1-fragments)
2. [Get a Single Asset](#2-get-a-single-asset)
3. [Get Product Gallery](#3-get-product-gallery)
4. [My Asset Library](#4-my-asset-library)
5. [Storage Quota Summary](#5-storage-quota-summary)
6. [Detailed Storage Usage](#6-detailed-storage-usage)
7. [Admin — List All Assets](#7-admin--list-all-assets)
8. [Usage Notes](#8-usage-notes)

---

## 1. Fragments

```graphql
fragment MediaAssetFields on MediaAsset {
  publicId
  ownerId
  ownerType
  provider
  originalFilename
  mimeType
  mediaType
  fileSize
  width
  height
  visibility
  status
  cdnUrl
  createdAt
}

fragment ProductMediaItemFields on ProductMediaItem {
  sortOrder
  isPrimary
  altText
  mediaAsset {
    ...MediaAssetFields
  }
}

fragment MediaPageFields on MediaAssetPage {
  totalElements
  totalPages
  currentPage
  hasNextPage
}
```

---

## 2. Get a Single Asset

Returns one asset by its `publicId`. Requires `media.read` permission.

```graphql
query GetMediaAsset($publicId: ID!) {
  mediaAsset(publicId: $publicId) {
    ...MediaAssetFields
  }
}
```

**Variables**

```json
{
  "publicId": "d7e9f0a2-..."
}
```

**Response**

```json
{
  "data": {
    "mediaAsset": {
      "publicId":         "d7e9f0a2-...",
      "ownerId":          "9a3c1d2e-...",
      "ownerType":        "PRODUCT",
      "provider":         "R2",
      "originalFilename": "product-front.jpg",
      "mimeType":         "image/jpeg",
      "mediaType":        "PRODUCT_IMAGE",
      "fileSize":         "204800",
      "width":            1200,
      "height":           1200,
      "visibility":       "PUBLIC",
      "status":           "READY",
      "cdnUrl":           "https://cdn.fynza.com/product/9a3c1d2e/.../original.jpg",
      "createdAt":        "2026-09-14T10:00:00Z"
    }
  }
}
```

Returns `null` if the asset does not exist or is soft-deleted.

> **`cdnUrl` rule**: `cdnUrl` is non-null only when `visibility = PUBLIC` and a CDN base URL is configured. For all other visibility levels, generate a signed URL via `POST /v1/media/{publicId}/signed-url` before displaying the image.

---

## 3. Get Product Gallery

Returns all media attached to a product, ordered by `sortOrder` ascending. No authentication required — this is public product data.

```graphql
query ProductGallery($productId: ID!) {
  productMedia(productId: $productId) {
    ...ProductMediaItemFields
  }
}
```

**Variables**

```json
{
  "productId": "9a3c1d2e-..."
}
```

**Response**

```json
{
  "data": {
    "productMedia": [
      {
        "sortOrder": 0,
        "isPrimary": true,
        "altText":   "Front view of the blue linen shirt",
        "mediaAsset": {
          "publicId":   "d7e9f0a2-...",
          "mimeType":   "image/jpeg",
          "mediaType":  "PRODUCT_IMAGE",
          "fileSize":   "204800",
          "width":      1200,
          "height":     1200,
          "visibility": "PUBLIC",
          "status":     "READY",
          "cdnUrl":     "https://cdn.fynza.com/...",
          "createdAt":  "2026-09-14T10:00:00Z"
        }
      },
      {
        "sortOrder": 1,
        "isPrimary": false,
        "altText":   "Side angle",
        "mediaAsset": { "..." : "..." }
      }
    ]
  }
}
```

Returns an empty list `[]` if the product has no media attached.

**UI guidance**: render `sortOrder = 0` (or `isPrimary = true`) as the hero/thumbnail image. Display remaining items as the gallery carousel.

---

## 4. My Asset Library

Returns a paginated list of all active assets uploaded by the authenticated user. Requires `media.read` permission.

```graphql
query MyMediaAssets($page: Int, $size: Int) {
  myMediaAssets(page: $page, size: $size) {
    content {
      ...MediaAssetFields
    }
    ...MediaPageFields
  }
}
```

**Variables**

```json
{
  "page": 0,
  "size": 20
}
```

Default: `page=0`, `size=20`. Omit variables to use defaults.

**Response**

```json
{
  "data": {
    "myMediaAssets": {
      "content": [
        {
          "publicId":   "d7e9f0a2-...",
          "mimeType":   "image/jpeg",
          "mediaType":  "PRODUCT_IMAGE",
          "fileSize":   "204800",
          "status":     "READY",
          "cdnUrl":     "https://cdn.fynza.com/...",
          "createdAt":  "2026-09-14T10:00:00Z"
        }
      ],
      "totalElements": 34,
      "totalPages":    2,
      "currentPage":   0,
      "hasNextPage":   true
    }
  }
}
```

Only `status = READY` assets where `isActive = true` are returned. Soft-deleted, expired, and still-uploading assets are excluded.

---

## 5. Storage Quota Summary

Returns the authenticated seller's current storage usage against their quota. Requires `media.quota.read` permission.

```graphql
query MyStorageQuota {
  myStorageQuota {
    storageBytes
    objectCount
    quotaBytes
    usagePercent
  }
}
```

No variables.

**Response**

```json
{
  "data": {
    "myStorageQuota": {
      "storageBytes":  "524288000",
      "objectCount":   "34",
      "quotaBytes":    "2147483648",
      "usagePercent":  24.41
    }
  }
}
```

| Field | Notes |
|---|---|
| `storageBytes` | Total bytes currently stored (string — parse as number) |
| `objectCount` | Number of active assets |
| `quotaBytes` | Seller's maximum allowed storage (default: 2 GB) |
| `usagePercent` | `storageBytes / quotaBytes * 100`; show as a progress bar |

**UI guidance**: warn the seller when `usagePercent >= 80`. Block the upload initiation call gracefully by checking this before opening the file picker. The server also enforces the quota and returns `402` if the limit is exceeded.

---

## 6. Detailed Storage Usage

Returns extended bandwidth and per-owner storage statistics. Requires `media.quota.read` permission.

```graphql
query MyStorageUsage {
  myStorageUsage {
    ownerId
    ownerType
    storageBytes
    objectCount
    bandwidthBytes
    quotaBytes
    percentUsed
    updatedAt
  }
}
```

No variables. Returns `null` if no usage record exists yet (new seller with no uploads).

**Response**

```json
{
  "data": {
    "myStorageUsage": {
      "ownerId":        "a1b2c3d4-...",
      "ownerType":      "USER",
      "storageBytes":   "524288000",
      "objectCount":    "34",
      "bandwidthBytes": "1073741824",
      "quotaBytes":     "2147483648",
      "percentUsed":    24.41,
      "updatedAt":      "2026-09-14T09:55:00Z"
    }
  }
}
```

| Field | Notes |
|---|---|
| `bandwidthBytes` | Cumulative CDN egress since account creation |
| `percentUsed` | Same as `myStorageQuota.usagePercent` |
| `updatedAt` | Last time a media operation affected these counters |

Use this query for the seller's storage dashboard / settings page. Use `myStorageQuota` for the simpler upload-flow check.

---

## 7. Admin — List All Assets

Returns a paginated, optionally filtered list of all assets on the platform. Requires `media.admin.read` permission (ADMIN role).

```graphql
query AdminMediaAssets($status: String, $page: Int, $size: Int) {
  adminMediaAssets(status: $status, page: $page, size: $size) {
    content {
      ...MediaAssetFields
    }
    ...MediaPageFields
  }
}
```

**Variables**

```json
{
  "status": "UPLOADING",
  "page": 0,
  "size": 50
}
```

| Variable | Required | Notes |
|---|---|---|
| `status` | No | One of the `MediaStatus` enum values; omit to return all statuses |
| `page` | No | Zero-indexed; defaults to `0` |
| `size` | No | Defaults to `20` |

**Response**

```json
{
  "data": {
    "adminMediaAssets": {
      "content": [
        {
          "publicId":   "d7e9f0a2-...",
          "ownerType":  "PRODUCT",
          "mimeType":   "image/jpeg",
          "status":     "UPLOADING",
          "cdnUrl":     null,
          "createdAt":  "2026-09-14T09:50:00Z"
        }
      ],
      "totalElements": 3,
      "totalPages":    1,
      "currentPage":   0,
      "hasNextPage":   false
    }
  }
}
```

Filter by `status: "UPLOADING"` to find stuck sessions (those older than 15 minutes that the expiry scheduler may not have cleaned up yet). Use `DELETE /v1/media/{publicId}/admin` to force-delete any asset.

---

## 8. Usage Notes

### CDN vs Signed URLs

```
asset.visibility === "PUBLIC" && asset.cdnUrl !== null
  → render <img src={asset.cdnUrl} /> directly — no auth needed

asset.visibility !== "PUBLIC" || asset.cdnUrl === null
  → call POST /v1/media/{publicId}/signed-url
  → render <img src={signedUrl.url} />
  → refresh before signedUrl.expiresAt
```

Never store signed URLs in persistent state (localStorage, database). They are ephemeral and expire.

### `fileSize` is a String

GraphQL returns `fileSize`, `storageBytes`, `objectCount`, and `bandwidthBytes` as strings (the schema uses `String!` to avoid 64-bit integer precision loss in JSON). Parse them as numbers before arithmetic:

```typescript
const fileSizeBytes = parseInt(asset.fileSize, 10);
const mb = fileSizeBytes / (1024 * 1024);
```

### Product Gallery Ordering

`productMedia` returns items already sorted `sortOrder ASC`. Index 0 is the primary/thumbnail image. After calling the reorder endpoint (`PUT /v1/products/{productId}/media/reorder`), re-query `productMedia` to refresh the local order.

### Error Shape

```json
{
  "errors": [
    {
      "message": "Media asset not found",
      "extensions": {
        "status": 404
      }
    }
  ]
}
```

Display `errors[0].message` directly to the user — it is always human-readable.

### `myStorageUsage` returns null for new accounts

If the seller has never uploaded anything, `myStorageUsage` returns `null`. Treat `null` as all-zero usage:

```typescript
const usage = data.myStorageUsage ?? {
  storageBytes: "0",
  objectCount: "0",
  bandwidthBytes: "0",
  percentUsed: 0,
};
```
