package ecommerce.modules.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_customer_stats")
@Getter
@NoArgsConstructor
public class CustomerStatsView {

    @Id
    @Column(name = "singleton")
    private Integer singleton;

    @Column(name = "total_customers")
    private Long totalCustomers;

    @Column(name = "active_customers")
    private Long activeCustomers;

    @Column(name = "new_customers_this_month")
    private Long newCustomersThisMonth;
}
