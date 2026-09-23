package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewRatingAggregate;
import ecommerce.modules.review.enums.ReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRatingAggregateRepository extends JpaRepository<ReviewRatingAggregate, Long> {

    Optional<ReviewRatingAggregate> findByTargetTypeAndTargetId(ReviewTargetType targetType, UUID targetId);
}
