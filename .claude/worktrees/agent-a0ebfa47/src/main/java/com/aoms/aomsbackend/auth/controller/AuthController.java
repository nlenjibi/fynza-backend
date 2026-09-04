package com.aoms.aomsbackend.auth.controller;

import com.aoms.aomsbackend.auth.dto.CallbackRequest;
import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.dto.UserProfileResponse;
import com.aoms.aomsbackend.auth.service.AuthService;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "SSO V2 authentication endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Handle V2 callback", description = "Process the JWT token from SSO and create session",
           responses = {
               @ApiResponse(responseCode = "200", description = "Login successful"),
               @ApiResponse(responseCode = "401", description = "Token verification failed")
           })
    @PostMapping("/callback")
    public ResponseEntity<ResponseWrapper<UserContextResponse>> callback(
            @RequestBody CallbackRequest payload,
            HttpServletRequest request) {
        UserContextResponse userContext = authService.handleV2Callback(payload.getToken(), request);
        return ResponseEntity.ok(ResponseWrapper.success("Login successful", userContext));
    }

    @Operation(summary = "Logout", description = "Invalidates session - frontend handles SSO redirect")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @Operation(summary = "Get current user", description = "Returns full user profile from active session")
    @GetMapping("/me")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> me(HttpServletRequest request) {
        UserProfileResponse userProfile = authService.getCurrentUser(request);
        return ResponseEntity.ok(ResponseWrapper.success(userProfile));
    }

    @Operation(summary = "Validate session", description = "Returns 200 with user context or 401")
    @GetMapping("/validate")
    public ResponseEntity<ResponseWrapper<UserContextResponse>> validate(HttpServletRequest request) {
        UserContextResponse userContext = authService.validateSession(request);
        return ResponseEntity.ok(ResponseWrapper.success(userContext));
    }
}