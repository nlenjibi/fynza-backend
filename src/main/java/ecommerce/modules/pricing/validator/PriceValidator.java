package ecommerce.modules.pricing.validator;

import ecommerce.modules.pricing.dto.request.CreatePriceRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.exception.InvalidPriceException;
import ecommerce.modules.pricing.exception.PriceOwnershipException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PriceValidator {

    public void validateCreate(CreatePriceRequest request) {
        if (request.getSaleAmount() != null && request.getAmount() != null
                && request.getSaleAmount().compareTo(request.getAmount()) > 0) {
            throw new InvalidPriceException("saleAmount must not exceed amount");
        }
        if (request.getValidFrom() != null && request.getValidUntil() != null
                && !request.getValidUntil().isAfter(request.getValidFrom())) {
            throw new InvalidPriceException("validUntil must be after validFrom");
        }
    }

    public void validateUpdate(UpdatePriceRequest request, Price existing) {
        BigDecimal newAmount     = request.getAmount()     != null ? request.getAmount()     : existing.getAmount();
        BigDecimal newSaleAmount = request.getSaleAmount() != null ? request.getSaleAmount() : existing.getSaleAmount();

        if (newSaleAmount != null && newSaleAmount.compareTo(newAmount) > 0) {
            throw new InvalidPriceException("saleAmount must not exceed amount");
        }
        if (request.getValidFrom() != null && request.getValidUntil() != null
                && !request.getValidUntil().isAfter(request.getValidFrom())) {
            throw new InvalidPriceException("validUntil must be after validFrom");
        }
    }

    public void assertOwner(Price price, UUID actorId) {
        if (!price.getCreatedBy().equals(actorId)) {
            throw new PriceOwnershipException(price.getPublicId());
        }
    }
}
