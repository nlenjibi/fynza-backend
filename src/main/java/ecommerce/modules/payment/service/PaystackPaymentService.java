package ecommerce.modules.payment.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.payment.config.PaystackProperties;
import ecommerce.modules.payment.dto.PaystackInitializeRequest;
import ecommerce.modules.payment.dto.PaystackInitializeResponse;
import ecommerce.modules.payment.dto.PaystackVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Service for interacting with Paystack payment gateway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackPaymentService {

    private final PaystackProperties paystackProperties;
    private final RestTemplate paystackRestTemplate;

    /**
     * Initialize a payment transaction with Paystack.
     *
     * @param request the payment initialization request
     * @return response containing the authorization URL
     */
    public PaystackInitializeResponse initializePayment(PaystackInitializeRequest request) {
        log.info("Initializing Paystack payment for amount: {} {}", request.getAmount(), request.getCurrency());

        // Generate reference if not provided
        String reference = request.getReference();
        if (reference == null || reference.isBlank()) {
            reference = generateReference();
        }

        // Convert amount to kobo (smallest currency unit for NGN)
        long amountInKobo = request.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();

        String url = paystackProperties.getBaseUrl() + "/transaction/initialize";

        // Build request body
        String requestBody = String.format("""
            {
                "email": "%s",
                "amount": %d,
                "reference": "%s",
                "currency": "%s",
                "callback_url": "%s",
                "metadata": %s
            }
            """,
            request.getEmail(),
            amountInKobo,
            reference,
            request.getCurrency(),
            request.getCallbackUrl() != null ? request.getCallbackUrl() : "",
            request.getMetadata() != null ? request.getMetadata() : "{}"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + paystackProperties.getSecretKey());

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            PaystackInitializeResponse response = paystackRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaystackInitializeResponse.class
            ).getBody();

            if (response != null && response.isStatus()) {
                log.info("Payment initialized successfully. Reference: {}", reference);
                return response;
            } else {
                log.error("Paystack initialization failed: {}", response != null ? response.getMessage() : "Unknown error");
                throw new BadRequestException("Failed to initialize payment: " + (response != null ? response.getMessage() : "Unknown error"));
            }
        } catch (HttpClientErrorException e) {
            log.error("Paystack HTTP error: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Paystack payment initialization failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error initializing Paystack payment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to initialize payment: " + e.getMessage());
        }
    }

    /**
     * Verify a payment transaction with Paystack.
     *
     * @param reference the transaction reference
     * @return response containing the transaction verification data
     */
    public PaystackVerifyResponse verifyPayment(String reference) {
        log.info("Verifying Paystack payment for reference: {}", reference);

        String url = paystackProperties.getBaseUrl() + "/transaction/verify/" + reference;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + paystackProperties.getSecretKey());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            PaystackVerifyResponse response = paystackRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PaystackVerifyResponse.class
            ).getBody();

            if (response != null && response.isStatus()) {
                log.info("Payment verified successfully. Status: {}", response.getData() != null ? response.getData().getStatus() : "unknown");
                return response;
            } else {
                log.error("Paystack verification failed: {}", response != null ? response.getMessage() : "Unknown error");
                throw new BadRequestException("Failed to verify payment: " + (response != null ? response.getMessage() : "Unknown error"));
            }
        } catch (HttpClientErrorException e) {
            log.error("Paystack HTTP error: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Paystack payment verification failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error verifying Paystack payment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to verify payment: " + e.getMessage());
        }
    }

    /**
     * Process a refund for a completed transaction.
     *
     * @param reference the transaction reference
     * @param amount the amount to refund (optional, null for full refund)
     * @return success message
     */
    public String processRefund(String reference, java.math.BigDecimal amount) {
        log.info("Processing Paystack refund for reference: {}, amount: {}", reference, amount);

        String url = paystackProperties.getBaseUrl() + "/transaction/refund";

        String requestBody;
        if (amount != null) {
            long amountInKobo = amount.multiply(java.math.BigDecimal.valueOf(100)).longValue();
            requestBody = String.format("{\"transaction\":\"%s\",\"amount\":%d}", reference, amountInKobo);
        } else {
            requestBody = String.format("{\"transaction\":\"%s\"}", reference);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + paystackProperties.getSecretKey());

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            var response = paystackRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            ).getBody();

            log.info("Refund processed successfully: {}", response);
            return "Refund processed successfully";
        } catch (Exception e) {
            log.error("Error processing refund: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Generate a unique reference for the transaction.
     */
    private String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}