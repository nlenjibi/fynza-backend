# Reports API — Frontend Integration Guide

Base path: `/v1/admin/reports` (all endpoints) · no GraphQL resolver for this module  
All REST requests and responses use `application/json`.

> **Architecture rule**: **REST changes the system. GraphQL reads the system.**  
> The reports module is Admin-only. Report generation, scheduling, and management are all REST. There is no GraphQL resolver for reports.  
> See [auth.md](auth.md) for token acquisition and refresh.

---

## Table of Contents

1. [Standard Response Shapes](#1-standard-response-shapes)
2. [Reference Types](#2-reference-types)
3. [TypeScript Interfaces](#3-typescript-interfaces)
4. [Report Lifecycle Overview](#4-report-lifecycle-overview)
5. [Get Available Report Types (REST)](#5-get-available-report-types-rest)
6. [Generate a Report (REST)](#6-generate-a-report-rest)
7. [List All Reports (REST)](#7-list-all-reports-rest)
8. [Get Report by ID (REST)](#8-get-report-by-id-rest)
9. [Download a Report (REST)](#9-download-a-report-rest)
10. [Regenerate a Report (REST)](#10-regenerate-a-report-rest)
11. [List Scheduled Reports (REST)](#11-list-scheduled-reports-rest)
12. [Create a Scheduled Report (REST)](#12-create-a-scheduled-report-rest)
13. [Get Scheduled Report by ID (REST)](#13-get-scheduled-report-by-id-rest)
14. [Update a Scheduled Report (REST)](#14-update-a-scheduled-report-rest)
15. [Pause a Scheduled Report (REST)](#15-pause-a-scheduled-report-rest)
16. [Resume a Scheduled Report (REST)](#16-resume-a-scheduled-report-rest)
17. [Delete a Scheduled Report (REST)](#17-delete-a-scheduled-report-rest)
18. [Error Reference](#18-error-reference)
19. [Quick Reference](#19-quick-reference)

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

### Paginated envelope

```json
{
  "success": true,
  "message": "...",
  "data": {
    "content": [ ... ],
    "currentPage":    0,
    "totalPages":     3,
    "totalElements":  52,
    "pageSize":       20,
    "hasNextPage":    true,
    "hasPreviousPage": false
  }
}
```

---

## 2. Reference Types

### `ReportType`

| Value | Display name | Description |
|---|---|---|
| `SALES` | Sales Report | Detailed breakdown of all sales transactions |
| `REVENUE` | Revenue Report | Revenue breakdown by category, seller, period |
| `ORDER` | Order Report | Complete order details and status tracking |
| `SELLER_PERFORMANCE` | Seller Performance | Seller metrics, sales, ratings, activity |
| `PRODUCT_PERFORMANCE` | Product Performance | Top products, inventory, sales data |
| `REFUND` | Refund Report | Refund requests, reasons, resolution times |
| `CUSTOMER_ANALYTICS` | Customer Analytics | Customer acquisition, retention, behavior |
| `CATEGORY_PERFORMANCE` | Category Performance | Sales and revenue by category |

### `ReportFormat`

| Value | MIME type | Best for |
|---|---|---|
| `PDF` | `application/pdf` | Sharing and printing |
| `CSV` | `text/csv` | Importing into spreadsheets |
| `EXCEL` | `application/vnd.ms-excel` | Excel-compatible analysis |

### `ReportStatus`

| Value | Meaning |
|---|---|
| `PENDING` | Queued — not yet started |
| `PROCESSING` | Generating asynchronously |
| `COMPLETED` | File ready — download available |
| `FAILED` | Generation failed — see `errorMessage` |

### `ScheduleType`

| Value | Recurrence |
|---|---|
| `DAILY` | Every day at `hour:minute` |
| `WEEKLY` | On `dayOfWeek` at `hour:minute` |
| `MONTHLY` | On `dayOfMonth` at `hour:minute` |

### `ScheduleStatus`

| Value | Meaning |
|---|---|
| `ACTIVE` | Running normally |
| `PAUSED` | Suspended — can be resumed |
| `DELETED` | Permanently stopped |

---

## 3. TypeScript Interfaces

```typescript
type ReportType =
  | "SALES" | "REVENUE" | "ORDER" | "SELLER_PERFORMANCE"
  | "PRODUCT_PERFORMANCE" | "REFUND" | "CUSTOMER_ANALYTICS" | "CATEGORY_PERFORMANCE";

type ReportFormat   = "PDF" | "CSV" | "EXCEL";
type ReportStatus   = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
type ScheduleType   = "DAILY" | "WEEKLY" | "MONTHLY";
type ScheduleStatus = "ACTIVE" | "PAUSED" | "DELETED";
type DayOfWeek      = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY"
                    | "FRIDAY" | "SATURDAY" | "SUNDAY";

interface ReportResponse {
  id:                    string;         // UUID
  reportNumber:          string;         // e.g. "RPT-1726649123456"
  reportType:            ReportType;
  reportTypeDisplayName: string;
  title:                 string;
  description:           string | null;
  format:                ReportFormat;
  status:                ReportStatus;
  filePath:              string | null;  // server-side path — never display to users
  fileSize:              number | null;  // bytes
  startDate:             string | null;  // ISO-8601 UTC
  endDate:               string | null;  // ISO-8601 UTC
  createdBy:             string;         // UUID
  completedAt:           string | null;  // ISO-8601 UTC
  errorMessage:          string | null;  // present when status = FAILED
  downloadUrl:           string | null;  // use for the download action
  createdAt:             string;         // ISO-8601 UTC
  updatedAt:             string;         // ISO-8601 UTC
}

interface ReportScheduleResponse {
  id:                    string;
  scheduleName:          string;
  reportType:            ReportType;
  reportTypeDisplayName: string;
  scheduleType:          ScheduleType;
  format:                ReportFormat;
  status:                ScheduleStatus;
  cronExpression:        string;            // derived cron — display only
  dayOfWeek:             DayOfWeek | null;  // WEEKLY only
  dayOfMonth:            number | null;     // MONTHLY only (1–31)
  hour:                  number;            // 0–23
  minute:                number;            // 0–59
  startDate:             string | null;
  endDate:               string | null;
  lastRunAt:             string | null;
  nextRunAt:             string | null;
  recipients:            string[];          // email addresses
  createdBy:             string;
  createdAt:             string;
  updatedAt:             string;
}
```

---

## 4. Report Lifecycle Overview

```
Admin selects type, format, date range
        |
        |  POST /v1/admin/reports/generate
        |  status = PENDING -> PROCESSING (async)
        v
Poll for completion
        |
        |  GET /v1/admin/reports/{reportId}
        |  status: PROCESSING ... COMPLETED | FAILED
        v
Download when COMPLETED
        |
        |  GET /v1/admin/reports/{reportId}/download
        |  binary file with Content-Disposition header
        v
(Optional) Regenerate
        |
        |  POST /v1/admin/reports/{reportId}/regenerate
        |  status resets to PENDING -> PROCESSING
```

**Polling guidance**: poll `GET /v1/admin/reports/{reportId}` every **3–5 seconds** after `/generate`. Stop when `status` is `COMPLETED` or `FAILED`. Time out after 2 minutes and show an error.

---

## 5. Get Available Report Types (REST)

```
GET /v1/admin/reports/types
Authorization: Bearer <accessToken>
```

**Response `200`**

```json
{
  "success": true,
  "message": "Report types retrieved successfully",
  "data": {
    "types": [
      {
        "value": "SALES",
        "displayName": "Sales Report",
        "description": "Detailed breakdown of all sales transactions",
        "color": "GREEN"
      }
    ],
    "formats": ["PDF", "CSV", "EXCEL"]
  }
}
```

---

## 6. Generate a Report (REST)

```
POST /v1/admin/reports/generate
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request body**

```json
{
  "reportType":       "SALES",
  "format":           "PDF",
  "title":            "September 2026 Sales Summary",
  "description":      "Monthly sales report for September 2026",
  "startDate":        "2026-09-01T00:00:00",
  "endDate":          "2026-09-30T23:59:59",
  "filters":          ["status:COMPLETED"],
  "emailRecipients":  ["admin@fynza.com"]
}
```

| Field | Required | Notes |
|---|---|---|
| `reportType` | Yes | `ReportType` value |
| `format` | Yes | `ReportFormat` value |
| `title` | No | Human-readable name |
| `description` | No | Additional context |
| `startDate` | No | ISO-8601 datetime — data window start |
| `endDate` | No | ISO-8601 datetime — data window end |
| `filters` | No | Array of filter strings |
| `emailRecipients` | No | Addresses to receive the completed file |

**Response `200`** — `ReportResponse` with `status: "PENDING"`. Save `id` to poll §8 and download via §9.

---

## 7. List All Reports (REST)

```
GET /v1/admin/reports?reportType=SALES&status=COMPLETED&page=0&size=20
Authorization: Bearer <accessToken>
```

**Query parameters**: `reportType`, `status`, `dateFrom`, `dateTo`, `page` (default 0), `size` (default 20), `sortBy` (default `createdAt`), `direction` (default `DESC`).

**Response `200`** — paginated `ReportResponse[]`.

---

## 8. Get Report by ID (REST)

```
GET /v1/admin/reports/{reportId}
Authorization: Bearer <accessToken>
```

**Response `200`** — `ReportResponse` with current `status`. When `FAILED`, `errorMessage` is set. `404` if not found.

---

## 9. Download a Report (REST)

```
GET /v1/admin/reports/{reportId}/download
Authorization: Bearer <accessToken>
```

Response is raw binary (PDF/CSV/XLSX) with `Content-Disposition: attachment; filename="RPT-....pdf"`. Returns `400` plain text when `status != COMPLETED`. Use `URL.createObjectURL` to trigger a browser save.

**TypeScript download helper**

```typescript
async function downloadReport(reportId: string, token: string): Promise<void> {
  const res = await fetch(`/v1/admin/reports/${reportId}/download`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(await res.text());
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = res.headers.get("Content-Disposition")?.split("filename=")[1] ?? "report";
  a.click();
  URL.revokeObjectURL(url);
}
```

---

## 10. Regenerate a Report (REST)

```
POST /v1/admin/reports/{reportId}/regenerate
Authorization: Bearer <accessToken>
```

No body. Returns `ReportResponse` with `status: "PENDING"`. Poll §8 after calling.

---

## 11. List Scheduled Reports (REST)

```
GET /v1/admin/reports/scheduled?status=ACTIVE&page=0&size=20
Authorization: Bearer <accessToken>
```

**Query parameters**: `status`, `page`, `size`, `sortBy`, `direction`. **Response `200`** — paginated `ReportScheduleResponse[]`.

---

## 12. Create a Scheduled Report (REST)

```
POST /v1/admin/reports/scheduled
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request body**

```json
{
  "scheduleName":    "Weekly Sales Report",
  "reportType":      "SALES",
  "scheduleType":    "WEEKLY",
  "format":          "PDF",
  "dayOfWeek":       "MONDAY",
  "dayOfMonth":      null,
  "hour":            9,
  "minute":          0,
  "startDate":       null,
  "endDate":         null,
  "emailRecipients": ["admin@fynza.com"],
  "filters":         []
}
```

| Field | Required | Notes |
|---|---|---|
| `scheduleName` | Yes | Non-blank |
| `reportType` | Yes | `ReportType` |
| `scheduleType` | Yes | `DAILY`, `WEEKLY`, `MONTHLY` |
| `format` | Yes | `ReportFormat` |
| `hour` | Yes | 0–23 UTC |
| `minute` | Yes | 0–59 |
| `dayOfWeek` | Conditional | Required for `WEEKLY` |
| `dayOfMonth` | Conditional | Required for `MONTHLY` (1–31) |
| `startDate` / `endDate` | No | ISO-8601 datetime window |
| `emailRecipients` | No | Email addresses |
| `filters` | No | Filter strings |

**Response `200`** — `ReportScheduleResponse`.

---

## 13. Get Scheduled Report by ID (REST)

```
GET /v1/admin/reports/scheduled/{scheduleId}
Authorization: Bearer <accessToken>
```

**Response `200`** — `ReportScheduleResponse`. `404` if not found.

---

## 14. Update a Scheduled Report (REST)

```
PUT /v1/admin/reports/scheduled/{scheduleId}
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request body** — same shape as §12. **Response `200`** — updated `ReportScheduleResponse`.

---

## 15. Pause a Scheduled Report (REST)

```
PATCH /v1/admin/reports/scheduled/{scheduleId}/pause
Authorization: Bearer <accessToken>
```

No body. **Response `200`** — `ReportScheduleResponse` with `status: "PAUSED"` and `nextRunAt: null`.

---

## 16. Resume a Scheduled Report (REST)

```
PATCH /v1/admin/reports/scheduled/{scheduleId}/resume
Authorization: Bearer <accessToken>
```

No body. **Response `200`** — `ReportScheduleResponse` with `status: "ACTIVE"` and recalculated `nextRunAt`.

---

## 17. Delete a Scheduled Report (REST)

```
DELETE /v1/admin/reports/scheduled/{scheduleId}
Authorization: Bearer <accessToken>
```

**Response `200`** — `ReportScheduleResponse` with `status: "DELETED"`. Not reversible.

---

## 18. Error Reference

| Status | When |
|---|---|
| `400` | Validation failure or download called before `COMPLETED` |
| `401` | Missing or expired access token |
| `403` | Caller is not `ADMIN` |
| `404` | Report or schedule ID not found |

| `message` | Cause |
|---|---|
| `"Report type is required"` | `reportType` null |
| `"Format is required"` | `format` null |
| `"Schedule name is required"` | `scheduleName` blank |
| `"Schedule type is required"` | `scheduleType` null |
| `"Hour is required"` | `hour` null |
| `"Minute is required"` | `minute` null |
| `"Report is not ready for download. Status: PROCESSING"` | Download before completion |

---

## 19. Quick Reference

```
Report management — REST  (ADMIN only)
────────────────────────────────────────────────────────────────────────
GET   /v1/admin/reports/types                      🔒  Catalogue of types & formats
POST  /v1/admin/reports/generate                   🔒  Generate (async)
GET   /v1/admin/reports                            🔒  List (paginated, filterable)
GET   /v1/admin/reports/{reportId}                 🔒  Status & metadata
GET   /v1/admin/reports/{reportId}/download        🔒  Binary download
POST  /v1/admin/reports/{reportId}/regenerate      🔒  Regenerate

Schedule management — REST  (ADMIN only)
────────────────────────────────────────────────────────────────────────
GET    /v1/admin/reports/scheduled                 🔒  List
POST   /v1/admin/reports/scheduled                 🔒  Create
GET    /v1/admin/reports/scheduled/{id}            🔒  Get
PUT    /v1/admin/reports/scheduled/{id}            🔒  Update (full replace)
PATCH  /v1/admin/reports/scheduled/{id}/pause      🔒  Pause
PATCH  /v1/admin/reports/scheduled/{id}/resume     🔒  Resume
DELETE /v1/admin/reports/scheduled/{id}            🔒  Delete (permanent)

🔒 = requires Authorization: Bearer <accessToken>  (ADMIN only)
```

### Admin UI checklist

1. Poll `GET /v1/admin/reports/{id}` every 4 s after `/generate` until `status` is `COMPLETED` or `FAILED`.
2. Show the "Download" button only when `status === "COMPLETED"`.
3. When `status === "FAILED"`, display `errorMessage` and offer "Regenerate".
4. Display `fileSize` as a human-readable hint (bytes → KB/MB) before the user clicks download.
5. Never display `filePath` — use `downloadUrl` or construct `/v1/admin/reports/{id}/download`.
