package ecommerce.modules.faq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactOptionsResponse {
    private String liveChat;
    private String emailSupport;
    private String phoneSupport;
    private String phoneHours;
}
