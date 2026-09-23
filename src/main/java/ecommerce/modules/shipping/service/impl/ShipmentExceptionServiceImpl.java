package ecommerce.modules.shipping.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.shipping.dto.request.ResolveExceptionRequest;
import ecommerce.modules.shipping.dto.request.ShipmentExceptionRequest;
import ecommerce.modules.shipping.dto.response.ShipmentExceptionResponse;
import ecommerce.modules.shipping.entity.ShipmentException;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionStatus;
import ecommerce.modules.shipping.enums.ExceptionType;
import ecommerce.modules.shipping.exception.ShipmentNotFoundException;
import ecommerce.modules.shipping.repository.ShipmentExceptionRepository;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.service.ShipmentExceptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShipmentExceptionServiceImpl implements ShipmentExceptionService {

    private final ShipmentExceptionRepository exceptionRepository;
    private final ShipmentRepository shipmentRepository;

    @Override
    @Transactional
    public ShipmentExceptionResponse createException(UUID shipmentPublicId, ShipmentExceptionRequest request) {
        return createException(shipmentPublicId, request.getType(), request.getSeverity(), request.getDescription());
    }

    @Override
    @Transactional
    public ShipmentExceptionResponse createException(UUID shipmentPublicId, ExceptionType type, ExceptionSeverity severity, String description) {
        shipmentRepository.findByPublicId(shipmentPublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentPublicId));

        ShipmentException exception = ShipmentException.builder()
                .shipmentId(shipmentPublicId)
                .type(type)
                .severity(severity)
                .description(description)
                .status(ExceptionStatus.OPEN)
                .detectedAt(Instant.now())
                .build();

        exception = exceptionRepository.save(exception);
        log.info("Created {} exception (severity={}) for shipment={}", type, severity, shipmentPublicId);
        return ShipmentExceptionResponse.from(exception);
    }

    @Override
    @Transactional
    public ShipmentExceptionResponse resolveException(UUID exceptionPublicId, ResolveExceptionRequest request) {
        ShipmentException exception = exceptionRepository.findByPublicId(exceptionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment exception not found: " + exceptionPublicId));

        exception.setStatus(ExceptionStatus.RESOLVED);
        exception.setResolvedAt(Instant.now());
        exception.setResolvedBy(request.getResolvedBy());
        exception.setResolutionNotes(request.getResolutionNotes());

        exception = exceptionRepository.save(exception);
        log.info("Resolved exception={} by={}", exceptionPublicId, sanitize(request.getResolvedBy()));
        return ShipmentExceptionResponse.from(exception);
    }

    @Override
    public List<ShipmentExceptionResponse> getExceptionsForShipment(UUID shipmentPublicId) {
        shipmentRepository.findByPublicId(shipmentPublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentPublicId));
        return exceptionRepository.findByShipmentIdOrderByCreatedAtDesc(shipmentPublicId)
                .stream().map(ShipmentExceptionResponse::from).toList();
    }

    @Override
    public List<ShipmentExceptionResponse> getOpenExceptions() {
        return exceptionRepository.findByStatusOrderByCreatedAtDesc(ExceptionStatus.OPEN)
                .stream().map(ShipmentExceptionResponse::from).toList();
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\r', '_').replace('\n', '_');
    }
}
