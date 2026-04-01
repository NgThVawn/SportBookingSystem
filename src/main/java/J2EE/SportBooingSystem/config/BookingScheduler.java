package J2EE.SportBooingSystem.config;

import J2EE.SportBooingSystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BookingScheduler {

    private final BookingService bookingService;

    /** Chạy mỗi 5 phút: cập nhật booking đã qua giờ → COMPLETED */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void completeExpiredBookings() {
        bookingService.completeExpiredBookings();
    }

    /** Chạy mỗi 2 phút: hủy booking PENDING chưa thanh toán sau timeout */
    @Scheduled(fixedDelay = 2 * 60 * 1000)
    public void cancelExpiredPendingBookings() {
        bookingService.cancelExpiredPendingBookings();
    }
}