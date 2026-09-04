package ecommerce.modules.settings.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.settings.dto.*;
import ecommerce.modules.settings.service.SiteSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings Management", description = "APIs for managing platform settings")
public class SettingsController {

    private final SiteSettingsService siteSettingsService;

    @GetMapping("/general")
    @Operation(summary = "Get general settings")
    public ResponseEntity<ApiResponse<GeneralSettingsResponse>> getGeneralSettings() {
        return ResponseEntity.ok(ApiResponse.success("General settings retrieved", 
                siteSettingsService.getGeneralSettings()));
    }

    @PutMapping("/general")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update general settings")
    public ResponseEntity<ApiResponse<GeneralSettingsResponse>> updateGeneralSettings(
            @Valid @RequestBody GeneralSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("General settings updated", 
                siteSettingsService.updateGeneralSettings(request)));
    }

    @GetMapping("/payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment settings")
    public ResponseEntity<ApiResponse<PaymentSettingsResponse>> getPaymentSettings() {
        return ResponseEntity.ok(ApiResponse.success("Payment settings retrieved", 
                siteSettingsService.getPaymentSettings()));
    }

    @PutMapping("/payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update payment settings")
    public ResponseEntity<ApiResponse<PaymentSettingsResponse>> updatePaymentSettings(
            @Valid @RequestBody PaymentSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment settings updated", 
                siteSettingsService.updatePaymentSettings(request)));
    }

    @GetMapping("/shipping")
    @Operation(summary = "Get shipping settings")
    public ResponseEntity<ApiResponse<ShippingSettingsResponse>> getShippingSettings() {
        return ResponseEntity.ok(ApiResponse.success("Shipping settings retrieved", 
                siteSettingsService.getShippingSettings()));
    }

    @PutMapping("/shipping")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update shipping settings")
    public ResponseEntity<ApiResponse<ShippingSettingsResponse>> updateShippingSettings(
            @Valid @RequestBody ShippingSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shipping settings updated", 
                siteSettingsService.updateShippingSettings(request)));
    }

    @GetMapping("/tax")
    @Operation(summary = "Get tax settings")
    public ResponseEntity<ApiResponse<TaxSettingsResponse>> getTaxSettings() {
        return ResponseEntity.ok(ApiResponse.success("Tax settings retrieved", 
                siteSettingsService.getTaxSettings()));
    }

    @PutMapping("/tax")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tax settings")
    public ResponseEntity<ApiResponse<TaxSettingsResponse>> updateTaxSettings(
            @Valid @RequestBody TaxSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tax settings updated", 
                siteSettingsService.updateTaxSettings(request)));
    }

    @GetMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get email settings")
    public ResponseEntity<ApiResponse<EmailSettingsResponse>> getEmailSettings() {
        return ResponseEntity.ok(ApiResponse.success("Email settings retrieved", 
                siteSettingsService.getEmailSettings()));
    }

    @PutMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update email settings")
    public ResponseEntity<ApiResponse<EmailSettingsResponse>> updateEmailSettings(
            @Valid @RequestBody EmailSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Email settings updated", 
                siteSettingsService.updateEmailSettings(request)));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get notification settings")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getNotificationSettings() {
        return ResponseEntity.ok(ApiResponse.success("Notification settings retrieved", 
                siteSettingsService.getNotificationSettings()));
    }

    @PutMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update notification settings")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateNotificationSettings(
            @Valid @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Notification settings updated", 
                siteSettingsService.updateNotificationSettings(request)));
    }

    @GetMapping("/security")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get security settings")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> getSecuritySettings() {
        return ResponseEntity.ok(ApiResponse.success("Security settings retrieved", 
                siteSettingsService.getSecuritySettings()));
    }

    @PutMapping("/security")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update security settings")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> updateSecuritySettings(
            @Valid @RequestBody SecuritySettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Security settings updated", 
                siteSettingsService.updateSecuritySettings(request)));
    }

    @GetMapping("/social")
    @Operation(summary = "Get social links")
    public ResponseEntity<ApiResponse<SocialLinksResponse>> getSocialLinks() {
        return ResponseEntity.ok(ApiResponse.success("Social links retrieved", 
                siteSettingsService.getSocialLinks()));
    }

    @PutMapping("/social")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update social links")
    public ResponseEntity<ApiResponse<SocialLinksResponse>> updateSocialLinks(
            @Valid @RequestBody SocialLinksRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Social links updated", 
                siteSettingsService.updateSocialLinks(request)));
    }
}
