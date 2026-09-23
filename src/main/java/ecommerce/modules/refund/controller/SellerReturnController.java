package ecommerce.modules.refund.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.refund.dto.CompleteInspectionRequest;
import ecommerce.modules.refund.dto.InitiateReturnShipmentRequest;
import ecommerce.modules.refund.dto.ReturnDecisionRequest;
import ecommerce.modules.refund.dto.ReturnResponse;
import ecommerce.modules.refund.dto.ReturnRefundResponse;
import ecommerce.modules.refund.service.ReturnInspectionService;
import ecommerce.modules.refund.service.ReturnRefundService;
import ecommerce.modules.refund.service.ReturnService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/returns")
@RequiredArgsConstructor
@Tag(name = "Seller Returns", description = "Seller return management endpoints")
public class SellerReturnController {

    private final ReturnService returnService;
    private final ReturnInspectionService returnInspectionService;
    private final ReturnRefundService returnRefundService;

    @PostMapping("/{returnId}/approve")
    @PreAuthorize("hasAuthority('return:approve')")
    @Operation(summary = "Approve return", description = "Approve a customer return request")
    public ResponseEntity<ApiResponse<ReturnResponse>> approveReturn(
            @PathVariable UUID returnId,
            @RequestBody(required = false) ReturnDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String note = request != null ? request.getNote() : null;
        ReturnResponse response = returnService.approveReturn(returnId, principal.getId(), note);
        return ResponseEntity.ok(ApiResponse.success("Return approved", response));
    }

    @PostMapping("/{returnId}/reject")
    @PreAuthorize("hasAuthority('return:reject')")
    @Operation(summary = "Reject return", description = "Reject a customer return request with a reason")
    public ResponseEntity<ApiResponse<ReturnResponse>> rejectReturn(
            @PathVariable UUID returnId,
            @RequestBody ReturnDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.rejectReturn(
                returnId, principal.getId(), request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success("Return rejected", response));
    }

    @PostMapping("/{returnId}/review")
    @PreAuthorize("hasAuthority('return:review')")
    @Operation(summary = "Start review", description = "Begin reviewing a return request")
    public ResponseEntity<ApiResponse<ReturnResponse>> startReview(
            @PathVariable UUID returnId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.markUnderReview(returnId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Return under review", response));
    }

    @PostMapping("/{returnId}/inspect")
    @PreAuthorize("hasAuthority('return:inspect')")
    @Operation(summary = "Start inspection", description = "Move return to INSPECTION status")
    public ResponseEntity<ApiResponse<ReturnResponse>> startInspection(
            @PathVariable UUID returnId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.startInspection(returnId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Return inspection started", response));
    }

    @PostMapping("/{returnId}/complete-inspection")
    @PreAuthorize("hasAuthority('return:inspect')")
    @Operation(summary = "Complete inspection", description = "Record per-item inspection results and determine resolution")
    public ResponseEntity<ApiResponse<ReturnResponse>> completeInspection(
            @PathVariable UUID returnId,
            @Valid @RequestBody CompleteInspectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnInspectionService.completeInspection(returnId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Inspection completed", response));
    }

    @PostMapping("/{returnId}/request-refund")
    @PreAuthorize("hasAuthority('return:resolve')")
    @Operation(summary = "Request refund", description = "Trigger refund for an approved return — calculates amount server-side and calls Payment Management")
    public ResponseEntity<ApiResponse<ReturnRefundResponse>> requestRefund(
            @PathVariable UUID returnId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnRefundResponse response = returnRefundService.requestRefund(returnId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Refund requested", response));
    }

    @PostMapping("/{returnId}/initiate-shipment")
    @PreAuthorize("hasAuthority('return:approve')")
    @Operation(summary = "Initiate return shipment", description = "Link a Shipping module shipment to this return and move to RETURN_SHIPPING")
    public ResponseEntity<ApiResponse<ReturnResponse>> initiateShipment(
            @PathVariable UUID returnId,
            @Valid @RequestBody InitiateReturnShipmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.initiateReturnShipment(returnId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Return shipment initiated", response));
    }
}
