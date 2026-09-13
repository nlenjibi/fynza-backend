package ecommerce.modules.media.config;

import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.RoutingStrategy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "fynza.media")
@Getter
@Setter
public class MediaProperties {

    private ProviderConfig provider = new ProviderConfig();
    private UploadConfig   upload   = new UploadConfig();
    private QuotaConfig    quota    = new QuotaConfig();

    @Getter @Setter
    public static class ProviderConfig {
        private ProviderType    primary         = ProviderType.R2;
        private RoutingStrategy routingStrategy = RoutingStrategy.PRIMARY_ONLY;
        private R2Config        r2              = new R2Config();
        private S3Config        s3              = new S3Config();
    }

    @Getter @Setter
    public static class R2Config {
        private String  accessKeyId;
        private String  secretAccessKey;
        private String  endpoint;
        private String  bucket;
        private String  cdnBaseUrl;
        private long    presignedUrlExpirySeconds = 600;
    }

    @Getter @Setter
    public static class S3Config {
        private String  accessKeyId;
        private String  secretAccessKey;
        private String  region;
        private String  bucket;
        private String  cdnBaseUrl;
        private long    presignedUrlExpirySeconds = 600;
    }

    @Getter @Setter
    public static class UploadConfig {
        private long          maxFileSizeBytes  = 10_485_760L; // 10 MB
        private int           maxWidthPixels    = 8000;
        private int           maxHeightPixels   = 8000;
        private int           sessionTtlMinutes = 15;
        private List<String>  allowedMimeTypes  = List.of(
                "image/jpeg", "image/png", "image/webp", "image/avif", "image/gif"
        );
    }

    @Getter @Setter
    public static class QuotaConfig {
        private long defaultSellerQuotaBytes = 2_147_483_648L; // 2 GB
    }
}
