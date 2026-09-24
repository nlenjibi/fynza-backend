package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewVote;
import ecommerce.modules.review.enums.ReviewVoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, UUID> {

    Optional<ReviewVote> findByReview_IdAndCustomerId(UUID reviewId, UUID customerId);

    long countByReview_IdAndVoteType(UUID reviewId, ReviewVoteType voteType);

    void deleteByReview_IdAndCustomerId(UUID reviewId, UUID customerId);
}
