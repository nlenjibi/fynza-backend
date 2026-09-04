package ecommerce.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    private PaginationUtils() {}

    public static Pageable of(int page, int size, int maxSize, Sort sort) {
        return PageRequest.of(Math.max(0, page), Math.min(size, maxSize), sort);
    }

    public static Pageable of(int page, int size, int maxSize) {
        return PageRequest.of(Math.max(0, page), Math.min(size, maxSize));
    }
}
