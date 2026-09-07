# 08 — Category & Taxonomy Management PRD

**Product:** Fynza E-Commerce Platform  
**Module:** Category & Taxonomy Management  
**Module ID:** `CATEGORY-08`  
**Priority:** Critical  
**Status:** Planned  
**Dependencies:** Authentication, User Management, Authorization, Product Management

---

# 1. Overview

The Category & Taxonomy Management module manages the classification and organization of products across Fynza.

It provides the structure that allows products to be organized into meaningful categories.

Example:

```text
Electronics
├── Computers
│   ├── Laptops
│   ├── Desktops
│   └── Tablets
│
├── Phones
│   ├── Smartphones
│   └── Accessories
│
└── Audio
    ├── Headphones
    └── Speakers
```

The module manages:

- Categories.
- Category hierarchy.
- Category slugs.
- Category descriptions.
- Category media.
- Category status.
- Category visibility.
- Category attributes.
- Category ordering.
- Category relationships.
- Category metadata.
- Taxonomy administration.
- Product-category relationships.

The module does **not** own product information.

---

# 2. Goals

The module should:

1. Provide a centralized product taxonomy.
2. Support hierarchical categories.
3. Support unlimited category depth where practical.
4. Allow products to reference categories.
5. Support category-specific attributes.
6. Support category visibility.
7. Support category ordering.
8. Support category administration.
9. Support category lifecycle.
10. Provide navigation structures for customers.
11. Support search and filtering.
12. Support future marketplace expansion.

---

# 3. Non-Goals

Category Management does not own:

- Products.
- Product descriptions.
- Product prices.
- Product inventory.
- Product variants.
- Orders.
- Sellers.
- Stores.
- Customers.

Relationship:

```text
Category
    │
    ▼
Product
    │
    ├── Pricing
    ├── Inventory
    └── Orders
```

Category provides classification.

Product owns product information.

---

# 4. Taxonomy Architecture

The recommended structure is:

```text
Taxonomy
   │
   └── Categories
          │
          ├── Parent
          │    └── Child
          │         └── Sub-child
          │
          └── Attributes
```

Example:

```text
Fashion
│
├── Men
│   ├── Clothing
│   │   ├── Shirts
│   │   └── Trousers
│   │
│   └── Shoes
│
└── Women
    ├── Clothing
    └── Shoes
```

---

# 5. Taxonomy vs Category

A taxonomy represents the overall classification system.

A category is an individual node within that taxonomy.

```text
Taxonomy
   │
   ├── Electronics
   ├── Fashion
   ├── Home
   └── Beauty
```

Initially Fynza may use one primary marketplace taxonomy.

The model should nevertheless allow multiple taxonomies later.

---

# 6. Category Lifecycle

Recommended statuses:

```text
DRAFT
ACTIVE
INACTIVE
ARCHIVED
DELETED
```

Typical lifecycle:

```text
DRAFT
  │
  ▼
ACTIVE
  │
  ├──► INACTIVE
  │
  └──► ARCHIVED
```

A category should not normally be hard-deleted if products historically referenced it.

---

# 7. CATEGORY-FR-001 — Create Category

Authorized administrators can create categories.

Example:

```text
Fashion
  │
  └── Men's Clothing
```

Creation requires:

- Name.
- Parent category where applicable.
- Status.
- Slug.
- Ordering information.

---

# 8. CATEGORY-FR-002 — Category Name

Every category must have a customer-facing name.

Examples:

```text
Electronics
Laptops
Smartphones
Men's Shoes
Kitchen Appliances
```

Names should be:

- Required.
- Validated.
- Sanitized.
- Unique within the relevant parent/taxonomy scope.

---

# 9. CATEGORY-FR-003 — Category Slug

Every public category should have a URL-friendly slug.

Examples:

```text
electronics
laptops
smartphones
mens-shoes
```

Example URLs:

```text
/categories/electronics
/categories/electronics/laptops
```

Slug uniqueness should be enforced within the taxonomy namespace.

---

# 10. CATEGORY-FR-004 — Category Hierarchy

Categories should support parent-child relationships.

Example:

```text
Electronics
│
├── Computers
│   ├── Laptops
│   └── Desktops
│
└── Phones
    ├── Smartphones
    └── Feature Phones
```

Data model:

```text
Category
 ├── id
 ├── parentId
 └── ...
```

The root category has:

```text
parentId = null
```

---

# 11. Hierarchy Rules

The system must prevent:

```text
Category A
  ↓
Category B
  ↓
Category A
```

A category cannot become its own descendant.

The system must also prevent circular relationships.

---

# 12. Category Depth

Fynza should support configurable maximum depth.

Example:

```text
Level 0 → Electronics
Level 1 → Computers
Level 2 → Laptops
Level 3 → Gaming Laptops
```

A practical default may be 3–5 levels.

Avoid unnecessarily deep taxonomies because they make navigation difficult.

---

# 13. CATEGORY-FR-005 — Category Description

Categories may contain descriptions.

Example:

```text
Browse laptops from trusted sellers
across different price ranges and brands.
```

Descriptions can be useful for:

- SEO.
- Category landing pages.
- Customer navigation.
- Search engines.

---

# 14. CATEGORY-FR-006 — Category Image

Categories may have:

- Icon.
- Thumbnail.
- Banner.
- Cover image.

Media should be managed by the Media/Storage module.

```text
Category
 │
 └── mediaId
       │
       ▼
 Media Service
```

The Category module should store references rather than files.

---

# 15. CATEGORY-FR-007 — Category Visibility

Possible visibility states:

```text
PUBLIC
PRIVATE
UNLISTED
```

Example:

```text
Category
status     = ACTIVE
visibility = PUBLIC
```

Only public categories should normally appear in customer navigation.

---

# 16. CATEGORY-FR-008 — Category Ordering

Categories should support ordering.

Example:

```text
Electronics    → 1
Fashion        → 2
Home           → 3
Beauty         → 4
```

Within a parent:

```text
Electronics
├── Phones       → 1
├── Computers    → 2
└── Audio        → 3
```

Recommended field:

```text
sortOrder
```

---

# 17. CATEGORY-FR-009 — Move Category

Administrators may move a category to a different parent.

Example:

```text
Old:
Electronics
 └── Accessories

New:
Fashion
 └── Accessories
```

Before moving, validate:

- No circular dependency.
- Maximum depth.
- Existing product relationships.
- URL/slug impact.

---

# 18. Category URL Changes

Changing category hierarchy may change its URL.

Example:

```text
/categories/electronics/laptops
```

becomes:

```text
/categories/computers/laptops
```

Fynza should preserve old URLs through redirects where SEO/history requires it.

---

# 19. CATEGORY-FR-010 — Category Attributes

Categories may define which attributes apply to products.

Example:

```text
Laptops
│
├── Brand
├── Processor
├── RAM
├── Storage
├── Screen Size
└── Operating System
```

This creates a structured product catalog.

---

# 20. Attribute Architecture

```text
Category
    │
    ▼
Attribute Definition
    │
    ▼
Product Attribute
```

Example:

```text
Category: Laptop

Attribute:
  RAM
  Type: NUMBER
  Unit: GB
  Required: true
```

---

# 21. Attribute Definition

Recommended fields:

```text
AttributeDefinition
-------------------------
id
categoryId
name
code
dataType
unit
required
filterable
searchable
variantDefining
sortOrder
createdAt
updatedAt
```

---

# 22. Attribute Data Types

Supported types may include:

```text
TEXT
NUMBER
DECIMAL
BOOLEAN
DATE
ENUM
MULTI_SELECT
```

Examples:

```text
Brand        → ENUM
RAM          → NUMBER
Screen Size  → DECIMAL
Waterproof   → BOOLEAN
Color        → ENUM
```

---

# 23. Attribute Options

For enum attributes:

```text
Color
├── Black
├── White
├── Blue
└── Red
```

Model:

```text
AttributeDefinition
       │
       ▼
AttributeOption
```

---

# 24. CATEGORY-FR-011 — Filterable Attributes

Some attributes should be available as filters.

Example:

```text
Laptops
│
├── Brand
├── RAM
├── Storage
├── Screen Size
└── Processor
```

Customer query:

```text
Brand = HP
RAM >= 16GB
Storage >= 512GB
```

Search infrastructure should consume these definitions.

---

# 25. CATEGORY-FR-012 — Variant-Defining Attributes

Some attributes define product variants.

Example:

```text
T-Shirt
│
├── Color
└── Size
```

The category can indicate:

```text
Color → variantDefining = true
Size  → variantDefining = true
```

Product Management uses these definitions to construct variants.

---

# 26. Product-Category Relationship

Products reference categories.

Recommended:

```text
ProductCategory
-------------------------
productId
categoryId
isPrimary
createdAt
```

A product may belong to multiple categories where business rules permit.

Example:

```text
Product
 │
 ├── Primary Category → Laptops
 └── Secondary        → Computers
```

---

# 27. Category Ownership

Categories should normally be platform-owned.

```text
Platform
   │
   ▼
Taxonomy
   │
   ▼
Categories
```

Sellers should not freely create marketplace categories unless Fynza deliberately supports seller-defined taxonomies.

This prevents:

```text
Seller A → "Phones"
Seller B → "Phone"
Seller C → "Mobile Phones"
Seller D → "Best Phones"
```

from creating a fragmented catalog.

---

# 28. Seller Category Suggestions

Sellers may suggest new categories.

```text
Seller
 │
 ▼
Category Suggestion
 │
 ▼
Admin Review
 │
 ├──► APPROVED
 └──► REJECTED
```

This is preferable to giving sellers direct control over the global taxonomy.

---

# 29. Category Suggestion Model

```text
CategorySuggestion
-------------------------
id
requestedBy
name
description
parentCategoryId
reason
status
reviewedBy
reviewedAt
createdAt
```

Statuses:

```text
PENDING
APPROVED
REJECTED
```

---

# 30. CATEGORY-FR-013 — Category Search

Administrators should be able to search categories.

Search:

```text
name
slug
code
status
parent
```

Customers should receive category discovery through Search/Discovery.

---

# 31. CATEGORY-FR-014 — Category Tree

The API should support retrieving category trees.

Example:

```text
Electronics
├── Computers
│   ├── Laptops
│   └── Desktops
├── Phones
│   ├── Smartphones
│   └── Accessories
└── Audio
```

This is heavily used by:

- Navigation.
- Mega menus.
- Category pages.
- Product filters.

---

# 32. Category Breadcrumbs

The system should support:

```text
Home
  >
Electronics
  >
Computers
  >
Laptops
```

This can be generated from the category hierarchy.

For high-traffic queries, breadcrumbs can be represented in a read model.

---

# 33. CATEGORY-FR-015 — Category Statistics

Useful derived information:

```text
productCount
activeProductCount
childCategoryCount
```

These should not necessarily be stored directly on Category.

Instead:

```text
Category
   │
   ▼
CategorySummaryView
   │
   ├── Product count
   ├── Active products
   └── Child categories
```

---

# 34. Database Views

Category landing pages may require:

```text
Category
+
Product count
+
Active product count
+
Store count
+
Average price
```

Do not make Category contain all these values.

Use:

```text
category/
├── entity/
├── repository/
├── query/
│   ├── projection/
│   └── view/
└── ...
```

Database views remain read-only.

---

# 35. Data Model

## Taxonomy

```text
Taxonomy
-------------------------
id
publicId
name
code
description
status
createdAt
updatedAt
```

---

# 36. Category

```text
Category
-------------------------
id
publicId
taxonomyId
parentId
name
slug
description
status
visibility
sortOrder
mediaId
createdAt
updatedAt
```

Indexes:

```text
taxonomyId
parentId
slug
status
visibility
```

---

# 37. Category Closure / Hierarchy Strategy

For small taxonomies, a simple adjacency list is sufficient:

```text
Category
  │
  └── parentId
```

For large and heavily queried taxonomies, Fynza can consider a closure table:

```text
CategoryClosure
-------------------------
ancestorId
descendantId
depth
```

Example:

```text
Electronics → Electronics       0
Electronics → Computers        1
Electronics → Laptops           2
Computers   → Laptops            1
Laptops     → Laptops             0
```

This makes descendant/ancestor queries efficient.

Do not introduce a closure table unless the taxonomy/query requirements justify it.

---

# 38. Attribute Definition Model

```text
AttributeDefinition
-------------------------
id
categoryId
name
code
dataType
unit
required
filterable
searchable
variantDefining
sortOrder
createdAt
updatedAt
```

---

# 39. Attribute Option

```text
AttributeOption
-------------------------
id
attributeDefinitionId
value
label
sortOrder
active
createdAt
updatedAt
```

---

# 40. Category Status History

```text
CategoryStatusHistory
-------------------------
id
categoryId
previousStatus
newStatus
reason
changedBy
createdAt
```

---

# 41. API Design

## REST

```text
GET    /categories
GET    /categories/tree
GET    /categories/{id}
GET    /categories/slug/{slug}

GET    /categories/{id}/children
GET    /categories/{id}/ancestors
GET    /categories/{id}/attributes

POST   /admin/categories
PATCH  /admin/categories/{id}
DELETE /admin/categories/{id}

POST   /admin/categories/{id}/move
POST   /admin/categories/{id}/activate
POST   /admin/categories/{id}/deactivate
POST   /admin/categories/{id}/archive

POST   /categories/suggestions
GET    /admin/categories/suggestions
POST   /admin/categories/suggestions/{id}/approve
POST   /admin/categories/suggestions/{id}/reject
```

---

# 42. GraphQL

## Queries

```graphql
categories
category(id: ID!)
categoryBySlug(slug: String!)

categoryTree
categoryChildren(categoryId: ID!)
categoryAncestors(categoryId: ID!)

categoryAttributes(categoryId: ID!)
categoryStatistics(categoryId: ID!)
```

## Mutations

```graphql
createCategory
updateCategory
moveCategory
activateCategory
deactivateCategory
archiveCategory

createAttributeDefinition
updateAttributeDefinition
deleteAttributeDefinition

createAttributeOption
updateAttributeOption
deleteAttributeOption

suggestCategory
approveCategorySuggestion
rejectCategorySuggestion
```

---

# 43. GraphQL Structure

```text
category/
└── graphql/
    ├── resolver/
    │   ├── CategoryResolver
    │   ├── CategoryAttributeResolver
    │   └── CategorySuggestionResolver
    │
    ├── input/
    │   ├── CreateCategoryInput
    │   ├── UpdateCategoryInput
    │   ├── MoveCategoryInput
    │   ├── CategoryFilterInput
    │   └── AttributeDefinitionInput
    │
    └── payload/
        ├── CategoryPayload
        ├── CategoryListPayload
        ├── CategoryTreePayload
        └── CategoryAttributePayload
```

---

# 44. Authorization

Administrative permissions:

```text
category.read
category.create
category.update
category.delete
category.move
category.publish
category.archive

category.attribute.manage

category.suggestion.read
category.suggestion.review
```

Seller permission:

```text
category.read
category.suggestion.create
```

Customers:

```text
category.read.public
```

---

# 45. Category Events

Recommended events:

```text
CATEGORY_CREATED
CATEGORY_UPDATED
CATEGORY_MOVED
CATEGORY_ACTIVATED
CATEGORY_DEACTIVATED
CATEGORY_ARCHIVED
CATEGORY_DELETED

CATEGORY_ATTRIBUTE_CREATED
CATEGORY_ATTRIBUTE_UPDATED
CATEGORY_ATTRIBUTE_DELETED

CATEGORY_SUGGESTION_CREATED
CATEGORY_SUGGESTION_APPROVED
CATEGORY_SUGGESTION_REJECTED
```

---

# 46. Event Consumers

```text
Category
   │
   ├──► Product
   ├──► Search
   ├──► Analytics
   ├──► Recommendation
   └──► Cache
```

For example:

```text
CATEGORY_UPDATED
       │
       ├──► Search Index
       ├──► Cache Invalidation
       └──► Analytics
```

---

# 47. Search Integration

The Category module should provide structured taxonomy information to Search.

Example:

```text
Product
 │
 └── categoryId
       │
       ▼
Category
       │
       ├── name
       ├── path
       ├── ancestors
       └── attributes
```

Search can index:

```text
categoryId
categoryPath
categoryName
attributeValues
```

---

# 48. SEO

Public categories can provide SEO metadata.

Possible fields:

```text
metaTitle
metaDescription
canonicalUrl
seoSlug
```

However, SEO-specific functionality can later be extracted into a dedicated SEO module.

The initial implementation can keep basic metadata in Category.

---

# 49. Caching

Category trees are strong caching candidates.

Example:

```text
taxonomy:tree
category:{id}
category:slug:{slug}
category:{id}:attributes
```

Invalidate on:

```text
CATEGORY_CREATED
CATEGORY_UPDATED
CATEGORY_MOVED
CATEGORY_ARCHIVED
CATEGORY_ATTRIBUTE_UPDATED
```

---

# 50. Performance Requirements

The module should optimize:

- Category tree retrieval.
- Parent/child queries.
- Breadcrumb generation.
- Attribute retrieval.
- Product-category filtering.
- Public category lookup.

Recommended indexes:

```text
Category
├── taxonomyId
├── parentId
├── slug
├── status
├── visibility
└── sortOrder

AttributeDefinition
├── categoryId
├── code
└── filterable
```

---

# 51. Security Requirements

## CATEGORY-NFR-001

Only authorized administrators can modify marketplace taxonomy.

## CATEGORY-NFR-002

Sellers cannot arbitrarily modify global categories.

## CATEGORY-NFR-003

Category hierarchy modifications must be validated.

## CATEGORY-NFR-004

Category administration must be audited.

## CATEGORY-NFR-005

Public APIs expose only active/public categories.

## CATEGORY-NFR-006

Attribute definitions cannot be modified by unauthorized users.

---

# 52. Testing

## 52.1 Unit Tests

Test:

- Category creation.
- Slug generation.
- Parent-child validation.
- Circular hierarchy prevention.
- Maximum depth.
- Category movement.
- Status transitions.
- Attribute validation.
- Sort ordering.

---

# 53. Integration Tests

Test:

```text
Taxonomy
 │
 ▼
Category
 │
 ├── Child Category
 ├── Attribute Definition
 └── Attribute Option
```

Also test:

```text
Product
 │
 ▼
ProductCategory
 │
 ▼
Category
```

---

# 54. Security Tests

Test:

- Seller modifying category.
- Unauthorized category creation.
- Unauthorized category deletion.
- Unauthorized attribute modification.
- Cross-tenant administrative access.
- Category suggestion privilege escalation.
- Public exposure of inactive categories.

---

# 55. E2E Tests

### Category Creation

```text
Admin
 │
 ▼
Create Category
 │
 ▼
Electronics
 │
 ▼
Create Child
 │
 ▼
Computers
 │
 ▼
Create Child
 │
 ▼
Laptops
```

### Category Attribute

```text
Laptops
 │
 ▼
Create Attribute
 │
 ├── RAM
 ├── Storage
 └── Screen Size
```

### Seller Suggestion

```text
Seller
 │
 ▼
Suggest Category
 │
 ▼
Pending Review
 │
 ▼
Admin
 │
 ├──► Approve
 └──► Reject
```

---

# 56. Architecture

Recommended module structure:

```text
category/
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
└── CategoryModule.java
```

Tests:

```text
test/
└── category/
    ├── service/
    ├── repository/
    ├── policy/
    ├── controller/
    ├── graphql/
    ├── integration/
    └── security/
```

---

# 57. Dependencies

```text
Category
│
├── Authentication
├── User Management
├── Authorization
├── Product Management
├── Storage / Media
├── Common
└── Event Infrastructure
```

Consumers:

```text
Category
 │
 ├── Product
 ├── Search
 ├── Storefront
 ├── Recommendation
 └── Analytics
```

---

# 58. User Stories

## US-001 — Browse Categories

> As a customer, I want to browse product categories so that I can discover products easily.

## US-002 — Category Hierarchy

> As a customer, I want categories to be organized hierarchically so that I can navigate from broad categories to specific products.

## US-003 — Product Classification

> As a seller, I want to assign products to categories so that customers can find them.

## US-004 — Category Attributes

> As an administrator, I want to define attributes for categories so that products have structured information.

## US-005 — Category Filters

> As a customer, I want to filter products by category-specific attributes so that I can find suitable products quickly.

## US-006 — Category Management

> As an administrator, I want to manage the category hierarchy so that Fynza's marketplace remains organized.

## US-007 — Category Suggestion

> As a seller, I want to suggest a category so that I can request classifications that are missing.

---

# 59. Milestones

## M1 — Taxonomy Foundation

- [ ] Taxonomy entity.
- [ ] Category entity.
- [ ] Category hierarchy.
- [ ] Category slug.
- [ ] Category lifecycle.
- [ ] Category CRUD.

## M2 — Category Navigation

- [ ] Category tree.
- [ ] Parent/child queries.
- [ ] Ancestors.
- [ ] Breadcrumbs.
- [ ] Ordering.
- [ ] Visibility.

## M3 — Product Classification

- [ ] Product-category relationship.
- [ ] Primary category.
- [ ] Multiple categories.
- [ ] Category filtering.

## M4 — Attribute System

- [ ] Attribute definitions.
- [ ] Attribute options.
- [ ] Required attributes.
- [ ] Filterable attributes.
- [ ] Searchable attributes.
- [ ] Variant-defining attributes.

## M5 — Administration

- [ ] Category management.
- [ ] Category movement.
- [ ] Category approval.
- [ ] Category suggestions.
- [ ] Audit.
- [ ] Permissions.

## M6 — Optimization

- [ ] Database views.
- [ ] Category caching.
- [ ] Search integration.
- [ ] SEO metadata.
- [ ] Analytics integration.
- [ ] Performance optimization.

---

# 60. Definition of Done

- [ ] Taxonomy implemented.
- [ ] Category entity implemented.
- [ ] Hierarchical categories implemented.
- [ ] Category slugs implemented.
- [ ] Category lifecycle implemented.
- [ ] Category visibility implemented.
- [ ] Category ordering implemented.
- [ ] Category movement implemented.
- [ ] Breadcrumbs implemented.
- [ ] Product-category relationships implemented.
- [ ] Attribute definitions implemented.
- [ ] Attribute options implemented.
- [ ] Filterable attributes implemented.
- [ ] Variant-defining attributes implemented.
- [ ] Category suggestions implemented.
- [ ] Administrative management implemented.
- [ ] Audit implemented.
- [ ] Domain events implemented.
- [ ] REST API implemented.
- [ ] GraphQL API implemented.
- [ ] Read models implemented where required.
- [ ] Caching implemented where required.
- [ ] Unit tests pass.
- [ ] Integration tests pass.
- [ ] Security tests pass.
- [ ] E2E tests pass.
- [ ] Documentation completed.

---

# 61. Architectural Principle

Category Management should own **classification**, not product data.

```text
                  TAXONOMY
                     │
                     ▼
                 CATEGORY
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
      Attributes             Children
          │
          ▼
       PRODUCT
          │
    ┌─────┼─────┐
    ▼     ▼     ▼
 Pricing Inventory Order
```

The key separation is:

```text
Category
  = How is the catalog organized?

Product
  = What is being sold?

Attribute
  = What characteristics describe it?

Variant
  = What purchasable configuration exists?

Pricing
  = How much does it cost?

Inventory
  = How many are available?
```

### Core Rule

> **Category Management defines the structure of the marketplace catalog. Product Management uses that structure; it does not own it.**

This gives Fynza a strong catalog foundation for the next domain: **`09 — Pricing Management PRD`**.