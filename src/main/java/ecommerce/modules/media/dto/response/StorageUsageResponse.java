package ecommerce.modules.media.dto.response;

import ecommerce.modules.media.entity.StorageUsage;
import ecommerce.modules.media.enums.MediaOwnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsageResponse {

    private UUID          ownerId;
    private MediaOwnerType ownerType;
    private long          storageBytes;
    private long          objectCount;
    private long          bandwidthBytes;
    private long          quotaBytes;
    private double        percentUsed;
    private Instant       updatedAt;

    public static StorageUsageResponse from(StorageUsage usage, long quotaBytes) {
        double percent = quotaBytes > 0 ? usage.getStorageBytes() * 100.0 / quotaBytes : 0.0;
        return StorageUsageResponse.builder()
                .ownerId(usage.getOwnerId())
                .ownerType(usage.getOwnerType())
                .storageBytes(usage.getStorageBytes())
                .objectCount(usage.getObjectCount())
                .bandwidthBytes(usage.getBandwidthBytes())
                .quotaBytes(quotaBytes)
                .percentUsed(percent)
                .updatedAt(usage.getUpdatedAt())
                .build();
    }
}
