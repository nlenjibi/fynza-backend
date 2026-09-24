package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.ReviewVoteRequest;
import ecommerce.modules.review.dto.ReviewVoteResponse;

import java.util.UUID;

public interface ReviewVoteService {

    ReviewVoteResponse vote(UUID reviewId, ReviewVoteRequest request, UUID customerId);

    void removeVote(UUID reviewId, UUID customerId);
}
