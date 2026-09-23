package ecommerce.modules.refund.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.refund.dto.EscalateReturnRequest;
import ecommerce.modules.refund.dto.ReturnDecisionRequest;
import ecommerce.modules.refund.dto.ReturnResponse;
import ecommerce.modules.refund.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/returns")
@RequiredArgsConstructor
@Tag(name = "Admin Returns", description = "Admin return management endpoints")
public class AdminReturnController {

    private final ReturnService returnService;

    @PostMapping("/{returnId}/review")
    @PreAuthorize("hasAuthority('return:review')")
    @Operation(summary = "Start review", description = "Move a return to UNDER_REVIEW")
    public ResponseEntity<ApiResponse<ReturnResponse>> startReview(
            @PathVariable UUID returnId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.markUnderReview(returnId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Return moved to review", response));
    }

    @PostMapping("/{returnId}/approve")
    @PreAuthorize("hasAuthority('return:approve')")
    @Operation(summary = "Approve return", description = "Approve a return request")
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
    @Operation(summary = "Reject return", description = "Reject a return request with a reason")
    public ResponseEntity<ApiResponse<ReturnResponse>> rejectReturn(
            @PathVariable UUID returnId,
            @RequestBody ReturnDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.rejectReturn(
                returnId, principal.getId(), request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.success("Return rejected", response));
    }

    @PostMapping("/{returnId}/escalate")
    @PreAuthorize("hasAuthority('return:admin.manage')")
    @Operation(summary = "Escalate return", description = "Flag a return for escalated review")
    public ResponseEntity<ApiResponse<ReturnResponse>> escalateReturn(
            @PathVariable UUID returnId,
            @jakarta.validation.Valid @RequestBody EscalateReturnRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.escalateReturn(returnId, principal.getId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Return escalated", response));
    }

    @PostMapping("/{returnId}/in-transit")
    @PreAuthorize("hasAuthority('return:admin.manage')")
    @Operation(summary = "Mark in transit", description = "Manually mark return package as in transit (admin override)")
    public ResponseEntity<ApiResponse<ReturnResponse>> markInTransit(
            @PathVariable UUID returnId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.markInTransit(returnId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Return marked in transit", response));
    }

    @PostMapping("/{returnId}/received")
    @PreAuthorize("hasAuthority('return:admin.manage')")
    @Operation(summary = "Mark received", description = "Mark return package as received at warehouse")
    public ResponseEntity<ApiResponse<ReturnResponse>> markReceived(
            @PathVariable UUID returnId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.markReceived(returnId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Return package received", response));
    }
}
