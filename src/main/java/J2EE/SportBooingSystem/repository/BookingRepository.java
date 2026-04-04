package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    /** Eager load user + field + facility — dùng cho PaymentController và BookingResponse.from() */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.field f
        JOIN FETCH f.facility
        LEFT JOIN FETCH b.extraServices
        WHERE b.bookingCode = :code
    """)
    Optional<Booking> findByBookingCodeEager(@Param("code") String code);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.field f
        JOIN FETCH f.facility
        LEFT JOIN FETCH b.extraServices
        WHERE b.user = :user
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.field f
        JOIN FETCH f.facility
        WHERE b.field = :field
          AND b.bookingDate = :date
        ORDER BY b.startTime ASC
    """)
    List<Booking> findByFieldAndBookingDateOrderByStartTime(@Param("field") Field field,
                                                            @Param("date") LocalDate date);

    /** Kiểm tra xung đột thời gian: tìm booking đã tồn tại trùng khung giờ */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.field = :field
          AND b.bookingDate = :date
          AND b.status NOT IN ('CANCELLED')
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    boolean existsConflict(@Param("field") Field field,
                           @Param("date") LocalDate date,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime);

    /** Tương tự nhưng loại trừ booking hiện tại (dùng khi sửa booking) */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.field = :field
          AND b.bookingDate = :date
          AND b.status NOT IN ('CANCELLED')
          AND b.startTime < :endTime
          AND b.endTime > :startTime
          AND b.id <> :excludeId
    """)
    boolean existsConflictExcluding(@Param("field") Field field,
                                    @Param("date") LocalDate date,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime,
                                    @Param("excludeId") Long excludeId);

    /** Lấy danh sách booking của owner (qua field → facility) */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.field f
        JOIN FETCH f.facility fc
        LEFT JOIN FETCH b.extraServices
        WHERE fc.owner.email = :ownerEmail
        ORDER BY b.id DESC
    """)
    List<Booking> findByOwnerEmail(@Param("ownerEmail") String ownerEmail);

    @Query("""
        SELECT DISTINCT u FROM Booking b
        JOIN b.user u
        JOIN b.field f
        JOIN f.facility fc
        WHERE fc.owner.email = :ownerEmail
        ORDER BY u.createdAt DESC
    """)
    List<User> findDistinctCustomersByOwnerEmail(@Param("ownerEmail") String ownerEmail);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.field f
        JOIN FETCH f.facility fc
        WHERE fc.owner.email = :ownerEmail
          AND b.bookingDate = :date
        ORDER BY b.startTime ASC
    """)
    List<Booking> findByOwnerEmailAndDate(@Param("ownerEmail") String ownerEmail,
                                          @Param("date") LocalDate date);
}
