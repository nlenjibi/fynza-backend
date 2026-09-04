package ecommerce.common.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Nested pagination metadata — mirrors the GraphQL {@code PageInfo} type.
 */
@Getter
@Builder
public class PageInfoResponse {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public static PageInfoResponse from(PaginatedResponse<?> source) {
        return PageInfoResponse.builder()
                .page(source.getPage())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .isFirst(source.isFirst())
                .isLast(source.isLast())
                .hasNext(source.isHasNext())
                .hasPrevious(source.isHasPrevious())
                .build();
    }
}
