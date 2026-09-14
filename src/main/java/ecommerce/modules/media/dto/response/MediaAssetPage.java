package ecommerce.modules.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetPage {

    private List<MediaAssetResponse> content;
    private long                     totalElements;
    private int                      totalPages;
    private int                      currentPage;
    private boolean                  hasNextPage;

    public static MediaAssetPage from(Page<MediaAssetResponse> page) {
        return MediaAssetPage.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .hasNextPage(page.hasNext())
                .build();
    }
}
