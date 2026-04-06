package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.response.NotificationResponse;
import J2EE.SportBooingSystem.entity.Notification;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.NotificationType;
import J2EE.SportBooingSystem.repository.NotificationRepository;
import J2EE.SportBooingSystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void send(User recipient, NotificationType type,
                     String title, String message, String link) {
        // 1. Lưu vào DB
        Notification notif = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .build();
        notif = notifRepo.save(notif);

        // 2. Gửi realtime qua WebSocket tới đúng userId
        NotificationResponse payload = NotificationResponse.from(notif);
        String destination = "/topic/notifications/user/" + recipient.getId();
        messagingTemplate.convertAndSend(destination, payload);

        log.debug("Sent notification [{}] to user {} via {}", type, recipient.getId(), destination);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(Long userId) {
        return notifRepo.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(Long userId) {
        return notifRepo.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notifRepo.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long notifId, Long userId) {
        notifRepo.findById(notifId).ifPresent(n -> {
            if (n.getRecipient().getId().equals(userId)) {
                n.setRead(true);
                notifRepo.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notifRepo.markAllReadByUserId(userId);
    }
}
