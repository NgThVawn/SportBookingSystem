package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.BookingRequest;
import J2EE.SportBooingSystem.dto.request.BookingExtraItemRequest;
import J2EE.SportBooingSystem.dto.response.AvailabilityResponse;
import J2EE.SportBooingSystem.dto.response.AvailabilityResponse.OccupiedSlot;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.entity.*;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.exception.ForbiddenException;
import J2EE.SportBooingSystem.exception.ResourceNotFoundException;
import J2EE.SportBooingSystem.repository.*;
import J2EE.SportBooingSystem.service.BookingService;
import J2EE.SportBooingSystem.service.ExtraServiceService;
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
    private final ExtraServiceService extraServiceService;
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
        BigDecimal fieldPrice = priceRuleService.getTotalPrice(
                field.getId(), req.getBookingDate(), req.getStartTime(), req.getEndTime());
        List<BookingExtraService> selectedExtras = buildBookingExtraItems(req, field.getFacility().getId());
        BigDecimal extraPrice = selectedExtras.stream()
            .map(BookingExtraService::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice = fieldPrice.add(extraPrice);

        Booking booking = Booking.builder()
                .bookingCode(generateCode())
                .user(user)
                .field(field)
                .bookingDate(req.getBookingDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
            .totalPrice(totalPrice)
                .status(BookingStatus.PENDING)
                .note(req.getNote())
                .build();

        selectedExtras.forEach(item -> item.setBooking(booking));
        booking.getExtraServices().addAll(selectedExtras);

        return bookingRepo.save(booking);
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


        bookingRepo.findByFieldAndBookingDateOrderByStartTime(field, date)
                .stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .forEach(b -> occupied.add(new OccupiedSlot(b.getStartTime(), b.getEndTime(), "BOOKING")));


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

    private List<BookingExtraService> buildBookingExtraItems(BookingRequest req, Long facilityId) {
        if (req.getExtraItems() == null || req.getExtraItems().isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> quantityByService = new LinkedHashMap<>();
        for (BookingExtraItemRequest item : req.getExtraItems()) {
            if (item == null || item.getServiceId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            quantityByService.merge(item.getServiceId(), item.getQuantity(), Integer::sum);
        }

        if (quantityByService.isEmpty()) {
            return List.of();
        }

        List<ExtraService> services = extraServiceService.findAllByIds(new ArrayList<>(quantityByService.keySet()));
        Map<Long, ExtraService> serviceMap = services.stream()
                .collect(Collectors.toMap(ExtraService::getId, svc -> svc));

        List<BookingExtraService> results = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantityByService.entrySet()) {
            ExtraService service = serviceMap.get(entry.getKey());
            if (service == null) {
                throw new IllegalArgumentException("Dịch vụ đi kèm không tồn tại");
            }
            if (!service.getFacility().getId().equals(facilityId)) {
                throw new IllegalArgumentException("Dịch vụ đi kèm không thuộc cơ sở của sân đã chọn");
            }
            if (!Boolean.TRUE.equals(service.getIsActive())) {
                throw new IllegalArgumentException("Dịch vụ '" + service.getName() + "' hiện không hoạt động");
            }

            int quantity = entry.getValue();
            if (service.getStock() != null && quantity > service.getStock()) {
                throw new IllegalArgumentException("Dịch vụ '" + service.getName() + "' chỉ còn " + service.getStock() + " sản phẩm");
            }

            BigDecimal subtotal = service.getPrice().multiply(BigDecimal.valueOf(quantity));
            results.add(BookingExtraService.builder()
                    .extraService(service)
                    .serviceName(service.getName())
                    .unit(service.getUnit())
                    .quantity(quantity)
                    .unitPrice(service.getPrice())
                    .subtotal(subtotal)
                    .build());
        }
        return results;
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
    @Override
    @Transactional
    public void confirmBooking(Long bookingId, String ownerEmail) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân"));

        if (!booking.getField().getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new J2EE.SportBooingSystem.exception.ForbiddenException("Bạn không có quyền thao tác trên đơn này!");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể duyệt đơn đang ở trạng thái CHỜ XÁC NHẬN (PENDING).");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking);
    }

    @Override
    @Transactional
    public J2EE.SportBooingSystem.enums.BookingStatus cancelBooking(Long bookingId, String userEmail, String reason) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân"));

        boolean isOwner = booking.getField().getFacility().getOwner().getEmail().equals(userEmail);
        boolean isCustomer = booking.getUser().getEmail().equals(userEmail);

        if (!isOwner && !isCustomer) {
            throw new ForbiddenException("Bạn không có quyền thực hiện hành động này");
        }

        String actualReason = (reason != null && !reason.trim().isEmpty()) ? reason : "Không có lý do";

        if (isOwner) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelReason("Chủ sân hủy: " + actualReason);
            bookingRepo.save(booking);
            return booking.getStatus(); 
        }

        if (isCustomer) {
            LocalDateTime startDateTime = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(startDateTime.minusHours(24))) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelReason("Khách tự hủy (trước 24h): " + actualReason);
            } else {
                booking.setStatus(BookingStatus.CANCEL_PENDING);
                booking.setCancelReason("Yêu cầu hủy sát giờ: " + actualReason);
            }
            bookingRepo.save(booking);
        }
        
        return booking.getStatus(); 
    }

    @Override
    @Transactional
    public void approveCancelRequest(Long bookingId, String ownerEmail, boolean approve) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        
        // Kiểm tra quyền chủ sân
        if (!booking.getField().getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new ForbiddenException("Không có quyền");
        }

        if (approve) {
            booking.setStatus(BookingStatus.CANCELLED);
            // Có thể thêm logic hoàn tiền ở đây nếu đã thanh toán VNPay
        } else {
            // Nếu từ chối cho hủy, đơn quay lại trạng thái CONFIRMED (bắt buộc đi đá hoặc mất tiền)
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        bookingRepo.save(booking);
    }

}
