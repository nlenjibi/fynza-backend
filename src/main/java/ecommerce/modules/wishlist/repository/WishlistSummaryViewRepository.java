package ecommerce.modules.wishlist.repository;

import ecommerce.modules.wishlist.entity.WishlistSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WishlistSummaryViewRepository extends JpaRepository<WishlistSummaryView, Long> {

    List<WishlistSummaryView> findByCustomerIdAndStatusNot(UUID customerId, String status);
}
