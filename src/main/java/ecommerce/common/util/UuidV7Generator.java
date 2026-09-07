package ecommerce.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {}

    public static UUID generate() {
        long epochMs = Instant.now().toEpochMilli();
        long msb = (epochMs << 16) | (0x7000L) | (RANDOM.nextLong() & 0x0FFFL);
        long lsb = (0x8000000000000000L) | (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL);
        return new UUID(msb, lsb);
    }
}
