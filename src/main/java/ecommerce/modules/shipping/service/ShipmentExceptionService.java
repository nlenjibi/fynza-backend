package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.ResolveExceptionRequest;
import ecommerce.modules.shipping.dto.request.ShipmentExceptionRequest;
import ecommerce.modules.shipping.dto.response.ShipmentExceptionResponse;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionType;

import java.util.List;
import java.util.UUID;

public interface ShipmentExceptionService {
    ShipmentExceptionResponse createException(UUID shipmentPublicId, ShipmentExceptionRequest request);
    ShipmentExceptionResponse createException(UUID shipmentPublicId, ExceptionType type, ExceptionSeverity severity, String description);
    ShipmentExceptionResponse resolveException(UUID exceptionPublicId, ResolveExceptionRequest request);
    List<ShipmentExceptionResponse> getExceptionsForShipment(UUID shipmentPublicId);
    List<ShipmentExceptionResponse> getOpenExceptions();
}
