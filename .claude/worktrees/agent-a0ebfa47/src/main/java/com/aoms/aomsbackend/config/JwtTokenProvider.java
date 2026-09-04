package com.aoms.aomsbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@Slf4j
public class JwtTokenProvider {

    private final RSAPublicKey publicKey;
    private final long sessionExpirySeconds;

    public JwtTokenProvider(
            @Value("${auth.public-key-path:classpath:keys/public.pem}") String publicKeyPath,
            @Value("${auth.public-key-content:}") String publicKeyContent,
            @Value("${auth.session-expiry-seconds:86400}") long sessionExpirySeconds,
            ResourceLoader resourceLoader
    ) throws Exception {
        this.publicKey = loadRsaPublicKey(publicKeyPath, publicKeyContent, resourceLoader);
        this.sessionExpirySeconds = sessionExpirySeconds;
        log.info("JWT Token Provider initialized");
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public long getSessionExpirySeconds() {
        return sessionExpirySeconds;
    }

    private RSAPublicKey loadRsaPublicKey(String path, String content, ResourceLoader resourceLoader) throws Exception {
        String pem;
        
        // Try to use content from environment variable first
        if (content != null && !content.isEmpty()) {
            log.info("Loading JWT public key from environment variable (AUTH_PUBLIC_KEY_CONTENT)");
            pem = content;
        } else {
            log.info("Loading JWT public key from file: {}", path);
            pem = readPemResource(path, resourceLoader);
        }
        
        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] decoded = Base64.getDecoder().decode(cleaned);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKey key = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
        log.info("RSA public key loaded — modulus length: {} bits", key.getModulus().bitLength());
        return key;
    }

    private String readPemResource(String path, ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IOException("Public key file not found: " + path);
        }
        try (var is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}