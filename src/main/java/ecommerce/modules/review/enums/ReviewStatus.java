package ecommerce.modules.review.enums;

public enum ReviewStatus {
    DRAFT,
    PENDING_MODERATION,
    PUBLISHED,
    REJECTED,
    HIDDEN,
    FLAGGED,
    DELETED;

    public boolean isPubliclyVisible() {
        return this == PUBLISHED;
    }

    public boolean isFinal() {
        return this == DELETED;
    }
}
