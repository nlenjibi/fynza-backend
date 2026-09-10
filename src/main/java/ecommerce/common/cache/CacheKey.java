package ecommerce.common.cache;

import java.util.UUID;

public final class CacheKey {

    private static final String PREFIX = "fynza:v1";

    private CacheKey() {}

    public static String of(String entity, String id) {
        return PREFIX + ":" + entity + ":" + id;
    }

    public static String of(String entity, UUID id) {
        return of(entity, id.toString());
    }

    public static String of(String entity, long id) {
        return of(entity, String.valueOf(id));
    }

    public static String tokenBlacklist(String tokenHash) {
        return of("token-blacklist", tokenHash);
    }

    public static String userTokenVersion(UUID userId) {
        return of("user-token-version", userId.toString());
    }

    public static String stockReservation(UUID productId) {
        return of("stock-reservation", productId.toString());
    }

    public static String userPrincipal(UUID userId) {
        return of("user-principal", userId.toString());
    }

    public static String store(String slug) {
        return of("store-slug", slug);
    }

    public static String storeById(UUID publicId) {
        return of("store", publicId.toString());
    }
}
