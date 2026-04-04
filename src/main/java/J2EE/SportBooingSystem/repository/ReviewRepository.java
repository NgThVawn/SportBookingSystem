package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 1. Lấy danh sách đánh giá (ĐÃ FIX LAZY LOAD: Dùng JOIN FETCH để lấy luôn thông tin User)
    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.facility.id = :facilityId ORDER BY r.createdAt DESC")
    List<Review> findByFacilityIdOrderByCreatedAtDesc(@Param("facilityId") Long facilityId);

    // 2. Tính điểm trung bình của một sân trực tiếp từ Database
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.facility.id = :facilityId")
    Double calculateAverageRatingByFacility(@Param("facilityId") Long facilityId);

    // 3. Kiểm tra xem user đã từng đánh giá sân này chưa (để tránh spam)
    boolean existsByUserIdAndFacilityId(Long userId, Long facilityId);
}