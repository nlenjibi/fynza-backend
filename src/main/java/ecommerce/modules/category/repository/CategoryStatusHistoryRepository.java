package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.CategoryStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryStatusHistoryRepository extends JpaRepository<CategoryStatusHistory, Long> {

    List<CategoryStatusHistory> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);
}
