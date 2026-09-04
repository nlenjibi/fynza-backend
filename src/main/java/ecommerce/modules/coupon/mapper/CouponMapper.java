package ecommerce.modules.coupon.mapper;

import ecommerce.modules.coupon.dto.CouponRequest;
import ecommerce.modules.coupon.dto.CouponResponse;
import ecommerce.modules.coupon.entity.Coupon;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface CouponMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usageCount", constant = "0")
    @Mapping(target = "status", expression = "java(ecommerce.common.enums.CouponStatus.ACTIVE)")
    Coupon toEntity(CouponRequest request);

    CouponResponse toResponse(Coupon coupon);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CouponRequest request, @MappingTarget Coupon coupon);
}
