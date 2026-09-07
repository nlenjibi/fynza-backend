package ecommerce.modules.auth.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.dto.ChangePasswordRequest;
import ecommerce.modules.auth.dto.ForgotPasswordRequest;
import ecommerce.modules.auth.dto.LinkedIdentityResponse;
import ecommerce.modules.auth.dto.LoginRequest;
import ecommerce.modules.auth.dto.LogoutRequest;
import ecommerce.modules.auth.dto.MfaDisableRequest;
import ecommerce.modules.auth.dto.MfaEnableRequest;
import ecommerce.modules.auth.dto.MfaSetupResponse;
import ecommerce.modules.auth.dto.MfaVerifyRequest;
import ecommerce.modules.auth.dto.RefreshTokenRequest;
import ecommerce.modules.auth.dto.RegisterRequest;
import ecommerce.modules.auth.dto.ResendVerificationRequest;
import ecommerce.modules.auth.dto.ResetPasswordRequest;
import ecommerce.modules.auth.dto.SessionResponse;
import ecommerce.modules.auth.dto.VerifyEmailRequest;
import ecommerce.modules.auth.service.AuthService;
import ecommerce.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Refresh token successful", response));

    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(principal.getId(), request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If an account exists for that email, a reset link has been sent", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request.getCurrentPassword(),
                request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Refresh-Token", required = false) String currentRefreshToken) {
        List<SessionResponse> sessions = authService.listActiveSessions(principal.getId(), currentRefreshToken);
        return ResponseEntity.ok(ApiResponse.success("Active sessions retrieved", sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        authService.revokeSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }

    @DeleteMapping("/sessions/others")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeOtherSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LogoutRequest request) {
        authService.revokeAllOtherSessions(principal.getId(), request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("All other sessions revoked", null));
    }

    @PostMapping("/mfa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> mfaSetup(
            @AuthenticationPrincipal UserPrincipal principal) {
        MfaSetupResponse response = authService.setupMfa(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Scan the QR code with your authenticator app, then call /mfa/enable to confirm", response));
    }

    @PostMapping("/mfa/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> mfaEnable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MfaEnableRequest request) {
        authService.enableMfa(principal.getId(), request.getTotpCode());
        return ResponseEntity.ok(ApiResponse.success("MFA enabled successfully", null));
    }

    @PostMapping("/mfa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> mfaDisable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MfaDisableRequest request) {
        authService.disableMfa(principal.getId(), request.getTotpCode());
        return ResponseEntity.ok(ApiResponse.success("MFA disabled successfully", null));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> mfaVerify(
            @Valid @RequestBody MfaVerifyRequest request) {
        AuthResponse response = authService.verifyMfa(request.getChallengeToken(), request.getTotpCode());
        return ResponseEntity.ok(ApiResponse.success("MFA verified, login successful", response));
    }

    @GetMapping("/social-accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LinkedIdentityResponse>>> listSocialAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<LinkedIdentityResponse> accounts = authService.listLinkedIdentities(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Linked social accounts retrieved", accounts));
    }

    @DeleteMapping("/social-accounts/{provider}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unlinkSocialAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String provider) {
        authService.unlinkSocialAccount(principal.getId(), provider.toLowerCase());
        return ResponseEntity.ok(ApiResponse.success("Social account unlinked", null));
    }
}
