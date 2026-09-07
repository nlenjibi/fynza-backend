package ecommerce.modules.settings.dto;

import ecommerce.modules.settings.entity.SocialLinks;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinksResponse {
    private UUID id;
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String linkedinUrl;
    private String youtubeUrl;
    private String tiktokUrl;
    private String pinterestUrl;
    private String whatsappNumber;
    private LocalDateTime updatedAt;

    public static SocialLinksResponse from(SocialLinks links) {
        return SocialLinksResponse.builder()
                .id(links.getPublicId())
                .facebookUrl(links.getFacebookUrl())
                .twitterUrl(links.getTwitterUrl())
                .instagramUrl(links.getInstagramUrl())
                .linkedinUrl(links.getLinkedinUrl())
                .youtubeUrl(links.getYoutubeUrl())
                .tiktokUrl(links.getTiktokUrl())
                .pinterestUrl(links.getPinterestUrl())
                .whatsappNumber(links.getWhatsappNumber())
                .updatedAt(links.getUpdatedAt() != null ? links.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
}
