package ecommerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean empty;

    /**
     * Optional map of available filter values keyed by filter name
     * (e.g. {@code status}, {@code category}). Omitted when {@code null}.
     */
    @JsonProperty("filter_options")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, List<String>> filterOptions;

    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .empty(page.isEmpty())
                .build();
    }

    public static <T> PaginatedResponse<T> from(Page<T> page, Map<String, List<String>> filterOptions) {
        PaginatedResponse<T> response = from(page);
        response.setFilterOptions(filterOptions);
        return response;
    }
}
