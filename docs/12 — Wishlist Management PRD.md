# 12 — Wishlist Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Wishlist Management  
**Module ID:** `WISHLIST-12`  
**Priority:** High  
**Status:** Planned  
**Architecture:** Enterprise Modular Monolith / Microservice-Ready  
**Backend:** Java + Spring Boot  
**API:** REST + GraphQL  
**Frontend:** Next.js + TypeScript  

---

# 1. Executive Summary

Wishlist Management allows customers to save products they are interested in purchasing later.

A wishlist is a **save-for-later intent**, not a shopping cart and not an order.

The module supports:

- Default wishlists
- Multiple custom wishlists
- Wishlist items
- Product/variant references
- Guest wishlists
- Customer wishlist persistence
- Guest-to-customer merging
- Move/add wishlist items to cart
- Wishlist sharing
- Wishlist privacy
- Price-change detection
- Availability detection
- Restock notifications
- Price-drop notification preferences
- Wishlist analytics
- Wishlist lifecycle management

The module must remain independent from Product, Pricing, Inventory, Cart, Order, Payment, and Notification ownership.

---

# 2. Product Vision

Fynza should allow customers to save products they may want to purchase without forcing them into the cart.

The intended journey is:

```text
Browse Product
      ↓
Save to Wishlist
      ↓
Compare / Revisit Later
      ↓
Check Current Price & Availability
      ↓
Add to Cart
      ↓
Checkout
      ↓
Order
```

Wishlist therefore becomes an important bridge between **discovery** and **purchase intent**.

---

# 3. Problem Statement

Customers frequently discover products they are interested in but are not ready to purchase immediately.

Without wishlists, customers may:

- Forget products
- Lose products they discovered
- Add unnecessary products to carts
- Repeatedly search for the same products
- Miss price reductions
- Miss products returning to stock
- Have difficulty organizing products for future purchases

Sellers and Fynza also lose valuable purchase-intent signals.

Wishlist Management solves this by providing persistent and organized product saving.

---

# 4. Goals

## 4.1 Primary Goals

1. Allow customers to save products.
2. Allow customers to save specific variants.
3. Provide a default wishlist.
4. Support multiple wishlists.
5. Support guest wishlists.
6. Merge guest wishlists after authentication.
7. Allow users to remove wishlist items.
8. Allow users to move items to cart.
9. Display current price information.
10. Display current availability.
11. Support wishlist sharing.
12. Support privacy controls.
13. Support restock and price-drop preferences.
14. Provide analytics-ready events.
15. Maintain strong resource ownership and authorization.

## 4.2 Secondary Goals

- Improve customer retention.
- Improve conversion.
- Support gift-oriented use cases.
- Support product discovery.
- Provide future recommendation signals.
- Provide seller analytics without exposing customer privacy.

---

# 5. Non-Goals

Wishlist Management does **not** own:

- Products
- Product variants
- Categories
- Prices
- Discounts
- Inventory
- Carts
- Orders
- Payments
- Shipping
- Product reviews
- Notifications execution
- Customer authentication

Those responsibilities remain with their respective modules.

---

# 6. Architectural Principle

> **Wishlist Management owns customer save-for-later intent. It references products and retrieves their current state from Product, Pricing, and Inventory rather than owning those domains.**

The boundary should remain:

```text
Product
   │
   ├── Product information
   ├── Variants
   └── Catalog status
          │
          ▼
      Wishlist
          │
          ├── Save-for-later intent
          ├── Lists
          ├── Wishlist items
          └── Notification preferences
          │
          ├───────────────┐
          ▼               ▼
      Pricing         Inventory
          │               │
          └───────┬───────┘
                  ▼
               Cart
                  │
                  ▼
               Checkout
```

---

# 7. Wishlist vs Cart vs Order

This distinction is critical.

| Domain | Meaning |
|---|---|
| Product | What is being sold? |
| Pricing | How much does it cost? |
| Inventory | How many are available? |
| Wishlist | What does the customer want to remember/save? |
| Cart | What does the customer intend to purchase now? |
| Checkout | Can the intended purchase become a transaction? |
| Order | What was actually purchased? |

Example:

```text
Wishlist
└── MacBook Pro
       ↓
Customer decides to buy
       ↓
Cart
└── MacBook Pro × 1
       ↓
Checkout
       ↓
Order
└── MacBook Pro × 1
```

A wishlist item is **not a reservation**.

---

# 8. User Types

Wishlist functionality primarily targets:

### Customer

Can:

- Create wishlists
- Add products
- Remove products
- Rename lists
- Organize products
- Move products to cart
- Share wishlists
- Configure notifications

### Guest

Can:

- Create/use a temporary wishlist
- Add products
- Remove products
- View saved products
- Merge wishlist after login

### Admin

Can access limited administrative/analytics capabilities according to authorization policy.

### Seller

Normally cannot access individual customers' private wishlists.

Seller-level aggregated analytics may be supported in the future.

---

# 9. Core Use Cases

## UC-01 Save Product

Customer selects:

```text
♡ Add to Wishlist
```

The product is added to the customer's default wishlist.

---

## UC-02 Save Variant

For products with variants:

```text
Product
└── Color: Black
└── Size: Large
```

The wishlist should be able to preserve the selected variant.

---

## UC-03 Remove Product

Customer removes an item from a wishlist.

---

## UC-04 Create Custom Wishlist

Example:

```text
My Wishlists

♥ Favorites
🎁 Birthday Ideas
💻 Tech
🏠 Home
```

---

## UC-05 Move to Cart

Customer selects:

```text
Add to Cart
```

The system validates:

- Product
- Variant
- Price
- Availability
- Quantity

Then delegates cart creation/update to Cart Management.

---

## UC-06 Guest Wishlist

Guest receives an opaque wishlist token.

Example:

```text
Browser
   ↓
Guest Wishlist Token
   ↓
Wishlist
```

No sequential or predictable identifier should be exposed.

---

## UC-07 Login Merge

Guest:

```text
Guest Wishlist
├── Product A
├── Product B
└── Product C
```

Customer already has:

```text
Customer Wishlist
├── Product B
└── Product D
```

After authentication:

```text
Merged Wishlist
├── Product A
├── Product B
├── Product C
└── Product D
```

Duplicate items must not be created.

---

# 10. Functional Requirements

## FR-01 Wishlist Creation

The system shall allow an authenticated customer to create a wishlist.

Fields:

- Name
- Description
- Visibility
- Default flag

---

## FR-02 Default Wishlist

Each customer should have one default wishlist.

Recommended:

```text
Customer
   ↓
Default Wishlist
```

The default wishlist should be automatically created when appropriate.

### Rule

A customer cannot have multiple default wishlists.

---

# 11. Multiple Wishlists

Customers may create additional lists.

Example:

```text
Customer
│
├── Favorites
├── Christmas
├── Electronics
└── Home Renovation
```

Recommended limits should be configurable.

Example:

```text
FREE     → 3 wishlists
PRO      → 20 wishlists
PREMIUM  → configurable/higher limit
```

These limits should be enforced through the Subscription/Entitlement system rather than hardcoded inside Wishlist Management.

---

# 12. Wishlist Status

Recommended statuses:

```text
ACTIVE
ARCHIVED
DELETED
```

### ACTIVE

Normal operational wishlist.

### ARCHIVED

Temporarily hidden but recoverable.

### DELETED

Logical deletion state.

Historical events should remain available according to retention policies.

---

# 13. Wishlist Visibility

Supported visibility:

```text
PRIVATE
SHARED
PUBLIC
```

### PRIVATE

Only owner can access.

### SHARED

Accessible through a secure share link/token.

### PUBLIC

Can be discoverable through explicitly supported public functionality.

Default:

```text
PRIVATE
```

Privacy must be opt-in.

---

# 14. Wishlist Item

A wishlist item represents a saved product reference.

```text
WishlistItem
-------------------------
id
wishlistId
productId
variantId
addedAt
updatedAt
createdAt
```

Important:

The wishlist must **not duplicate the complete Product entity**.

It stores references:

```text
wishlistItem.productId
wishlistItem.variantId
```

Product details are resolved from Product Management.

---

# 15. Wishlist Data Model

## 15.1 Wishlist

```text
Wishlist
-------------------------
id
publicId
customerId
guestTokenHash
name
description
status
visibility
isDefault
shareTokenHash
createdAt
updatedAt
```

### Constraints

```text
publicId UNIQUE
shareTokenHash UNIQUE
```

For customer wishlists:

```text
customerId NOT NULL
```

For guest wishlists:

```text
guestTokenHash NOT NULL
```

Depending on the guest architecture, the same table can support both, or guest state can use a separate persistence model.

---

# 16. Wishlist Item Data Model

```text
WishlistItem
-------------------------
id
wishlistId
productId
variantId
createdAt
updatedAt
```

Recommended constraint:

```text
UNIQUE(
    wishlistId,
    productId,
    variantId
)
```

This prevents duplicate wishlist entries.

---

# 17. Variant Handling

Products may be:

### Simple Product

```text
Product
└── no variant
```

Wishlist item:

```text
productId = P1
variantId = null
```

### Variant Product

```text
Product
├── Variant A
├── Variant B
└── Variant C
```

Wishlist item:

```text
productId = P1
variantId = V2
```

The variant reference should remain valid according to Product lifecycle rules.

---

# 18. Product Deletion / Archiving

Wishlist should not physically duplicate product information.

If Product becomes:

```text
ARCHIVED
```

or:

```text
DELETED
```

the wishlist should return an appropriate state.

Example:

```text
Wishlist Item

MacBook Pro
Status: No longer available
```

The system may:

- retain the wishlist item temporarily
- mark it unavailable
- allow removal
- optionally automatically clean it later

Do not silently destroy customer intent without a retention policy.

---

# 19. Pricing Integration

Wishlist does not own prices.

When displaying a wishlist:

```text
Wishlist
   ↓
Product
   ↓
Pricing
```

The system can display:

```text
Current Price
Previous Observed Price
Price Changed
Price Dropped
Currency
```

Example:

```text
Product: Laptop

Saved Price: $1,200
Current Price: $999

Price dropped: $201
```

The saved price is an **observation/snapshot for comparison**, not the authoritative product price.

---

# 20. Inventory Integration

Wishlist does not own inventory.

Availability should be dynamically resolved.

Example:

```text
AVAILABLE
LOW_STOCK
OUT_OF_STOCK
UNAVAILABLE
```

Wishlist should not reserve stock.

Therefore:

```text
Wishlist
    ≠
Inventory Reservation
```

---

# 21. Price Change Detection

The system may detect:

```text
Current Price < Previously Observed Price
```

Then emit:

```text
WISHLIST_ITEM_PRICE_CHANGED
```

or a more specific internal event:

```text
WISHLIST_ITEM_PRICE_DROPPED
```

Notification delivery belongs to Notification Management.

Wishlist only determines eligibility/context.

---

# 22. Restock Detection

If a saved product was:

```text
OUT_OF_STOCK
```

and Inventory reports:

```text
AVAILABLE
```

the system can identify a restock event.

Example:

```text
Wishlist Item
       ↓
Previously unavailable
       ↓
Inventory updated
       ↓
Available
       ↓
WISHLIST_ITEM_BACK_IN_STOCK
       ↓
Notification Module
```

---

# 23. Notification Preferences

Optional preferences:

```text
WishlistNotificationPreference
--------------------------------
id
wishlistId
itemId
notifyOnPriceDrop
notifyOnRestock
createdAt
updatedAt
```

Alternatively, preferences can be attached to the wishlist item:

```text
WishlistItem
├── notifyOnPriceDrop
└── notifyOnRestock
```

For a first implementation, item-level preferences are simpler.

---

# 24. Notification Ownership

Wishlist:

> Determines what the customer wants to be notified about.

Notification module:

> Determines how the notification is delivered.

For example:

```text
Wishlist
   ↓
Price dropped
   ↓
Event
   ↓
Notification
   ├── Email
   ├── Push
   └── In-App
```

Wishlist should never contain:

```text
sendEmail()
sendPush()
```

---

# 25. Guest Wishlist

Guest users should be able to save products without creating an account.

Use:

```text
guestToken
```

The token should be:

- cryptographically random
- opaque
- sufficiently long
- stored securely
- never based on sequential IDs

Recommended storage:

```text
guestTokenHash
```

rather than storing the raw token.

---

# 26. Guest Wishlist Merge

When guest authenticates:

```text
Guest Wishlist
       +
Customer Wishlist
       ↓
Merge
```

Merge rules:

1. Match product + variant.
2. Prevent duplicates.
3. Preserve existing customer item.
4. Add missing guest items.
5. Validate maximum wishlist limits.
6. Preserve notification preferences where appropriate.
7. Apply subscription limits.
8. Record merge event.

Event:

```text
WISHLIST_MERGED
```

---

# 27. Merge Conflict

Example:

```text
Guest:
Product A

Customer:
Product A
```

Result:

```text
Product A
```

Not:

```text
Product A
Product A
```

Recommended rule:

> Customer wishlist state wins for duplicate items.

Guest-specific metadata may be merged if useful.

---

# 28. Move to Cart

The operation should be:

```text
Wishlist
   ↓
Validate Product
   ↓
Validate Variant
   ↓
Resolve Price
   ↓
Check Inventory
   ↓
Cart.addItem()
```

Wishlist should call Cart Management rather than directly manipulating Cart tables.

---

# 29. Add vs Move

Two distinct operations are recommended.

### Add to Cart

```text
Wishlist
   ↓
Add to Cart
   ↓
Wishlist remains
```

### Move to Cart

```text
Wishlist
   ↓
Add to Cart
   ↓
Remove Wishlist Item
```

Default UI action should preferably be:

```text
Add to Cart
```

because customers often want to retain the item in their wishlist.

---

# 30. Wishlist Sharing

A customer can share a wishlist.

Example:

```text
My Birthday Wishlist
        ↓
Share
        ↓
Secure Link
```

Use:

```text
shareTokenHash
```

rather than:

```text
/wishlist/12345
```

Share links must be opaque and revocable.

---

# 31. Sharing Lifecycle

```text
PRIVATE
   ↓
SHARED
   ↓
UNSHARED
   ↓
PRIVATE
```

Customer should be able to:

- Generate share link
- Revoke share link
- Disable sharing
- Change visibility
- Regenerate token

Regenerating a token invalidates the previous token.

---

# 32. Public Wishlist

If public wishlists are enabled:

```text
PUBLIC
```

means the wishlist can be viewed without authentication.

However, public access must only expose intended information.

Never expose:

- Customer email
- Internal customer ID
- Private metadata
- Security information
- Purchase history
- Internal pricing information

---

# 33. Wishlist Limits

Limits should be configurable.

Possible limits:

```text
Maximum wishlists per customer
Maximum items per wishlist
Maximum shared wishlists
Maximum guest items
```

These limits should integrate with entitlement/subscription management.

---

# 34. Wishlist Validation

Before displaying actionable information, validate:

### Product

- Exists
- Active
- Purchasable

### Variant

- Exists
- Belongs to product
- Active

### Pricing

- Current price available
- Currency valid

### Inventory

- Availability current

### Store

- Store active
- Product purchasable

---

# 35. Wishlist Summary

A wishlist response may contain:

```text
Wishlist
├── id
├── name
├── itemCount
├── visibility
├── status
└── items
      ├── product
      ├── variant
      ├── currentPrice
      ├── priceChanged
      ├── availability
      └── notifications
```

These values may come from different domains.

They should therefore be treated as a **read model**, not as duplicated domain ownership.

---

# 36. Read Model

Recommended:

```text
WishlistSummaryView
```

Potential joins:

```text
Wishlist
   ↓
WishlistItem
   ↓
Product
   ↓
ProductVariant
   ↓
Store
   ↓
Pricing
   ↓
Inventory
```

Example:

```text
WishlistSummaryView
--------------------------------
wishlistId
wishlistName
itemId
productId
variantId
productName
productImage
variantName
storeName
currentPrice
currency
availabilityStatus
```

This is a good use case for the project's database-view strategy.

---

# 37. Database View Principle

Do not turn:

```text
WishlistSummaryView
```

into a normal JPA write entity.

Use:

```text
wishlist/query/view/
```

and:

```text
wishlist/query/projection/
```

for read-only access.

Example:

```text
wishlist/
├── query/
│   ├── projection/
│   │   └── WishlistSummaryProjection.java
│   └── view/
│       └── WishlistSummaryView.java
```

Domain entities remain clean.

---

# 38. REST API

## Customer Wishlist APIs

### List Wishlists

```http
GET /api/v1/wishlists
```

### Create Wishlist

```http
POST /api/v1/wishlists
```

### Get Wishlist

```http
GET /api/v1/wishlists/{wishlistId}
```

### Update Wishlist

```http
PATCH /api/v1/wishlists/{wishlistId}
```

### Delete Wishlist

```http
DELETE /api/v1/wishlists/{wishlistId}
```

---

# 39. Wishlist Item APIs

### Add Item

```http
POST /api/v1/wishlists/{wishlistId}/items
```

Request:

```json
{
  "productId": "product-public-id",
  "variantId": "variant-public-id"
}
```

### Remove Item

```http
DELETE /api/v1/wishlists/{wishlistId}/items/{itemId}
```

### Add to Cart

```http
POST /api/v1/wishlists/{wishlistId}/items/{itemId}/add-to-cart
```

### Move to Cart

```http
POST /api/v1/wishlists/{wishlistId}/items/{itemId}/move-to-cart
```

---

# 40. Sharing APIs

### Share

```http
POST /api/v1/wishlists/{wishlistId}/share
```

### Disable Sharing

```http
DELETE /api/v1/wishlists/{wishlistId}/share
```

### Regenerate Share Token

```http
POST /api/v1/wishlists/{wishlistId}/share/regenerate
```

### Public/Shared View

```http
GET /api/v1/shared/wishlists/{shareToken}
```

---

# 41. Notification APIs

### Update Preferences

```http
PATCH /api/v1/wishlists/{wishlistId}/items/{itemId}/notifications
```

Example:

```json
{
  "notifyOnPriceDrop": true,
  "notifyOnRestock": true
}
```

---

# 42. GraphQL Queries

Recommended:

```graphql
type Query {
    wishlists: [Wishlist!]!
    wishlist(id: ID!): Wishlist
    wishlistItems(wishlistId: ID!): [WishlistItem!]!
    sharedWishlist(token: String!): Wishlist
}
```

---

# 43. GraphQL Mutations

```graphql
type Mutation {
    createWishlist(input: CreateWishlistInput!): WishlistPayload!
    updateWishlist(input: UpdateWishlistInput!): WishlistPayload!
    deleteWishlist(id: ID!): DeleteWishlistPayload!

    addWishlistItem(input: AddWishlistItemInput!): WishlistItemPayload!
    removeWishlistItem(id: ID!): WishlistItemPayload!

    addWishlistItemToCart(
        wishlistItemId: ID!
    ): AddToCartPayload!

    moveWishlistItemToCart(
        wishlistItemId: ID!
    ): MoveToCartPayload!

    shareWishlist(
        wishlistId: ID!
    ): ShareWishlistPayload!

    unshareWishlist(
        wishlistId: ID!
    ): WishlistPayload!

    updateWishlistNotificationPreference(
        input: WishlistNotificationPreferenceInput!
    ): WishlistItemPayload!
}
```

---

# 44. GraphQL Input Objects

```text
wishlist/graphql/input/
├── CreateWishlistInput
├── UpdateWishlistInput
├── AddWishlistItemInput
├── UpdateWishlistItemInput
├── WishlistFilterInput
├── ShareWishlistInput
└── WishlistNotificationPreferenceInput
```

---

# 45. GraphQL Payloads

```text
wishlist/graphql/payload/
├── WishlistPayload
├── WishlistItemPayload
├── WishlistListPayload
├── DeleteWishlistPayload
├── AddToCartPayload
├── MoveToCartPayload
└── ShareWishlistPayload
```

Payloads should provide structured errors.

Example:

```graphql
{
    wishlist {
        id
        name
    }

    errors {
        code
        message
        field
    }
}
```

---

# 46. DTO Structure

Recommended:

```text
wishlist/dto/
├── request/
│   ├── CreateWishlistRequest
│   ├── UpdateWishlistRequest
│   ├── AddWishlistItemRequest
│   └── NotificationPreferenceRequest
│
└── response/
    ├── WishlistResponse
    ├── WishlistItemResponse
    └── WishlistSummaryResponse
```

REST DTOs should not expose internal database entities.

---

# 47. Permissions

Recommended permissions:

```text
wishlist.read
wishlist.create
wishlist.update
wishlist.delete

wishlist.item.add
wishlist.item.remove

wishlist.share
wishlist.unshare

wishlist.notification.manage
```

Ownership:

```text
OWN_WISHLIST
```

must be enforced.

Having:

```text
wishlist.read
```

does not automatically allow access to every customer's wishlist.

---

# 48. Authorization

The authorization process should be:

```text
Authentication
      ↓
Permission
      ↓
Wishlist Ownership
      ↓
Resource Policy
      ↓
Allow / Deny
```

Example:

```java
wishlistPolicy.canRead(userId, wishlistId)
```

The policy verifies that the wishlist belongs to the requesting customer.

---

# 49. Security Requirements

## SEC-01 Ownership

Users must only modify their own private wishlists.

## SEC-02 Guest Security

Guest tokens must be:

- opaque
- random
- hashed
- rate limited

## SEC-03 Share Security

Share tokens must be:

- unpredictable
- revocable
- non-sequential
- optionally expirable

## SEC-04 Rate Limiting

Rate-limit:

- Wishlist creation
- Item additions
- Share token generation
- Public wishlist access

## SEC-05 Input Validation

Validate:

- Product IDs
- Variant IDs
- Wishlist names
- Descriptions
- Token formats
- Request sizes

---

# 50. Concurrency

Potential race:

```text
Request A → Add Product X
Request B → Add Product X
```

The database constraint:

```text
UNIQUE(wishlistId, productId, variantId)
```

should prevent duplicates.

The application should handle the resulting conflict gracefully.

Possible result:

```text
Item already exists
```

rather than returning a server error.

---

# 51. Idempotency

Wishlist operations should be safe against retries.

For example:

```text
POST addWishlistItem
```

may be retried due to network failure.

The unique constraint plus idempotent service behavior should ensure:

```text
One logical item
```

rather than:

```text
Duplicate items
```

---

# 52. Events

Recommended domain events:

```text
WISHLIST_CREATED
WISHLIST_UPDATED
WISHLIST_DELETED
WISHLIST_ARCHIVED

WISHLIST_ITEM_ADDED
WISHLIST_ITEM_REMOVED
WISHLIST_ITEM_MOVED_TO_CART

WISHLIST_MERGED

WISHLIST_SHARED
WISHLIST_UNSHARED

WISHLIST_ITEM_PRICE_CHANGED
WISHLIST_ITEM_BACK_IN_STOCK
```

---

# 53. Event Example

```text
WISHLIST_ITEM_ADDED
```

Payload:

```json
{
  "eventId": "event-id",
  "wishlistId": "wishlist-id",
  "customerId": "customer-id",
  "productId": "product-id",
  "variantId": "variant-id",
  "occurredAt": "timestamp"
}
```

Avoid publishing unnecessary private customer information.

---

# 54. Event Architecture

Recommended:

```text
Wishlist Transaction
        ↓
Database Commit
        ↓
Transactional Outbox
        ↓
Event Publisher
        ↓
Message Broker
        ↓
Consumers
```

Potential consumers:

```text
Analytics
Notification
Recommendation
Marketing
Search/Discovery
```

---

# 55. Notification Flow

```text
Product Price Changes
        ↓
Pricing Event
        ↓
Wishlist Consumer
        ↓
Identify affected wishlist items
        ↓
Check notification preference
        ↓
Emit notification request
        ↓
Notification Module
        ↓
Email / Push / In-App
```

Wishlist should not send the actual message.

---

# 56. Availability Flow

```text
Inventory Changes
        ↓
Inventory Event
        ↓
Wishlist
        ↓
Identify saved products
        ↓
Check previous availability
        ↓
Detect restock
        ↓
WISHLIST_ITEM_BACK_IN_STOCK
```

---

# 57. Analytics

Wishlist provides valuable behavioral signals.

Track:

```text
Wishlist Created
Wishlist Item Added
Wishlist Item Removed
Wishlist Item Viewed
Wishlist Item Added to Cart
Wishlist Item Purchased
Wishlist Shared
Wishlist Converted
```

Important:

Analytics should be event-driven rather than storing excessive analytics fields in Wishlist.

---

# 58. Conversion Metrics

Useful future metrics:

```text
Wishlist → Cart Conversion
Wishlist → Order Conversion
Price Drop → Cart Conversion
Restock → Cart Conversion
Wishlist Share → Product View
Wishlist Share → Purchase
```

Example:

```text
100 Wishlist Saves
        ↓
35 Add to Cart
        ↓
20 Orders
```

Conversion:

```text
20%
```

Analytics should be owned by Analytics/Reporting, not Wishlist.

---

# 59. Search Integration

Wishlist can consume product/search identifiers but should not become a search engine.

Example:

```text
Customer searches
      ↓
Search
      ↓
Product
      ↓
Save to Wishlist
```

Wishlist only stores the product reference.

---

# 60. Recommendation Integration

Future recommendation systems can consume:

```text
Wishlist Item Added
Wishlist Item Removed
Wishlist Item Purchased
Wishlist Item Viewed
```

Potential pipeline:

```text
Wishlist
   ↓
Events
   ↓
Recommendation Engine
   ↓
Personalized Product Recommendations
```

This avoids coupling Wishlist directly to AI/recommendation logic.

---

# 61. Caching

Potential cache targets:

```text
Customer wishlist summary
Wishlist item count
Shared wishlist
```

Example:

```text
Redis
  ↓
Wishlist Summary
```

However:

> Cache is never the authoritative source of wishlist ownership or security state.

Database remains authoritative.

---

# 62. Expiration and Cleanup

Guest wishlists may have shorter retention.

Example:

```text
Guest Wishlist
      ↓
Inactive
      ↓
Expiration
      ↓
Cleanup
```

Authenticated wishlists should generally persist longer.

Cleanup should be policy-driven.

Never delete active customer wishlists simply because they have not been used recently unless explicitly required by business policy.

---

# 63. Database Indexes

Recommended:

```text
Wishlist
---------
UNIQUE(public_id)
INDEX(customer_id)
INDEX(guest_token_hash)
INDEX(status)
INDEX(visibility)
```

WishlistItem:

```text
UNIQUE(wishlist_id, product_id, variant_id)
INDEX(wishlist_id)
INDEX(product_id)
INDEX(variant_id)
```

Notification preferences:

```text
INDEX(wishlist_id)
INDEX(item_id)
```

---

# 64. Database Relationships

```text
Customer
   │
   │ 1:N
   ▼
Wishlist
   │
   │ 1:N
   ▼
WishlistItem
   │
   ├──── productId ────> Product
   │
   └──── variantId ────> ProductVariant
```

These external references should not necessarily become JPA relationships across module boundaries.

Prefer IDs and domain services/read models.

---

# 65. Recommended JPA Boundary

Avoid:

```java
@ManyToOne
private Product product;
```

when Product belongs to another bounded module.

Prefer:

```java
private UUID productId;
private UUID variantId;
```

Then resolve through:

```text
ProductService
PricingService
InventoryService
```

or through query/read-model infrastructure.

---

# 66. Module Structure

Recommended module:

```text
wishlist/
├── config/
│   └── WishlistConfig.java
│
├── controller/
│   └── WishlistController.java
│
├── service/
│   ├── WishlistService.java
│   ├── WishlistItemService.java
│   ├── WishlistMergeService.java
│   └── WishlistSharingService.java
│
├── repository/
│   ├── WishlistRepository.java
│   ├── WishlistItemRepository.java
│   └── WishlistNotificationPreferenceRepository.java
│
├── entity/
│   ├── Wishlist.java
│   ├── WishlistItem.java
│   └── WishlistNotificationPreference.java
│
├── dto/
│   ├── request/
│   └── response/
│
├── mapper/
│   └── WishlistMapper.java
│
├── validator/
│   ├── WishlistValidator.java
│   └── WishlistItemValidator.java
│
├── exception/
│   ├── WishlistNotFoundException.java
│   ├── WishlistAccessDeniedException.java
│   ├── WishlistItemAlreadyExistsException.java
│   └── WishlistLimitExceededException.java
│
├── policy/
│   └── WishlistPolicy.java
│
├── event/
│   ├── WishlistCreatedEvent.java
│   ├── WishlistItemAddedEvent.java
│   ├── WishlistMergedEvent.java
│   └── ...
│
├── query/
│   ├── projection/
│   └── view/
│
├── graphql/
│   ├── resolver/
│   │   ├── WishlistQueryResolver.java
│   │   └── WishlistMutationResolver.java
│   │
│   ├── input/
│   │   ├── CreateWishlistInput.java
│   │   ├── UpdateWishlistInput.java
│   │   ├── AddWishlistItemInput.java
│   │   └── WishlistNotificationPreferenceInput.java
│   │
│   └── payload/
│       ├── WishlistPayload.java
│       ├── WishlistItemPayload.java
│       ├── AddToCartPayload.java
│       └── ShareWishlistPayload.java
│
└── WishlistModule.java
```

---

# 67. Module Class

The module boundary should be explicit:

```java
@Configuration
public class WishlistModule {
}
```

The module should expose controlled interfaces to other modules.

Avoid allowing arbitrary modules to directly access Wishlist repositories.

---

# 68. Inter-Module Dependencies

Wishlist depends on:

```text
Authentication
User
Customer
Authorization
Product
Pricing
Inventory
Cart
Common
```

Potential future dependencies:

```text
Notification
Analytics
Subscription
Recommendation
```

Dependency direction should remain controlled.

---

# 69. Recommended Service Boundaries

```text
WishlistService
```

Owns:

- Wishlist lifecycle
- Create/update/delete
- Ownership

```text
WishlistItemService
```

Owns:

- Add
- Remove
- Item operations

```text
WishlistMergeService
```

Owns:

- Guest/customer merging

```text
WishlistSharingService
```

Owns:

- Share
- Unshare
- Token rotation

```text
WishlistValidationService
```

Owns:

- Product validation
- Variant validation
- Wishlist state validation

---

# 70. Error Model

Use stable application error codes.

Examples:

```text
WISHLIST_NOT_FOUND
WISHLIST_ACCESS_DENIED
WISHLIST_LIMIT_EXCEEDED
WISHLIST_ITEM_NOT_FOUND
WISHLIST_ITEM_ALREADY_EXISTS

PRODUCT_NOT_FOUND
PRODUCT_NOT_PURCHASABLE
VARIANT_NOT_FOUND
VARIANT_NOT_AVAILABLE

WISHLIST_SHARE_NOT_ALLOWED
WISHLIST_SHARE_TOKEN_INVALID

CART_ADD_FAILED
```

Do not expose internal database errors to clients.

---

# 71. Business Rules

### BR-01

A customer may have one default wishlist.

### BR-02

A wishlist item cannot be duplicated within the same wishlist.

### BR-03

A wishlist does not reserve inventory.

### BR-04

Wishlist does not own pricing.

### BR-05

Wishlist does not own product data.

### BR-06

Private wishlists are accessible only to their owner.

### BR-07

Shared wishlist access requires a valid share token.

### BR-08

Share tokens can be revoked.

### BR-09

Adding an item to a wishlist does not guarantee future availability.

### BR-10

Adding an item to a wishlist does not lock its price.

### BR-11

Current price must be resolved from Pricing.

### BR-12

Current availability must be resolved from Inventory.

### BR-13

Moving an item to Cart must validate current state.

### BR-14

Guest wishlist data can be merged into an authenticated customer's wishlist.

---

# 72. Non-Functional Requirements

## Performance

Target:

```text
Wishlist read: < 300ms
Wishlist mutation: < 500ms
```

under normal operating conditions.

## Availability

Wishlist functionality should remain highly available.

## Scalability

Support:

```text
Millions of customers
Millions of wishlist items
```

without requiring structural redesign.

## Security

- Strong ownership checks
- Secure guest tokens
- Secure share tokens
- Rate limiting
- Auditability

## Observability

Provide:

- structured logs
- metrics
- tracing
- event monitoring

---

# 73. Observability Metrics

Track:

```text
wishlist.created
wishlist.item.added
wishlist.item.removed
wishlist.item.add_to_cart
wishlist.item.move_to_cart
wishlist.merged
wishlist.shared
wishlist.unshared

wishlist.price_drop.detected
wishlist.restock.detected
```

Operational metrics:

```text
wishlist.api.latency
wishlist.api.error_rate
wishlist.merge.failure_rate
wishlist.share.access_rate
```

---

# 74. Audit Requirements

Audit sensitive operations:

```text
Wishlist Created
Wishlist Deleted
Wishlist Shared
Wishlist Unshared
Wishlist Visibility Changed
Wishlist Merged
Administrative Access
```

Audit record:

```text
actorId
action
resourceType
resourceId
timestamp
metadata
```

---

# 75. Testing Strategy

Tests should exist at multiple levels.

```text
Unit
Integration
Repository
Security
Concurrency
Controller
GraphQL
E2E
```

---

# 76. Unit Tests

Test:

- Wishlist creation
- Default wishlist logic
- Rename
- Archive
- Delete
- Add item
- Remove item
- Duplicate detection
- Ownership rules
- Merge logic
- Share token generation
- Notification preferences

---

# 77. Repository Tests

Test:

- Customer lookup
- Wishlist lookup
- Item lookup
- Unique constraints
- Default wishlist constraint
- Guest token lookup
- Share token lookup
- Pagination

Use Testcontainers for realistic database testing.

---

# 78. Integration Tests

Test:

```text
Wishlist
   ↓
Product
   ↓
Pricing
   ↓
Inventory
   ↓
Cart
```

Scenarios:

1. Save product.
2. Product becomes unavailable.
3. Price changes.
4. Product returns to stock.
5. Add to cart.
6. Move to cart.
7. Guest merge.

---

# 79. Security Tests

Test:

```text
Customer A → Customer B Wishlist
```

Expected:

```text
403 Forbidden
```

Also test:

- invalid guest token
- invalid share token
- revoked share token
- unauthorized mutation
- ID enumeration
- privilege escalation

---

# 80. Concurrency Tests

Test:

```text
Request A → Add Product
Request B → Add Product
```

Expected:

```text
One WishlistItem
```

Also test:

```text
Guest Merge A
Guest Merge B
```

without producing duplicate items.

---

# 81. GraphQL Tests

Test:

- Query wishlist
- Query items
- Create wishlist
- Update wishlist
- Delete wishlist
- Add item
- Remove item
- Share
- Unshare
- Add to cart
- Move to cart
- Authorization

---

# 82. End-to-End Tests

### E2E-01 Customer Wishlist

```text
Login
 ↓
Browse Product
 ↓
Add Wishlist
 ↓
View Wishlist
 ↓
Add to Cart
 ↓
Checkout
```

### E2E-02 Guest Wishlist

```text
Browse
 ↓
Save Products
 ↓
Login
 ↓
Merge
 ↓
View Wishlist
```

### E2E-03 Price Drop

```text
Save Product
 ↓
Price Changes
 ↓
Detect Change
 ↓
Notification Event
```

### E2E-04 Restock

```text
Save Product
 ↓
Product Out of Stock
 ↓
Inventory Restocked
 ↓
Restock Event
```

---

# 83. API Pagination

For large wishlists:

```http
GET /api/v1/wishlists/{id}/items?page=0&size=20
```

Prefer cursor pagination for very large datasets.

GraphQL should support:

```graphql
items(
    first: 20,
    after: "cursor"
)
```

---

# 84. Sorting

Potential sorting:

```text
Recently Added
Oldest Added
Price Low → High
Price High → Low
Availability
Product Name
```

Price-based sorting should use the current Pricing read model rather than stale stored prices.

---

# 85. Filtering

Possible filters:

```text
Available
Out of Stock
Price Dropped
Price Range
Store
Category
Variant
```

Filtering across Product/Pricing/Inventory is another strong use case for a read model.

---

# 86. Wishlist Search

Within a large wishlist:

```text
Search wishlist items
```

Search may operate against the read model.

Do not create a second product-search engine inside Wishlist.

---

# 87. Subscription Integration

Wishlist limits can be entitlement-driven.

Example:

```text
Subscription
     ↓
Entitlements
     ↓
WishlistPolicy
```

Example:

```text
wishlist.max_lists
wishlist.max_items
wishlist.sharing
wishlist.price_alerts
wishlist.restock_alerts
```

This keeps plan rules out of domain logic.

---

# 88. Future Gift Registry

The wishlist architecture can later support:

```text
Gift Registry
```

without redesigning the core model.

Potential:

```text
Wishlist
   ↓
Gift Registry
   ├── Event
   ├── Event Date
   ├── Gift Preferences
   └── Contribution/Reservation
```

This should be a future module/extension, not part of the initial implementation.

---

# 89. Future Collaborative Wishlists

Future functionality could support:

```text
Wishlist
├── Owner
├── Collaborators
└── Permissions
```

Permissions:

```text
VIEW
ADD
REMOVE
EDIT
ADMIN
```

This should use Authorization/Policy infrastructure rather than hardcoded user checks.

---

# 90. Future Wishlist Recommendations

Potential:

```text
Customer Wishlist
        ↓
AI / Recommendation Engine
        ↓
Recommended Products
```

Example:

```text
You saved:
Laptop
Mouse
Keyboard

You may also like:
Laptop Stand
USB Hub
Monitor
```

This belongs to Recommendation/AI rather than Wishlist.

---

# 91. Future Price History

A future feature could show:

```text
$1,299
 ↓
$1,199
 ↓
$999
```

However, historical price tracking should eventually belong to Pricing/Analytics.

Wishlist should only consume relevant price-change information.

---

# 92. State Transition

Wishlist:

```text
          ┌─────────────┐
          │             │
          ▼             │
       ACTIVE ──────────┘
          │
          ▼
      ARCHIVED
          │
          ▼
       DELETED
```

Wishlist item:

```text
ADDED
  ↓
ACTIVE
  ├── REMOVED
  ├── MOVED_TO_CART
  └── PRODUCT_UNAVAILABLE
```

Unavailable does not necessarily mean deleted.

---

# 93. Sequence: Add to Wishlist

```text
Customer
   ↓
Next.js
   ↓
Wishlist API
   ↓
Authorization
   ↓
Wishlist Service
   ↓
Product Validation
   ↓
Create WishlistItem
   ↓
Commit Transaction
   ↓
Outbox
   ↓
WISHLIST_ITEM_ADDED
```

---

# 94. Sequence: Add Wishlist Item to Cart

```text
Wishlist
   ↓
Validate Ownership
   ↓
Validate Product
   ↓
Validate Variant
   ↓
Resolve Price
   ↓
Check Inventory
   ↓
Cart.addItem()
   ↓
Cart Updated
```

Important:

Wishlist does not directly modify Cart database tables.

---

# 95. Sequence: Guest Merge

```text
Guest
  ↓
Login
  ↓
Authentication
  ↓
Customer identified
  ↓
WishlistMergeService
  ↓
Load Guest Wishlist
  ↓
Load Customer Wishlist
  ↓
Deduplicate
  ↓
Apply Limits
  ↓
Merge
  ↓
Delete/Archive Guest Wishlist
  ↓
WISHLIST_MERGED
```

---

# 96. Milestones

## M1 — Wishlist Foundation

Deliver:

- Wishlist entity
- Repository
- Service
- Customer ownership
- Default wishlist
- CRUD
- Validation

---

## M2 — Wishlist Items

Deliver:

- Add item
- Remove item
- Duplicate prevention
- Product references
- Variant references
- Item pagination

---

## M3 — Guest Wishlist

Deliver:

- Guest token
- Guest persistence
- Guest operations
- Authentication merge
- Deduplication
- Cleanup

---

## M4 — Cart Integration

Deliver:

- Add to cart
- Move to cart
- Product validation
- Pricing integration
- Inventory availability integration

---

## M5 — Sharing

Deliver:

- Private/shared/public
- Share token
- Share link
- Token regeneration
- Revocation
- Public read model

---

## M6 — Notifications

Deliver:

- Price-drop preference
- Restock preference
- Pricing events
- Inventory events
- Notification integration

---

## M7 — Analytics & Optimization

Deliver:

- Wishlist events
- Conversion analytics
- Read-model optimization
- Redis caching
- Performance optimization
- Advanced reporting

---

# 97. User Stories

## US-01

**As a customer, I want to save a product to my wishlist so that I can find it later.**

Acceptance:

- Product is saved.
- Duplicate is prevented.
- Item appears in wishlist.

---

## US-02

**As a customer, I want multiple wishlists so that I can organize products.**

Acceptance:

- Customer can create lists.
- Lists have names.
- Items can belong to different lists.

---

## US-03

**As a guest, I want to save products so that I do not lose them before signing in.**

Acceptance:

- Guest receives secure wishlist.
- Products persist.
- Wishlist survives normal browser sessions according to retention policy.

---

## US-04

**As a customer, I want my guest wishlist merged after login.**

Acceptance:

- Guest items are merged.
- Duplicates are removed.
- Customer items are preserved.

---

## US-05

**As a customer, I want to add wishlist products to my cart.**

Acceptance:

- Product is validated.
- Current price is resolved.
- Inventory is checked.
- Cart is updated.

---

## US-06

**As a customer, I want to know when a saved product becomes available.**

Acceptance:

- Restock preference can be enabled.
- Inventory event can trigger notification workflow.

---

## US-07

**As a customer, I want to know when a saved product becomes cheaper.**

Acceptance:

- Price-drop preference can be enabled.
- Pricing changes can trigger notification workflow.

---

## US-08

**As a customer, I want to share my wishlist.**

Acceptance:

- Share token is generated.
- Private data is protected.
- Sharing can be revoked.

---

## US-09

**As a customer, I want to make my wishlist private.**

Acceptance:

- Public/shared access stops.
- Owner retains access.

---

## US-10

**As an administrator, I want controlled access to wishlist data for support purposes.**

Acceptance:

- Permission is required.
- Access is audited.
- Private customer information is protected.

---

# 98. Definition of Done

Wishlist Management is complete when:

### Core

- [ ] Wishlist CRUD implemented
- [ ] Default wishlist implemented
- [ ] Multiple wishlists supported
- [ ] Wishlist items implemented
- [ ] Duplicate protection implemented

### Guest

- [ ] Guest wishlist implemented
- [ ] Secure token implemented
- [ ] Guest/customer merge implemented

### Commerce

- [ ] Product integration implemented
- [ ] Variant integration implemented
- [ ] Pricing integration implemented
- [ ] Inventory integration implemented
- [ ] Cart integration implemented

### Sharing

- [ ] Private visibility implemented
- [ ] Shared visibility implemented
- [ ] Secure share tokens implemented
- [ ] Token revocation implemented

### Notifications

- [ ] Price-drop preference implemented
- [ ] Restock preference implemented
- [ ] Event integration implemented

### APIs

- [ ] REST implemented
- [ ] GraphQL implemented
- [ ] DTOs implemented
- [ ] Error model implemented

### Security

- [ ] Ownership policies implemented
- [ ] Guest security implemented
- [ ] Share-token security implemented
- [ ] Rate limiting implemented
- [ ] Audit logging implemented

### Testing

- [ ] Unit tests
- [ ] Repository tests
- [ ] Integration tests
- [ ] Security tests
- [ ] Concurrency tests
- [ ] GraphQL tests
- [ ] E2E tests

### Operations

- [ ] Metrics
- [ ] Logging
- [ ] Tracing
- [ ] Events
- [ ] Transactional outbox
- [ ] Documentation

---

# 99. Recommended Final Architecture

```text
                     ┌───────────────┐
                     │   Customer    │
                     └───────┬───────┘
                             │
                             ▼
                     ┌───────────────┐
                     │   Wishlist    │
                     └───────┬───────┘
                             │
                    ┌────────┼─────────┐
                    │        │         │
                    ▼        ▼         ▼
                 Product   Pricing   Inventory
                    │        │         │
                    └────────┼─────────┘
                             ▼
                           Cart
                             │
                             ▼
                         Checkout
                             │
                             ▼
                           Order
```

Supporting systems:

```text
Wishlist
   │
   ├── Authorization
   ├── Notification
   ├── Analytics
   ├── Subscription/Entitlement
   └── Common Infrastructure
```

---

# 100. Final Architectural Rules

The following rules should be treated as mandatory:

1. **Wishlist owns save-for-later intent.**
2. **Product owns product information.**
3. **Pricing owns prices.**
4. **Inventory owns availability.**
5. **Cart owns active purchase intent.**
6. **Order owns completed purchase transactions.**
7. **Notification owns notification delivery.**
8. **Analytics owns behavioral analytics.**
9. **Authorization owns permissions.**
10. **Wishlist must not become a product catalog.**
11. **Wishlist must not reserve inventory.**
12. **Wishlist must not trust client-provided prices.**
13. **Wishlist must not directly modify Cart tables.**
14. **Private wishlist access requires ownership authorization.**
15. **Guest and sharing tokens must be opaque and secure.**
16. **Database views are read models, not domain entities.**
17. **Cross-module references should normally use IDs/contracts rather than JPA entity relationships.**
18. **Events should use transactional outbox patterns.**
19. **Customer intent should not be destroyed merely because a product becomes unavailable.**
20. **Wishlist should remain independently scalable and replaceable.**

---

# 101. Commerce Domain Position

After implementing Wishlist, the commerce flow becomes:

```text
                 Category
                    │
                    ▼
                 Product
                /       \
               ▼         ▼
           Pricing    Inventory
               \         /
                \       /
                 ▼     ▼
                  Cart
                    │
                    ▼
               Checkout
                    │
          ┌─────────┴─────────┐
          │                   │
          ▼                   ▼
      Reservation          Pricing
          │                   │
          └─────────┬─────────┘
                    ▼
                  Order
                    │
                    ▼
                 Payment
```

Wishlist operates alongside the purchase flow:

```text
                 Product
                /      \
               ▼        ▼
         Wishlist       Cart
             │           │
             │           ▼
             │        Checkout
             │           │
             └──→ Cart ──┘
```

Therefore:

> **Wishlist is the customer's persistent “maybe later” layer, while Cart is the customer's “I intend to buy this now” layer.**

This separation keeps the Fynza commerce architecture clean, scalable, and suitable for later expansion into recommendations, price alerts, gift registries, and advanced customer engagement.