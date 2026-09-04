package ecommerce.modules.delivery.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "delivery_regions", indexes = {
    @Index(name = "idx_region_code", columnList = "code", unique = true),
    @Index(name = "idx_region_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeliveryRegion extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "Ghana";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
