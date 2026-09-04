package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.dto.LoginRequest;
import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthV2Service {

    UserContextResponse login(LoginRequest request, HttpServletRequest httpRequest);

    UserProfileResponse getCurrentUser(HttpServletRequest request);

    void logout(HttpServletRequest request, HttpServletResponse response);

    UserContextResponse validateSession(HttpServletRequest request);
}
