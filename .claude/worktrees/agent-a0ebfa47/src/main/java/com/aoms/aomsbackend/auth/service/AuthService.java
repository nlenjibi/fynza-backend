package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    UserContextResponse handleV2Callback(String appToken, HttpServletRequest request);

    UserProfileResponse getCurrentUser(HttpServletRequest request);

    void logout(HttpServletRequest request);

    UserContextResponse validateSession(HttpServletRequest request);
}