package ecommerce.modules.notification.provider;

/**
 * Slack Web API response shapes. Every Slack call returns HTTP 200 even on failure;
 * SlackClient checks ok explicitly before trusting the rest of the payload.
 */
final class SlackResponses {

    private SlackResponses() {}

    record ChatPostMessageResponse(boolean ok, String error, String channel, String ts) {}

    record UserLookupResponse(boolean ok, String error, SlackUser user) {
        record SlackUser(String id) {}
    }

    record ConversationOpenResponse(boolean ok, String error, SlackChannel channel) {
        record SlackChannel(String id) {}
    }
}
