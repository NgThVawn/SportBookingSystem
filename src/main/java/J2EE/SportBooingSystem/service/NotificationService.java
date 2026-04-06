package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.response.NotificationResponse;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    /**
     * Tạo và gửi thông báo realtime + lưu DB.
     * @param recipient  User nhận thông báo
     * @param type       Loại thông báo
     * @param title      Tiêu đề ngắn
     * @param message    Nội dung đầy đủ
     * @param link       URL điều hướng khi click (null nếu không có)
     */
    void send(User recipient, NotificationType type,
              String title, String message, String link);

    /** Lấy tất cả thông báo của user */
    List<NotificationResponse> getAll(Long userId);

    /** Lấy thông báo chưa đọc */
    List<NotificationResponse> getUnread(Long userId);

    /** Đếm thông báo chưa đọc */
    long countUnread(Long userId);

    /** Đánh dấu 1 thông báo đã đọc */
    void markRead(Long notifId, Long userId);

    /** Đánh dấu tất cả đã đọc */
    void markAllRead(Long userId);
}
