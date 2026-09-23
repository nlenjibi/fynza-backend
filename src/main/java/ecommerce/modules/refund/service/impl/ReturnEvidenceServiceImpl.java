package ecommerce.modules.refund.service.impl;

import ecommerce.modules.refund.ReturnAuditRecorder;
import ecommerce.modules.refund.dto.AddReturnEvidenceRequest;
import ecommerce.modules.refund.dto.ReturnAuditResponse;
import ecommerce.modules.refund.dto.ReturnEvidenceResponse;
import ecommerce.modules.refund.entity.ReturnEvidence;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.exception.ReturnNotFoundException;
import ecommerce.modules.refund.repository.ReturnAuditRepository;
import ecommerce.modules.refund.repository.ReturnEvidenceRepository;
import ecommerce.modules.refund.repository.ReturnRepository;
import ecommerce.modules.refund.service.ReturnEvidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReturnEvidenceServiceImpl implements ReturnEvidenceService {

    private final ReturnEvidenceRepository evidenceRepository;
    private final ReturnAuditRepository auditRepository;
    private final ReturnRepository returnRepository;
    private final ReturnAuditRecorder auditRecorder;

    @Override
    @Transactional
    public ReturnEvidenceResponse addEvidence(UUID returnPublicId, AddReturnEvidenceRequest request, UUID uploadedBy) {
        returnRepository.findByPublicId(returnPublicId)
                .orElseThrow(() -> new ReturnNotFoundException("Return not found: " + returnPublicId));

        ReturnEvidence evidence = ReturnEvidence.builder()
                .returnId(returnPublicId)
                .returnItemId(request.getReturnItemId())
                .mediaReference(request.getMediaReference())
                .evidenceType(request.getEvidenceType())
                .description(request.getDescription())
                .uploadedBy(uploadedBy)
                .build();

        evidence = evidenceRepository.save(evidence);

        auditRecorder.record(returnPublicId, ReturnAuditAction.EVIDENCE_ADDED, uploadedBy,
                request.getEvidenceType() + " evidence added");

        log.info("Evidence {} added to return {} by {}", evidence.getPublicId(), returnPublicId, uploadedBy);
        return toResponse(evidence);
    }

    @Override
    public List<ReturnEvidenceResponse> getEvidence(UUID returnPublicId) {
        return evidenceRepository.findByReturnIdOrderByCreatedAtAsc(returnPublicId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReturnAuditResponse> getAuditTrail(UUID returnPublicId) {
        return auditRepository.findByReturnIdOrderByCreatedAtAsc(returnPublicId)
                .stream()
                .map(a -> ReturnAuditResponse.builder()
                        .id(a.getId())
                        .returnId(a.getReturnId())
                        .action(a.getAction())
                        .previousStatus(a.getPreviousStatus())
                        .newStatus(a.getNewStatus())
                        .performedBy(a.getPerformedBy())
                        .reason(a.getReason())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private ReturnEvidenceResponse toResponse(ReturnEvidence e) {
        return ReturnEvidenceResponse.builder()
                .publicId(e.getPublicId())
                .returnId(e.getReturnId())
                .returnItemId(e.getReturnItemId())
                .mediaReference(e.getMediaReference())
                .evidenceType(e.getEvidenceType())
                .description(e.getDescription())
                .uploadedBy(e.getUploadedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
