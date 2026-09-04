package com.aoms.aomsbackend.auth.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserContextResponse {

    String userId;
    String email;
    List<String> roles;
}
