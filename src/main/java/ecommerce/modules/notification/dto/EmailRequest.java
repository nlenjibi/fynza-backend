package ecommerce.modules.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class EmailRequest {

    private final String from;
    private final List<String> to;

    @Builder.Default
    private final List<String> cc = List.of();

    @Builder.Default
    private final List<String> bcc = List.of();

    private final String subject;
    private final String textBody;
    private final String htmlBody;
    private final String replyTo;

    @Builder.Default
    private final Map<String, String> tags = Map.of();
}
