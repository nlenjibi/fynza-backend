package ecommerce.modules.review.mapper;

import ecommerce.modules.review.dto.ReviewCreateRequest;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewUpdateRequest;
import ecommerce.modules.review.entity.Review;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReviewMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSlug", source = "product.slug")
    @Mapping(target = "user.id", source = "customer.id")
    @Mapping(target = "user.firstName", source = "customer.firstName")
    @Mapping(target = "user.lastName", source = "customer.lastName")
    @Mapping(target = "user.email", source = "customer.email")
    @Mapping(target = "helpfulCount", source = "helpful")
    @Mapping(target = "notHelpfulCount", source = "unhelpful")
    @Mapping(target = "pros", source = "pros", qualifiedByName = "stringToList")
    @Mapping(target = "cons", source = "cons", qualifiedByName = "stringToList")
    ReviewResponse toDto(Review review);

    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(value.split(","));
    }

    @Named("listToString")
    default String listToString(List<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return String.join(",", value);
    }

    List<ReviewResponse> toDtoList(List<Review> reviews);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "verifiedPurchase", constant = "false")
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "helpful", constant = "0")
    @Mapping(target = "unhelpful", constant = "0")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "adminResponse", ignore = true)
    @Mapping(target = "adminResponseAt", ignore = true)
    @Mapping(target = "adminResponseBy", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "hasImages", ignore = true)
    @Mapping(target = "pros", source = "pros", qualifiedByName = "listToString")
    @Mapping(target = "cons", source = "cons", qualifiedByName = "listToString")
    Review toEntity(ReviewCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "pros", ignore = true)
    @Mapping(target = "cons", ignore = true)
    void updateFromDto(ReviewUpdateRequest request, @MappingTarget Review review);
}
