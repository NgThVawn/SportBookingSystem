package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.ReviewRequest;
import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Review;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.exception.ForbiddenException;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.repository.FacilityRepository;
import J2EE.SportBooingSystem.repository.ReviewRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public Review createReview(ReviewRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // 1. Tìm đơn đặt sân
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Đơn đặt sân không tồn tại"));

        // 2. Các lớp bảo mật (Validate)
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền đánh giá đơn hàng này");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Đơn đặt sân chưa hoàn thành, không thể đánh giá");
        }
        if (booking.getIsReviewed()) {
            throw new IllegalStateException("Đơn đặt sân này đã được đánh giá rồi");
        }

        Facility facility = booking.getField().getFacility();

        // 3. Tạo Review
        Review review = Review.builder()
                .user(user)
                .facility(facility)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        reviewRepository.save(review);

        // 4. Đánh dấu đơn hàng là "Đã đánh giá"
        booking.setIsReviewed(true);
        bookingRepository.save(booking);

        // 5. Cập nhật điểm trung bình cho Cơ sở
        Double newAvgRating = reviewRepository.calculateAverageRatingByFacility(facility.getId());
        newAvgRating = Math.round(newAvgRating * 10.0) / 10.0;
        facility.setAvgRating(BigDecimal.valueOf(newAvgRating));
        facilityRepository.save(facility);

        return review;
    }

    @Override
    public List<Review> getReviewsByFacility(Long facilityId) {
        return reviewRepository.findByFacilityIdOrderByCreatedAtDesc(facilityId);
    }
}