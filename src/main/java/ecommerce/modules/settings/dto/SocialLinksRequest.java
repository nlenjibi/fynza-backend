package ecommerce.modules.settings.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinksRequest {

    @Size(max = 500, message = "Facebook URL must not exceed 500 characters")
    private String facebookUrl;

    @Size(max = 500, message = "Twitter URL must not exceed 500 characters")
    private String twitterUrl;

    @Size(max = 500, message = "Instagram URL must not exceed 500 characters")
    private String instagramUrl;

    @Size(max = 500, message = "LinkedIn URL must not exceed 500 characters")
    private String linkedinUrl;

    @Size(max = 500, message = "YouTube URL must not exceed 500 characters")
    private String youtubeUrl;

    @Size(max = 500, message = "TikTok URL must not exceed 500 characters")
    private String tiktokUrl;

    @Size(max = 500, message = "Pinterest URL must not exceed 500 characters")
    private String pinterestUrl;

    @Size(max = 50, message = "WhatsApp number must not exceed 50 characters")
    private String whatsappNumber;
}
