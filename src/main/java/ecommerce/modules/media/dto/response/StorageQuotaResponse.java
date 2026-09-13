package ecommerce.modules.media.dto.response;

public record StorageQuotaResponse(
        long   storageBytes,
        long   objectCount,
        long   quotaBytes,
        double usagePercent
) {}
