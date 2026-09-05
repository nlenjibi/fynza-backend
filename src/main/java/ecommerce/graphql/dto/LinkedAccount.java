package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedAccount {
    private String  provider;
    private String  displayName;
    private String  email;
    private String  avatarUrl;
    private Instant linkedAt;
}
