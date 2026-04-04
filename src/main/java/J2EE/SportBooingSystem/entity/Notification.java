package J2EE.SportBooingSystem.entity;

import J2EE.SportBooingSystem.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người nhận thông báo */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /** Tiêu đề ngắn (hiển thị in đậm trên toast) */
    @Column(nullable = false, length = 200)
    private String title;

    /** Nội dung chi tiết */
    @Column(nullable = false, length = 500)
    private String message;

    /** URL để điều hướng khi click vào thông báo (có thể null) */
    @Column(length = 300)
    private String link;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
