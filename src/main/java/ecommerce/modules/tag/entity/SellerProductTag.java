package ecommerce.modules.tag.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "seller_product_tags", indexes = {
    @Index(name = "idx_seller_product_tag_seller", columnList = "seller_id"),
    @Index(name = "idx_seller_product_tag_product", columnList = "product_id"),
    @Index(name = "idx_seller_product_tag_tag", columnList = "tag_id"),
    @Index(name = "idx_seller_product_tag_unique", columnList = "product_id,tag_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SellerProductTag extends BaseEntity {

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;
}
