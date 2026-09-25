package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.CategoryStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryStatusHistoryRepository extends JpaRepository<CategoryStatusHistory, UUID> {

    List<CategoryStatusHistory> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId);
}
