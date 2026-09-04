package com.aoms.aomsbackend.auth.controller;

import com.aoms.aomsbackend.auth.dto.LoginRequest;
import com.aoms.aomsbackend.auth.dto.UserContextResponse;
import com.aoms.aomsbackend.auth.dto.UserProfileResponse;
import com.aoms.aomsbackend.auth.service.AuthV2Service;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/auth")
@Tag(name = "Authentication V2", description = "Email and password authentication endpoints")
public class AuthV2Controller {

    private final AuthV2Service authV2Service;

    public AuthV2Controller(AuthV2Service authV2Service) {
        this.authV2Service = authV2Service;
    }

    @Operation(summary = "Email/password login", description = "Authenticates with email and password",
           responses = {
               @ApiResponse(responseCode = "200", description = "Login successful"),
               @ApiResponse(responseCode = "401", description = "Invalid credentials")
           })
    @PostMapping("/login")
    public ResponseEntity<ResponseWrapper<UserContextResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request) {

        UserContextResponse userContext = authV2Service.login(loginRequest, request);
        return ResponseEntity.ok(ResponseWrapper.success("Login successful.", userContext));
    }

    @Operation(summary = "Get current user", description = "Returns full user profile from session",
           responses = {
               @ApiResponse(responseCode = "200", description = "User profile returned"),
               @ApiResponse(responseCode = "401", description = "Session invalid or expired")
           })
    @GetMapping("/me")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> me(HttpServletRequest request) {
        UserProfileResponse userProfile = authV2Service.getCurrentUser(request);
        return ResponseEntity.ok(ResponseWrapper.success(userProfile));
    }

    @Operation(summary = "Logout", description = "Invalidates session",
           responses = @ApiResponse(responseCode = "200", description = "Logout successful"))
    @PostMapping("/logout")
    public ResponseEntity<ResponseWrapper<Void>> logout(HttpServletRequest request,
                                                        HttpServletResponse response) {
        authV2Service.logout(request, response);
        return ResponseEntity.ok(ResponseWrapper.success("Logged out successfully.", null));
    }

    @Operation(summary = "Validate session",
           responses = {
               @ApiResponse(responseCode = "200", description = "Session valid"),
               @ApiResponse(responseCode = "401", description = "Session invalid or expired")
           })
    @GetMapping("/validate")
    public ResponseEntity<ResponseWrapper<UserContextResponse>> validate(HttpServletRequest request) {
        UserContextResponse userContext = authV2Service.validateSession(request);
        return ResponseEntity.ok(ResponseWrapper.success(userContext));
    }
}
