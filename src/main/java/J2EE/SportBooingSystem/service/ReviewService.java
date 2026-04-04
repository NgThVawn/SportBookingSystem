package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.ReviewRequest;
import J2EE.SportBooingSystem.entity.Review;
import java.util.List;

public interface ReviewService {
    Review createReview(ReviewRequest request, String userEmail);
    List<Review> getReviewsByFacility(Long facilityId);
}
