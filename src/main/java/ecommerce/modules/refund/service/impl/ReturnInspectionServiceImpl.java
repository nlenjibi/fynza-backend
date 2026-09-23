package ecommerce.modules.refund.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.refund.ReturnAuditRecorder;
import ecommerce.modules.refund.ReturnStateMachine;
import ecommerce.modules.refund.dto.*;
import ecommerce.modules.refund.entity.Return;
import ecommerce.modules.refund.entity.ReturnDisposition;
import ecommerce.modules.refund.entity.ReturnInspection;
import ecommerce.modules.refund.entity.ReturnItem;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnResolution;
import ecommerce.modules.refund.enums.ReturnStatus;
import ecommerce.modules.refund.exception.ReturnNotFoundException;
import ecommerce.modules.refund.repository.*;
import ecommerce.modules.refund.service.ReturnInspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReturnInspectionServiceImpl implements ReturnInspectionService {

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnInspectionRepository inspectionRepository;
    private final ReturnDispositionRepository dispositionRepository;
    private final ReturnAuditRecorder auditRecorder;

    @Override
    @Transactional
    public ReturnResponse completeInspection(UUID returnPublicId, CompleteInspectionRequest request, UUID inspectedBy) {
        Return returnEntity = findByPublicId(returnPublicId);
        ReturnStatus prev = returnEntity.getStatus();

        ReturnStatus nextStatus = resolveNextStatus(request.getResolution());
        ReturnStateMachine.validate(prev, nextStatus);

        List<ReturnInspection> inspections = new ArrayList<>();
        List<ReturnDisposition> dispositions = new ArrayList<>();

        for (InspectionItemResult itemResult : request.getItemResults()) {
            ReturnItem item = returnItemRepository.findByPublicId(itemResult.getReturnItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Return item not found: " + itemResult.getReturnItemId()));

            if (itemResult.getApprovedQuantity() != null) item.setApprovedQuantity(itemResult.getApprovedQuantity());
            if (itemResult.getReceivedQuantity() != null) item.setReceivedQuantity(itemResult.getReceivedQuantity());
            item.setResolution(request.getResolution());
            returnItemRepository.save(item);

            ReturnInspection inspection = ReturnInspection.builder()
                    .returnId(returnPublicId)
                    .returnItemId(item.getPublicId())
                    .inspectedBy(inspectedBy)
                    .condition(itemResult.getCondition())
                    .result(itemResult.getResult())
                    .notes(itemResult.getNotes())
                    .build();
            inspections.add(inspection);

            if (itemResult.getDispositionType() != null) {
                int qty = itemResult.getDispositionQuantity() != null ? itemResult.getDispositionQuantity() : item.getQuantity();
                ReturnDisposition disposition = ReturnDisposition.builder()
                        .returnId(returnPublicId)
                        .returnItemId(item.getPublicId())
                        .type(itemResult.getDispositionType())
                        .quantity(qty)
                        .locationId(itemResult.getLocationId())
                        .reason(itemResult.getNotes())
                        .processedBy(inspectedBy)
                        .build();
                dispositions.add(disposition);
            }
        }

        inspectionRepository.saveAll(inspections);
        if (!dispositions.isEmpty()) {
            dispositionRepository.saveAll(dispositions);
            auditRecorder.record(returnPublicId, ReturnAuditAction.DISPOSITION_RECORDED,
                    prev, prev, inspectedBy, dispositions.size() + " disposition(s) recorded");
        }

        returnEntity.setStatus(nextStatus);
        returnEntity = returnRepository.save(returnEntity);

        auditRecorder.record(returnPublicId, ReturnAuditAction.INSPECTION_COMPLETED,
                prev, nextStatus, inspectedBy,
                "Inspection completed — resolution: " + request.getResolution()
                        + (request.getOverallNotes() != null ? " — " + request.getOverallNotes() : ""));

        log.info("Return {} inspection completed by {} → {} ({})",
                returnEntity.getReturnNumber(), inspectedBy, nextStatus, request.getResolution());

        List<ReturnItem> items = returnItemRepository.findByReturnId(returnPublicId);
        return toResponse(returnEntity, items);
    }

    @Override
    public List<ReturnInspectionResponse> getInspections(UUID returnPublicId) {
        return inspectionRepository.findByReturnIdOrderByCreatedAtAsc(returnPublicId)
                .stream().map(this::toInspectionResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReturnDispositionResponse> getDispositions(UUID returnPublicId) {
        return dispositionRepository.findByReturnIdOrderByCreatedAtAsc(returnPublicId)
                .stream().map(this::toDispositionResponse).collect(Collectors.toList());
    }

    private ReturnStatus resolveNextStatus(ReturnResolution resolution) {
        return switch (resolution) {
            case FULL_REFUND    -> ReturnStatus.APPROVED_FOR_REFUND;
            case PARTIAL_REFUND -> ReturnStatus.PARTIALLY_APPROVED;
            case REJECT_RETURN  -> ReturnStatus.REJECTED;
        };
    }

    private Return findByPublicId(UUID publicId) {
        return returnRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ReturnNotFoundException("Return not found: " + publicId));
    }

    private ReturnResponse toResponse(Return r, List<ReturnItem> items) {
        return ReturnResponse.builder()
                .publicId(r.getPublicId())
                .returnNumber(r.getReturnNumber())
                .orderId(r.getOrderId())
                .customerId(r.getCustomerId())
                .sellerId(r.getSellerId())
                .storeId(r.getStoreId())
                .status(r.getStatus())
                .reason(r.getReason())
                .customerNote(r.getCustomerNote())
                .adminNote(r.getAdminNote())
                .rejectionReason(r.getRejectionReason())
                .returnDeadline(r.getReturnDeadline())
                .requestedAt(r.getRequestedAt())
                .approvedAt(r.getApprovedAt())
                .receivedAt(r.getReceivedAt())
                .rejectedAt(r.getRejectedAt())
                .resolvedAt(r.getResolvedAt())
                .returnShipmentId(r.getReturnShipmentId())
                .returnLabelReference(r.getReturnLabelReference())
                .isEscalated(r.getIsEscalated())
                .escalatedAt(r.getEscalatedAt())
                .items(items.stream().map(this::toItemResponse).collect(Collectors.toList()))
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private ReturnItemResponse toItemResponse(ReturnItem i) {
        return ReturnItemResponse.builder()
                .publicId(i.getPublicId())
                .orderItemId(i.getOrderItemId())
                .productId(i.getProductId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .reason(i.getReason())
                .condition(i.getCondition())
                .unitPrice(i.getUnitPrice())
                .approvedQuantity(i.getApprovedQuantity())
                .receivedQuantity(i.getReceivedQuantity())
                .resolution(i.getResolution())
                .createdAt(i.getCreatedAt())
                .build();
    }

    private ReturnInspectionResponse toInspectionResponse(ReturnInspection i) {
        return ReturnInspectionResponse.builder()
                .publicId(i.getPublicId())
                .returnId(i.getReturnId())
                .returnItemId(i.getReturnItemId())
                .inspectedBy(i.getInspectedBy())
                .condition(i.getCondition())
                .result(i.getResult())
                .notes(i.getNotes())
                .inspectedAt(i.getInspectedAt())
                .createdAt(i.getCreatedAt())
                .build();
    }

    private ReturnDispositionResponse toDispositionResponse(ReturnDisposition d) {
        return ReturnDispositionResponse.builder()
                .publicId(d.getPublicId())
                .returnId(d.getReturnId())
                .returnItemId(d.getReturnItemId())
                .type(d.getType())
                .quantity(d.getQuantity())
                .locationId(d.getLocationId())
                .reason(d.getReason())
                .processedBy(d.getProcessedBy())
                .processedAt(d.getProcessedAt())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
