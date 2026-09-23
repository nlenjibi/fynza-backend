package ecommerce.modules.refund.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.refund.dto.CreateReturnRequest;
import ecommerce.modules.refund.dto.ReturnResponse;
import ecommerce.modules.refund.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/returns")
@RequiredArgsConstructor
@Tag(name = "Customer Returns", description = "Customer return request endpoints")
public class CustomerReturnController {

    private final ReturnService returnService;

    @PostMapping
    @PreAuthorize("hasAuthority('return:create')")
    @Operation(summary = "Request a return", description = "Submit a return request for a delivered order")
    public ResponseEntity<ApiResponse<ReturnResponse>> requestReturn(
            @Valid @RequestBody CreateReturnRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.createReturn(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Return request submitted successfully", response));
    }

    @PostMapping("/{returnId}/cancel")
    @PreAuthorize("hasAuthority('return:cancel')")
    @Operation(summary = "Cancel a return", description = "Cancel an open return request")
    public ResponseEntity<ApiResponse<ReturnResponse>> cancelReturn(
            @PathVariable java.util.UUID returnId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReturnResponse response = returnService.cancelReturn(returnId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Return cancelled successfully", response));
    }
}
