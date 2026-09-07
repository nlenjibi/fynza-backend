# 07 — Product Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Product Management  
**Module ID:** `PRODUCT-07`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Authentication, User Management, Authorization, Seller Management, Store Management

---

# 1. Overview

The Product Management module manages the products listed on Fynza.

A product represents something a seller offers for sale through a store.

The module manages:

- Product creation.
- Product information.
- Product descriptions.
- Product media references.
- Product status.
- Product visibility.
- Product identifiers.
- Product attributes.
- Product variants.
- Product categories.
- Product publishing.
- Product lifecycle.
- Product metadata.
- Product catalog queries.

The module does **not** own:

- Inventory quantities.
- Payments.
- Orders.
- Customer accounts.
- Seller verification.
- Shipping execution.

---

# 2. Product Domain Boundary

The product domain should be separated from related domains.

```text id="9w4tqz"
Seller
   │
   ▼
Store
   │
   ▼
Product
   │
   ├── Product Information
   ├── Product Media
   ├── Product Attributes
   ├── Product Variants
   └── Product Category References
```

Other domains:

```text id="j7m1rd"
Product
 │
 ├── Price       → Pricing
 ├── Stock       → Inventory
 ├── Order       → Orders
 ├── Payment     → Payments
 ├── Shipping    → Shipping
 └── Review      → Reviews
```

---

# 3. Goals

The Product module should:

1. Allow sellers to create products.
2. Allow sellers to update products.
3. Support product publishing.
4. Support product drafts.
5. Support product categories.
6. Support product attributes.
7. Support product variants.
8. Support product media.
9. Support product visibility.
10. Support product lifecycle.
11. Support product search/indexing.
12. Provide product information to storefronts.
13. Support marketplace-scale catalog querying.

---

# 4. Non-Goals

Product Management does not own:

- Stock quantity.
- Warehouse operations.
- Customer carts.
- Orders.
- Payments.
- Seller payouts.
- Shipping execution.
- Customer reviews.
- Product recommendations.

Those should belong to separate modules.

---

# 5. Product Lifecycle

Recommended statuses:

```text id="j0p2ne"
DRAFT
PENDING_REVIEW
ACTIVE
INACTIVE
OUT_OF_STOCK
SUSPENDED
ARCHIVED
DELETED
```

Typical lifecycle:

```text id="6z1wse"
DRAFT
  │
  ▼
PENDING_REVIEW
  │
  ├────► SUSPENDED
  │
  ▼
ACTIVE
  │
  ├────► INACTIVE
  │
  ├────► OUT_OF_STOCK
  │
  └────► ARCHIVED
```

Important:

> `OUT_OF_STOCK` is often better treated as an availability condition rather than a permanent product lifecycle state.

Therefore, the implementation should avoid tightly coupling stock state to the Product entity.

---

# 6. Product Visibility

Product status and visibility should be separate.

Possible visibility:

```text id="a8h7pp"
PUBLIC
PRIVATE
UNLISTED
```

Example:

```text id="fj4d5v"
status     = ACTIVE
visibility = PUBLIC
```

---

# 7. PRODUCT-FR-001 — Create Product

Authorized seller users can create products.

```text id="4w3a1v"
Seller
 │
 ▼
Store
 │
 ▼
Create Product
 │
 ▼
Validate
 │
 ▼
DRAFT
```

Required information may include:

- Product name.
- Store.
- Description.
- Product type.
- Category.
- Base product information.

---

# 8. PRODUCT-FR-002 — Product Name

Every product must have a customer-facing name.

Example:

```text id="q9i5ks"
Wireless Bluetooth Headphones
Men's Cotton Shirt
Laptop Stand
Office Desk
```

Requirements:

- Required.
- Length validated.
- Unsafe/prohibited content filtered.
- Search-indexable.
- Uniqueness should not generally be required globally.

Two stores may sell similarly named products.

---

# 9. PRODUCT-FR-003 — Product Description

Products should support detailed descriptions.

The description may contain:

```text id="r7p5y9"
Features
Specifications
Usage Information
Materials
Dimensions
Care Instructions
Warranty Information
```

Rich text should be sanitized before storage/rendering.

---

# 10. PRODUCT-FR-004 — Product SKU

Products and variants may have SKUs.

Example:

```text id="7q0n9b"
SKU-HEADPHONE-001
SKU-SHIRT-BLK-M
SKU-LAPTOP-001
```

SKU ownership should normally be seller/store scoped rather than globally required.

Recommended constraint:

```text id="5x0vhs"
(storeId, sku) → UNIQUE
```

---

# 11. Product Identifiers

The system should distinguish:

```text id="l4skv2"
id
publicId
productNumber
sku
barcode
```

Example:

```text id="u2g1am"
Database ID  → internal
Public ID    → API-facing
Product No   → Fynza business identifier
SKU          → seller inventory identifier
Barcode      → external/product identifier
```

---

# 12. PRODUCT-FR-005 — Product Media

Products may have:

- Main image.
- Additional images.
- Videos.
- Documents.

The Product module should store media references.

```text id="i3c9jv"
Product
 │
 ├── Media 1
 ├── Media 2
 ├── Media 3
 └── Video
```

Actual files belong to the Media/Storage module.

---

# 13. Product Media Model

```text id="b6bq4h"
ProductMedia
-------------------------
id
productId
mediaId
type
sortOrder
isPrimary
createdAt
```

Types:

```text id="c2pn2g"
IMAGE
VIDEO
DOCUMENT
```

---

# 14. PRODUCT-FR-006 — Product Categories

Products should belong to categories.

```text id="3o0a0z"
Product
 │
 ▼
Category
```

A product may support:

- Primary category.
- Secondary categories.
- Category-specific attributes.

Category ownership should eventually belong to a dedicated Category/Taxonomy module.

---

# 15. Category Architecture

Avoid embedding category definitions inside Product.

```text id="2m36y9"
Category Module
      │
      ▼
Product
      │
      ▼
categoryId
```

This allows categories to evolve independently.

---

# 16. PRODUCT-FR-007 — Product Attributes

Products may have structured attributes.

Examples:

```text id="flp2at"
Brand: Sony
Color: Black
Material: Cotton
Weight: 2kg
Screen Size: 15.6"
```

Generic model:

```text id="2x6q4m"
Product
 │
 └── Attributes
      ├── Brand
      ├── Color
      ├── Material
      └── Weight
```

---

# 17. Attribute Architecture

Attributes should support category-specific definitions.

```text id="h5m7tq"
Category
 │
 ▼
Attribute Definition
 │
 ▼
Product Attribute Value
```

Example:

```text id="4f0w4a"
Laptop
 │
 ├── RAM
 ├── Storage
 ├── Processor
 └── Screen Size
```

This avoids putting hundreds of nullable columns into Product.

---

# 18. PRODUCT-FR-008 — Product Variants

Products may have variants.

Example:

```text id="0f9l8e"
T-Shirt
 │
 ├── Black / Small
 ├── Black / Medium
 ├── Black / Large
 ├── White / Small
 └── White / Medium
```

Each variant may have its own:

- SKU.
- Barcode.
- Price reference.
- Inventory reference.
- Attributes.
- Media.

---

# 19. Variant Architecture

```text id="r5h4tv"
Product
 │
 ├── Variant A
 │    ├── Color: Black
 │    └── Size: M
 │
 ├── Variant B
 │    ├── Color: Black
 │    └── Size: L
 │
 └── Variant C
      ├── Color: White
      └── Size: M
```

---

# 20. Product vs Variant

The parent Product represents the commercial item.

The Variant represents a purchasable configuration.

Example:

```text id="y0c1tt"
Product:
  Nike Air Max

Variants:
  Black / 42
  Black / 43
  White / 42
  White / 43
```

Inventory should normally be tracked at variant level when variants exist.

---

# 21. PRODUCT-FR-009 — Product Publishing

A seller can publish a product after required information is complete.

```text id="8p3xqk"
DRAFT
 │
 ▼
Validation
 │
 ▼
Required Information Complete
 │
 ▼
PENDING_REVIEW / ACTIVE
```

Whether products require marketplace review should be configurable.

---

# 22. Product Publication Requirements

Before publication:

- Name exists.
- Description meets requirements.
- Category exists where required.
- Media requirements satisfied.
- Seller is active.
- Store is active.
- Required attributes exist.
- Pricing is available.
- Inventory rules are satisfied.

Pricing and inventory should be validated through their respective modules.

---

# 23. PRODUCT-FR-010 — Update Product

Authorized sellers can update their products.

Updates should be validated according to product status.

Some changes may require re-review.

For example:

```text id="n6h2xz"
Product Active
      │
      ▼
Change Restricted Attribute
      │
      ▼
PENDING_REVIEW
```

---

# 24. PRODUCT-FR-011 — Archive Product

Sellers can archive products.

```text id="1n6m6e"
ACTIVE
  │
  ▼
ARCHIVED
```

Archiving should preserve historical references.

Existing orders should continue referencing the original product snapshot.

---

# 25. Product Deletion

Hard deletion should be avoided for products that have participated in transactions.

Instead:

```text id="q1xq5m"
Product
 │
 ▼
ARCHIVED
```

or:

```text id="6i0g0w"
DELETED
```

with historical records preserved.

---

# 26. Product Ownership

Every product must belong to an authorized seller/store.

```text id="i9a1z3"
User
 │
 ▼
Seller Membership
 │
 ▼
Store
 │
 ▼
Product
```

The system must verify ownership at every mutation.

---

# 27. Authorization

Seller permissions:

```text id="v5k4yq"
product.read.own
product.create.own
product.update.own
product.delete.own
product.publish.own
product.archive.own
```

Administrative:

```text id="h2p8w4"
product.read
product.manage
product.review
product.suspend
product.restore
```

---

# 28. Customer Product Access

Customers generally access public products without seller authorization.

```text id="p8k9q2"
Customer
 │
 ▼
Product Query
 │
 ▼
Visibility Check
 │
 ▼
PUBLIC + ACTIVE
 │
 ▼
Return Product
```

---

# 29. Product Pricing Boundary

Do not make Product responsible for all pricing logic.

Prefer:

```text id="9x3d7s"
Product
 │
 └── Product/Variant Identity
             │
             ▼
          Pricing
             │
             ├── Base Price
             ├── Sale Price
             ├── Promotions
             └── Price History
```

The Product module may reference a current price but Pricing should own pricing rules.

---

# 30. Product Inventory Boundary

Inventory should be independent.

```text id="0z5z1d"
Product
 │
 ▼
Variant
 │
 ▼
Inventory
 │
 ├── Quantity
 ├── Reserved
 ├── Available
 └── Warehouse
```

Do not put:

```text id="d4s7cm"
quantity
availableQuantity
reservedQuantity
```

directly inside Product unless there is a deliberate read-model/cache strategy.

---

# 31. Product Order Boundary

Orders should reference products/variants.

```text id="b2g7cs"
Order
 │
 └── OrderItem
       │
       ├── productId
       ├── variantId
       ├── quantity
       └── priceSnapshot
```

Historical order data should not depend on the current Product record.

---

# 32. Product Snapshot

When an order is created, store a snapshot of relevant product information.

```text id="n7g5fk"
OrderItem
 ├── productId
 ├── variantId
 ├── productNameSnapshot
 ├── skuSnapshot
 ├── priceSnapshot
 └── selectedAttributesSnapshot
```

This protects historical orders from future product changes.

---

# 33. Product Review Boundary

Reviews belong to Review Management.

```text id="w8y4o3"
Customer
 │
 ▼
Review
 │
 ▼
Product
```

Product may expose:

```text id="k6p1tc"
averageRating
reviewCount
```

but these should be derived values.

---

# 34. Product Search

Product search should eventually be handled by a dedicated Search/Discovery module.

Product Management should publish/index:

```text id="3p4z4a"
productId
storeId
sellerId
name
description
categoryIds
attributes
status
visibility
```

Possible search technologies can be introduced later without changing the Product domain model.

---

# 35. Product Data Model

## Product

```text id="1m6f8r"
Product
-------------------------
id
publicId
productNumber
storeId
sellerId
name
slug
description
productType
status
visibility
createdAt
updatedAt
```

Whether both `storeId` and `sellerId` are persisted should depend on the final domain consistency strategy.

If Store uniquely determines Seller, `sellerId` may be derived.

---

# 36. Product Variant

```text id="9h4s7p"
ProductVariant
-------------------------
id
productId
publicId
sku
barcode
name
status
createdAt
updatedAt
```

---

# 37. Product Attribute

```text id="6c8v2s"
ProductAttribute
-------------------------
id
productId
attributeDefinitionId
value
createdAt
updatedAt
```

For variants:

```text id="r0m3ye"
ProductVariantAttribute
-------------------------
id
variantId
attributeDefinitionId
value
```

---

# 38. Product Category Reference

```text id="3j8k6q"
ProductCategory
-------------------------
id
productId
categoryId
isPrimary
createdAt
```

This allows many-to-many category relationships where required.

---

# 39. Product Status History

```text id="5x2c8a"
ProductStatusHistory
-------------------------
id
productId
previousStatus
newStatus
reason
changedBy
createdAt
```

---

# 40. Product Slug

Products should have SEO/customer-friendly slugs.

Example:

```text id="w5h9vp"
/products/wireless-bluetooth-headphones
```

The slug should:

- Be normalized.
- Be unique within the appropriate namespace.
- Handle changes safely.
- Support redirects if required.

---

# 41. Product API

## REST

```text id="b0y8a5"
GET    /products
GET    /products/{id}
GET    /products/slug/{slug}

POST   /sellers/me/stores/{storeId}/products
GET    /sellers/me/stores/{storeId}/products

PATCH  /sellers/me/stores/{storeId}/products/{id}
DELETE /sellers/me/stores/{storeId}/products/{id}

POST   /products/{id}/publish
POST   /products/{id}/archive

GET    /products/{id}/variants
POST   /products/{id}/variants
PATCH  /products/{id}/variants/{variantId}
DELETE /products/{id}/variants/{variantId}
```

---

# 42. GraphQL

## Queries

```graphql id="x3q5t7"
product(id: ID!)
productBySlug(slug: String!)
products(filter: ProductFilterInput)
myProducts(filter: ProductFilterInput)
productVariants(productId: ID!)
```

## Mutations

```graphql id="m2j6f9"
createProduct
updateProduct
publishProduct
archiveProduct

createProductVariant
updateProductVariant
deleteProductVariant

addProductMedia
removeProductMedia

addProductCategory
removeProductCategory

updateProductAttributes
```

---

# 43. GraphQL Structure

```text id="w7k1m3"
product/
└── graphql/
    ├── resolver/
    │   ├── ProductResolver
    │   ├── ProductVariantResolver
    │   └── ProductAttributeResolver
    │
    ├── input/
    │   ├── CreateProductInput
    │   ├── UpdateProductInput
    │   ├── ProductFilterInput
    │   ├── CreateVariantInput
    │   └── ProductAttributeInput
    │
    └── payload/
        ├── ProductPayload
        ├── ProductListPayload
        ├── ProductVariantPayload
        └── ProductStatisticsPayload
```

---

# 44. Product Query Architecture

For complex product listing pages:

```text id="q7n8p1"
Product
 │
 ├── Category
 ├── Store
 ├── Seller
 ├── Pricing
 ├── Inventory
 └── Review
       │
       ▼
ProductSummaryView
```

Use database views/projections where appropriate.

Example:

```text id="z2y7x8"
product/
├── entity/
├── repository/
├── query/
│   ├── projection/
│   └── view/
└── ...
```

Views should remain read-only.

---

# 45. Product Listing Read Model

A product listing may require:

```text id="7s3q0k"
productName
productSlug
primaryImage
storeName
storeSlug
category
currentPrice
discount
available
averageRating
reviewCount
```

These values originate from multiple domains.

A read model prevents the Product entity from becoming:

```text id="p0z9y7"
Product
 ├── Price
 ├── Inventory
 ├── Review
 ├── Store
 ├── Seller
 └── ...
```

---

# 46. Product Events

Recommended events:

```text id="4c8x9n"
PRODUCT_CREATED
PRODUCT_UPDATED
PRODUCT_PUBLISHED
PRODUCT_ARCHIVED
PRODUCT_SUSPENDED
PRODUCT_RESTORED

PRODUCT_VARIANT_CREATED
PRODUCT_VARIANT_UPDATED
PRODUCT_VARIANT_DELETED

PRODUCT_MEDIA_ADDED
PRODUCT_MEDIA_REMOVED

PRODUCT_CATEGORY_CHANGED
PRODUCT_ATTRIBUTES_UPDATED
```

---

# 47. Product Event Consumers

Events may be consumed by:

```text id="2r5m7c"
Product
 │
 ├──► Search
 ├──► Analytics
 ├──► Recommendation
 ├──► Notification
 └──► Audit
```

This keeps the Product module independent.

---

# 48. Security Requirements

## PRODUCT-NFR-001

A seller may only modify products belonging to their authorized store.

## PRODUCT-NFR-002

Seller staff permissions must be enforced.

## PRODUCT-NFR-003

Public APIs must expose only publicly visible product information.

## PRODUCT-NFR-004

Administrative actions must be audited.

## PRODUCT-NFR-005

Product descriptions and media metadata must be sanitized.

## PRODUCT-NFR-006

Product ownership checks must be server-side.

---

# 49. Performance Requirements

Recommended indexes:

```text id="1f8m0d"
Product
├── storeId
├── sellerId
├── slug
├── status
├── visibility
├── createdAt
└── productNumber

ProductVariant
├── productId
├── sku
└── barcode

ProductCategory
├── productId
└── categoryId
```

For SKU:

```text id="4n6v2w"
(storeId, sku) → UNIQUE
```

---

# 50. Caching

Public product data can be cached.

Example:

```text id="0a7y3e"
product:{productId}
product:slug:{slug}
```

Invalidate when:

```text id="2v9m8r"
PRODUCT_UPDATED
PRODUCT_PUBLISHED
PRODUCT_ARCHIVED
PRODUCT_SUSPENDED
PRODUCT_MEDIA_ADDED
PRODUCT_ATTRIBUTES_UPDATED
```

Price and inventory caches should generally be owned by their respective domains.

---

# 51. Testing

## 51.1 Unit Tests

Test:

- Product creation.
- Product validation.
- Slug generation.
- SKU validation.
- Variant management.
- Attribute validation.
- Category association.
- Publication rules.
- Status transitions.

---

# 52. Integration Tests

Test:

```text id="p6w2a4"
Seller
 │
 ▼
Store
 │
 ▼
Product
 │
 ├── Variant
 ├── Media
 ├── Category
 └── Attributes
```

Also test:

- Authorization.
- Storage references.
- Category integration.
- Pricing integration.
- Inventory integration.

---

# 53. Security Tests

Test:

- Seller A modifying Seller B's product.
- Seller staff without permission updating products.
- Unauthorized publication.
- Unauthorized deletion.
- Cross-store access.
- IDOR.
- Suspended seller creating products.
- Suspended store publishing products.

---

# 54. E2E Tests

### Product Creation

```text id="7c2x5p"
Seller
 │
 ▼
Select Store
 │
 ▼
Create Product
 │
 ▼
Add Information
 │
 ▼
Add Media
 │
 ▼
Add Category
 │
 ▼
Save Draft
```

### Product Publication

```text id="5q1v8m"
DRAFT
 │
 ▼
Complete Required Fields
 │
 ▼
Validate
 │
 ▼
Publish
 │
 ▼
ACTIVE
```

### Variant Creation

```text id="3p7w2k"
Product
 │
 ▼
Create Variant
 │
 ├── Color
 ├── Size
 └── SKU
      │
      ▼
Variant Created
```

---

# 55. Architecture

Recommended module structure:

```text id="c9m5r2"
product/
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
└── ProductModule.java
```

Tests:

```text id="e2y8q7"
test/
└── product/
    ├── service/
    ├── repository/
    ├── policy/
    ├── controller/
    ├── graphql/
    ├── integration/
    └── security/
```

---

# 56. Dependencies

```text id="b4n7m2"
Product
│
├── Authentication
├── User Management
├── Authorization
├── Seller Management
├── Store Management
├── Category/Taxonomy
├── Storage/Media
└── Event Infrastructure
```

Future integrations:

```text id="s8k2m4"
Product
 │
 ├── Pricing
 ├── Inventory
 ├── Search
 ├── Review
 ├── Recommendation
 ├── Cart
 └── Order
```

---

# 57. User Stories

## US-001 — Create Product

> As a seller, I want to create products so that I can offer items for sale.

## US-002 — Edit Product

> As a seller, I want to update product information so that customers see accurate information.

## US-003 — Product Media

> As a seller, I want to add product images so that customers can understand what I am selling.

## US-004 — Categories

> As a seller, I want to categorize my products so that customers can find them easily.

## US-005 — Variants

> As a seller, I want to create product variants so that customers can choose options such as size and color.

## US-006 — Product Publishing

> As a seller, I want to publish my products so that customers can purchase them.

## US-007 — Product Search

> As a customer, I want to search and filter products so that I can quickly find what I need.

## US-008 — Product Archive

> As a seller, I want to archive products that I no longer sell without losing historical information.

## US-009 — Product Administration

> As an administrator, I want to review and suspend products so that marketplace policies can be enforced.

---

# 58. Milestones

## M1 — Product Foundation

- [ ] Product entity.
- [ ] Product number.
- [ ] Product slug.
- [ ] Store relationship.
- [ ] Product lifecycle.
- [ ] Product CRUD.

## M2 — Product Catalog

- [ ] Product descriptions.
- [ ] Product media.
- [ ] Categories.
- [ ] Attributes.
- [ ] Product metadata.

## M3 — Product Variants

- [ ] Variant entity.
- [ ] Variant attributes.
- [ ] Variant SKU.
- [ ] Variant barcode.
- [ ] Variant lifecycle.

## M4 — Publishing

- [ ] Product validation.
- [ ] Product publishing.
- [ ] Drafts.
- [ ] Review workflow.
- [ ] Suspension.
- [ ] Archiving.

## M5 — Marketplace Integration

- [ ] Pricing integration.
- [ ] Inventory integration.
- [ ] Storefront integration.
- [ ] Review integration.
- [ ] Search indexing.
- [ ] Product events.

## M6 — Optimization

- [ ] Product read models.
- [ ] Database views.
- [ ] Caching.
- [ ] Search optimization.
- [ ] Query optimization.
- [ ] Analytics integration.

---

# 59. Definition of Done

- [ ] Product entity implemented.
- [ ] Product CRUD implemented.
- [ ] Product number implemented.
- [ ] Product slug implemented.
- [ ] Product lifecycle implemented.
- [ ] Product media implemented.
- [ ] Category integration implemented.
- [ ] Product attributes implemented.
- [ ] Product variants implemented.
- [ ] SKU management implemented.
- [ ] Product publishing implemented.
- [ ] Product archiving implemented.
- [ ] Product suspension implemented.
- [ ] Seller ownership authorization implemented.
- [ ] GraphQL API implemented.
- [ ] REST API implemented.
- [ ] Product events implemented.
- [ ] Audit implemented.
- [ ] Read models implemented where required.
- [ ] Database indexes implemented.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] E2E tests pass.
- [ ] Documentation completed.

---

# 60. Architectural Principle

Product Management should remain the **catalog/product identity domain**.

```text id="y5v1c7"
                 SELLER
                    │
                    ▼
                  STORE
                    │
                    ▼
                 PRODUCT
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
       VARIANT   ATTRIBUTE   MEDIA
          │
          ▼
      INVENTORY
          │
          ▼
        ORDER
          │
          ▼
       PAYMENT
```

The important separation is:

```text id="v8x3n1"
Product
  = What is being sold?

Pricing
  = How much does it cost?

Inventory
  = How many are available?

Order
  = What did the customer purchase?

Payment
  = How was it paid for?

Shipping
  = How is it delivered?

Review
  = What did the customer think?
```

### Core Rule

> **Product Management owns the definition and catalog representation of what is being sold. It should not become the owner of pricing, inventory, ordering, payment, or fulfillment.**

This gives Fynza a clean catalog foundation for the next major domain: **`08 — Category & Taxonomy Management PRD`**.