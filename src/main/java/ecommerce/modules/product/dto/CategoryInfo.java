package ecommerce.modules.product.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryInfo {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
}
