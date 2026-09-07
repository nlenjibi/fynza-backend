package ecommerce.common.security;

import com.google.common.io.BaseEncoding;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * RFC 6238 TOTP implementation backed by Guava Base32 and Java's HmacSHA1.
 * Compatible with Google Authenticator, Authy, and any RFC 6238 client.
 */
public final class TotpUtil {

    private static final int DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int WINDOW = 1;      // ±1 step tolerance for clock skew
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpUtil() {}

    public static String generateSecret() {
        byte[] bytes = new byte[20];           // 160-bit secret (TOTP standard minimum)
        RANDOM.nextBytes(bytes);
        return BaseEncoding.base32().encode(bytes);
    }

    /**
     * Returns an otpauth:// URI consumable by any TOTP authenticator app.
     */
    public static String generateQrUri(String issuer, String accountEmail, String secret) {
        String label = encode(issuer + ":" + accountEmail);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + encode(issuer)
                + "&digits=" + DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    /**
     * Validates a 6-digit code against the secret, accepting ±1 time window.
     */
    public static boolean verify(String secret, String codeStr) {
        int code;
        try {
            code = Integer.parseInt(codeStr);
        } catch (NumberFormatException e) {
            return false;
        }
        long currentStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (totp(secret, currentStep + i) == code) return true;
        }
        return false;
    }

    private static int totp(String secret, long timeStep) {
        byte[] key = BaseEncoding.base32().decode(secret.toUpperCase());
        byte[] msg = ByteBuffer.allocate(8).putLong(timeStep).array();
        byte[] hash = hmacSha1(key, msg);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset]     & 0x7F) << 24)
                   | ((hash[offset + 1] & 0xFF) << 16)
                   | ((hash[offset + 2] & 0xFF) << 8)
                   |  (hash[offset + 3] & 0xFF);
        return binary % (int) Math.pow(10, DIGITS);
    }

    private static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA1 unavailable", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
