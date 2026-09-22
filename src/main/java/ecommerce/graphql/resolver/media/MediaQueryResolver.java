package ecommerce.graphql.resolver.media;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.media.dto.response.MediaAssetPage;
import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.ProductMediaItemResponse;
import ecommerce.modules.media.dto.response.StorageQuotaResponse;
import ecommerce.modules.media.dto.response.StorageUsageResponse;
import ecommerce.modules.media.enums.MediaStatus;
import ecommerce.modules.media.service.MediaQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MediaQueryResolver {

    private final MediaQueryService queryService;

    @QueryMapping
    @PreAuthorize("hasAuthority('media.read')")
    public MediaAssetResponse mediaAsset(@Argument String publicId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL mediaAsset publicId={}", publicId);
        return queryService.getAsset(UUID.fromString(publicId), principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<ProductMediaItemResponse> productMedia(@Argument String productId) {
        log.debug("GQL productMedia productId={}", productId);
        return queryService.getProductMedia(UUID.fromString(productId));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('media.quota.read')")
    public StorageQuotaResponse myStorageQuota(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myStorageQuota userId={}", principal.getId());
        return queryService.getStorageQuota(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('media.read')")
    public MediaAssetPage myMediaAssets(@Argument Integer page,
                                        @Argument Integer size,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myMediaAssets userId={}", principal.getId());
        return queryService.getMyAssets(principal.getId(),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('media.quota.read')")
    public StorageUsageResponse myStorageUsage(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myStorageUsage userId={}", principal.getId());
        return queryService.getStorageUsage(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('media.admin.read')")
    public MediaAssetPage adminMediaAssets(@Argument String status,
                                           @Argument Integer page,
                                           @Argument Integer size) {
        log.debug("GQL adminMediaAssets status={}", status);
        MediaStatus mediaStatus = status != null ? MediaStatus.valueOf(status) : null;
        return queryService.adminListAssets(mediaStatus,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }
}
