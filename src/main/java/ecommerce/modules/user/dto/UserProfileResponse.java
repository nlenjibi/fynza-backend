package ecommerce.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class UserProfileResponse {

    UUID id;
    String email;
    String username;
    String firstName;
    String lastName;
    String displayName;
    String phone;
    String avatarUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth;

    String language;
    String timezone;
    String currency;
    Role role;
    UserStatus status;
    Boolean emailVerified;
    Boolean mfaEnabled;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant deletionRequestedAt;
}
