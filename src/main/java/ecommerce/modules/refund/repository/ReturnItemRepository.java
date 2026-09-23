package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

    List<ReturnItem> findByReturnId(UUID returnId);
}
