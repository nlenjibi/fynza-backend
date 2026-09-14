package ecommerce.modules.media.controller;

import ecommerce.modules.media.config.MediaProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

@RestController
@RequestMapping("/v1/media/local")
@ConditionalOnProperty(name = "fynza.media.provider.primary", havingValue = "LOCAL")
@RequiredArgsConstructor
public class LocalUploadController {

    private final MediaProperties props;

    @PutMapping("/upload")
    public ResponseEntity<Void> uploadFile(
            @RequestParam("k") String encodedKey,
            HttpServletRequest request) throws IOException {

        String objectKey = new String(
                Base64.getUrlDecoder().decode(encodedKey), StandardCharsets.UTF_8);

        Path storageDir = Paths.get(props.getProvider().getLocal().getStorageDir());
        Path filePath = storageDir.resolve(objectKey).normalize();

        if (!filePath.startsWith(storageDir)) {
            return ResponseEntity.badRequest().build();
        }

        Files.createDirectories(filePath.getParent());
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return ResponseEntity.noContent().build();
    }
}
