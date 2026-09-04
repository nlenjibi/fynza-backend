package ecommerce.modules.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactStats {
    private long total;
    private long newCount;
    private long inProgress;
    private long responded;
    private long closed;
}
