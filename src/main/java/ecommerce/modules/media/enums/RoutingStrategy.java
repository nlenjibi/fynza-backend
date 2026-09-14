package ecommerce.modules.media.enums;

public enum RoutingStrategy {
    PRIMARY_ONLY,
    FAILOVER,
    ROUND_ROBIN,
    BY_MEDIA_TYPE,
    BY_TENANT,
    BY_REGION,
    BY_COST
}
