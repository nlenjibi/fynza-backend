package ecommerce.modules.notification.provider;

import ecommerce.modules.notification.config.SlackProperties;
import ecommerce.modules.notification.exceptions.SlackDispatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class SlackClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT    = Duration.ofSeconds(10);

    private static final Set<String> NON_RETRYABLE_ERRORS = Set.of(
            "channel_not_found", "not_in_channel", "is_archived",
            "invalid_auth", "account_inactive", "missing_scope", "token_revoked");

    private final RestClient      restClient;
    private final SlackProperties properties;

    public SlackClient(SlackProperties properties) {
        this.properties = properties;

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build());
        factory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public boolean isConfigured() {
        String token = properties.getBotToken();
        return token != null && !token.isBlank();
    }

    public void send(String channelId, String text) {
        requireConfigured();

        SlackResponses.ChatPostMessageResponse response;
        try {
            response = restClient.post()
                    .uri("/chat.postMessage")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .contentType(MediaType.valueOf("application/json; charset=utf-8"))
                    .body(Map.of("channel", channelId, "text", text))
                    .retrieve()
                    .body(SlackResponses.ChatPostMessageResponse.class);
        } catch (RestClientResponseException ex) {
            throw new SlackDispatchException("chat.postMessage HTTP " + ex.getStatusCode(), ex, true);
        } catch (Exception ex) {
            throw new SlackDispatchException("chat.postMessage error: " + ex.getMessage(), ex, true);
        }

        if (response == null) throw new SlackDispatchException("Empty response from chat.postMessage", null, true);
        if (!response.ok()) throw classify("chat.postMessage", response.error());
        log.info("[Slack] Message sent channel={}", channelId);
    }

    public Optional<String> lookupUserIdByEmail(String email) {
        requireConfigured();

        SlackResponses.UserLookupResponse response;
        try {
            response = restClient.get()
                    .uri(b -> b.path("/users.lookupByEmail").queryParam("email", email).build())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .body(SlackResponses.UserLookupResponse.class);
        } catch (RestClientResponseException ex) {
            throw new SlackDispatchException("users.lookupByEmail HTTP " + ex.getStatusCode(), ex, true);
        } catch (Exception ex) {
            throw new SlackDispatchException("users.lookupByEmail error", ex, true);
        }

        if (response == null) throw new SlackDispatchException("Empty response from users.lookupByEmail", null, true);
        if (!response.ok()) {
            if ("users_not_found".equals(response.error())) return Optional.empty();
            throw classify("users.lookupByEmail", response.error());
        }
        return Optional.ofNullable(response.user()).map(SlackResponses.UserLookupResponse.SlackUser::id);
    }

    public Optional<String> openDirectMessageChannel(String slackUserId) {
        requireConfigured();

        SlackResponses.ConversationOpenResponse response;
        try {
            response = restClient.post()
                    .uri("/conversations.open")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("users", slackUserId))
                    .retrieve()
                    .body(SlackResponses.ConversationOpenResponse.class);
        } catch (RestClientResponseException ex) {
            throw new SlackDispatchException("conversations.open HTTP " + ex.getStatusCode(), ex, true);
        } catch (Exception ex) {
            throw new SlackDispatchException("conversations.open error", ex, true);
        }

        if (response == null) throw new SlackDispatchException("Empty response from conversations.open", null, true);
        if (!response.ok()) throw classify("conversations.open", response.error());
        return Optional.ofNullable(response.channel()).map(SlackResponses.ConversationOpenResponse.SlackChannel::id);
    }

    private void requireConfigured() {
        if (!isConfigured()) throw new SlackDispatchException("Slack bot token is not configured.", null, false);
    }

    private String bearerToken() { return "Bearer " + properties.getBotToken(); }

    private SlackDispatchException classify(String method, String error) {
        boolean retryable = error == null || !NON_RETRYABLE_ERRORS.contains(error);
        String msg = "Slack " + method + " failed: error=" + error;
        if (retryable) log.error("[Slack] {}", msg); else log.warn("[Slack] {}", msg);
        return new SlackDispatchException(msg, null, retryable);
    }
}
