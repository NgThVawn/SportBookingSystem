package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.BookingRequest;
import J2EE.SportBooingSystem.dto.response.AvailabilityResponse;
import J2EE.SportBooingSystem.dto.response.AvailabilityResponse.OccupiedSlot;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.entity.*;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.exception.ForbiddenException;
import J2EE.SportBooingSystem.repository.*;
import J2EE.SportBooingSystem.service.BookingService;
import J2EE.SportBooingSystem.service.PriceRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepo;
    private final BlockedTimeRepository blockedRepo;
    private final FieldRepository fieldRepo;
    private final UserRepository userRepo;
    private final PriceRuleService priceRuleService;
    @Value("${booking.payment-timeout-minutes:5}")
    private int paymentTimeoutMinutes;

    @Override
    @Transactional
    public Booking createBooking(BookingRequest req, String userEmail) {
        Field field = fieldRepo.findById(req.getFieldId()).orElseThrow(
                () -> new IllegalArgumentException("Sân không tồn tại"));

        validateTimeRange(field, req);

        if (bookingRepo.existsConflict(field, req.getBookingDate(),
                req.getStartTime(), req.getEndTime())) {
            throw new IllegalStateException("Khung giờ này đã được đặt, vui lòng chọn giờ khác");
        }
        if (blockedRepo.existsConflict(field, req.getBookingDate(),
                req.getStartTime(), req.getEndTime())) {
            throw new IllegalStateException("Khung giờ này không khả dụng (bảo trì/chặn)");
        }

        User user = userRepo.findByEmail(userEmail).orElseThrow();
        BigDecimal price = priceRuleService.getTotalPrice(
                field.getId(), req.getBookingDate(), req.getStartTime(), req.getEndTime());

        Booking booking = Booking.builder()
                .bookingCode(generateCode())
                .user(user)
                .field(field)
                .bookingDate(req.getBookingDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .totalPrice(price)
                .status(BookingStatus.PENDING)
                .note(req.getNote())
                .build();

        return bookingRepo.save(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, String userEmail, String reason) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        boolean isUser = booking.getUser().getEmail().equals(userEmail);
        boolean isOwner = booking.getField().getFacility().getOwner().getEmail().equals(userEmail);
        if (!isUser && !isOwner)
            throw new ForbiddenException("Bạn không có quyền hủy booking này");
        if (booking.getStatus() == BookingStatus.CANCELLED)
            throw new IllegalStateException("Booking đã được hủy trước đó");
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelReason(reason);
        bookingRepo.save(booking);
    }

    @Override
    public List<BookingResponse> getBookingsByUser(String userEmail) {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        return bookingRepo.findByUserOrderByCreatedAtDesc(user)
                .stream().map(BookingResponse::from).collect(Collectors.toList());
    }

    @Override
    public List<BookingResponse> getBookingsByOwner(String ownerEmail) {
        return bookingRepo.findByOwnerEmail(ownerEmail)
                .stream().map(BookingResponse::from).collect(Collectors.toList());
    }

    @Override
    public BookingResponse getBookingByCode(String code, String requestorEmail) {
        Booking b = bookingRepo.findByBookingCode(code).orElseThrow();
        boolean isUser  = b.getUser().getEmail().equals(requestorEmail);
        boolean isOwner = b.getField().getFacility().getOwner().getEmail().equals(requestorEmail);
        if (!isUser && !isOwner)
            throw new ForbiddenException("Bạn không có quyền xem booking này");
        return BookingResponse.from(b);
    }
    @Transactional(readOnly = true)
    @Override
    public AvailabilityResponse getAvailability(Long fieldId, LocalDate date) {
        Field field = fieldRepo.findById(fieldId).orElseThrow();
        Facility facility = field.getFacility();

        List<OccupiedSlot> occupied = new ArrayList<>();

        // Booking đã xác nhận
        bookingRepo.findByFieldAndBookingDateOrderByStartTime(field, date)
                .stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .forEach(b -> occupied.add(new OccupiedSlot(b.getStartTime(), b.getEndTime(), "BOOKING")));

        // Khung giờ bị chặn
        blockedRepo.findByFieldAndDateOrderByStartTime(field, date)
                .forEach(bt -> occupied.add(new OccupiedSlot(bt.getStartTime(), bt.getEndTime(), "BLOCKED")));

        return AvailabilityResponse.builder()
                .openTime(facility.getOpenTime())
                .closeTime(facility.getCloseTime())
                .occupied(occupied)
                .build();
    }

    @Override
    @Transactional
    public void completeExpiredBookings() {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getBookingDate().isBefore(today)
                        || (b.getBookingDate().isEqual(today) && b.getEndTime().isBefore(now)))
                .forEach(b -> {
                    b.setStatus(BookingStatus.COMPLETED);
                    bookingRepo.save(b);
                });
    }

    // ── Helpers ──────────────────────────────────────────────

    private void validateTimeRange(Field field, BookingRequest req) {
        Facility fac = field.getFacility();
        if (req.getStartTime().isBefore(fac.getOpenTime()))
            throw new IllegalArgumentException("Giờ bắt đầu trước giờ mở cửa (" + fac.getOpenTime() + ")");
        if (req.getEndTime().isAfter(fac.getCloseTime()))
            throw new IllegalArgumentException("Giờ kết thúc sau giờ đóng cửa (" + fac.getCloseTime() + ")");
        if (!req.getStartTime().isBefore(req.getEndTime()))
            throw new IllegalArgumentException("Giờ bắt đầu phải nhỏ hơn giờ kết thúc");
        long minutes = Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
        if (minutes < 30)
            throw new IllegalArgumentException("Thời gian đặt sân tối thiểu là 30 phút");
    }

    private String generateCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%04d", new Random().nextInt(10000));
        return "BK" + date + rand;
    }

    @Override
    @Transactional
    public void cancelExpiredPendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        bookingRepo.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getCreatedAt().isBefore(cutoff))
                .forEach(b -> {
                    b.setStatus(BookingStatus.CANCELLED);
                    b.setCancelReason("Tự động hủy do chưa thanh toán sau " + paymentTimeoutMinutes + " phút");
                    bookingRepo.save(b);
                    log.info("Auto-cancelled PENDING booking: {}", b.getBookingCode());
                });
    }
}
