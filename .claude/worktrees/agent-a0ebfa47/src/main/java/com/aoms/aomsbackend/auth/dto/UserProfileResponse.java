package com.aoms.aomsbackend.auth.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserProfileResponse {

    String userId;
    String email;
    List<String> roles;
    String firstName;
    String lastName;
    String otherName;
    String profileImage;
    String office;
    String organization;
}
