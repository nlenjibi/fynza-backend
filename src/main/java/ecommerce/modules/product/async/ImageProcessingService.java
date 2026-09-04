package ecommerce.modules.product.async;

import ecommerce.common.config.AsyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingService {

    private final AsyncProperties asyncProperties;

    @Async("imageExecutor")
    public CompletableFuture<ImageResult> processImageAsync(UUID imageId, String productId, byte[] imageData) {
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Starting async image processing for product: {}, image: {}", 
                correlationId, productId, imageId);

        long timeout = asyncProperties.getTimeouts().getImage();

        var thumbnailFuture = resizeImageAsync(imageData, 150)
                .orTimeout(timeout / 3, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> null);

        var mediumFuture = resizeImageAsync(imageData, 600)
                .orTimeout(timeout / 3, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> null);

        var largeFuture = resizeImageAsync(imageData, 1200)
                .orTimeout(timeout / 3, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> null);

        return allOf(thumbnailFuture, mediumFuture, largeFuture)
                .thenApply(v -> {
                    List<String> urls = List.of(
                            "/images/products/" + productId + "/thumb.jpg",
                            "/images/products/" + productId + "/medium.jpg",
                            "/images/products/" + productId + "/large.jpg"
                    );
                    log.info("[{}] Image processing completed for product: {}", correlationId, productId);
                    return ImageResult.builder()
                            .imageId(imageId)
                            .productId(productId)
                            .status(ImageResult.ImageStatus.COMPLETED)
                            .generatedUrls(urls)
                            .build();
                })
                .exceptionally(ex -> {
                    log.error("[{}] Image processing failed for product: {}", correlationId, productId, ex);
                    return ImageResult.builder()
                            .imageId(imageId)
                            .productId(productId)
                            .status(ImageResult.ImageStatus.FAILED)
                            .errorMessage(ex.getMessage())
                            .build();
                });
    }

    private CompletableFuture<byte[]> resizeImageAsync(byte[] imageData, int targetSize) {
        return supplyAsync(() -> {
            log.debug("Resizing image to {}px", targetSize);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new byte[0];
        });
    }

    @Async("imageExecutor")
    public CompletableFuture<ImageResult> uploadToCdnAsync(ImageResult result) {
        return supplyAsync(() -> {
            log.debug("Uploading images to CDN for product: {}", result.getProductId());
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return result;
        }).thenApply(r -> {
            log.info("Images uploaded to CDN for product: {}", r.getProductId());
            return r;
        });
    }
}
