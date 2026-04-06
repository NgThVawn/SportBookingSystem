package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Lấy tất cả thông báo của user, mới nhất trước */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /** Lấy thông báo chưa đọc */
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

    /** Đếm chưa đọc */
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /** Đánh dấu tất cả đã đọc cho 1 user */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
    void markAllReadByUserId(Long userId);
}
