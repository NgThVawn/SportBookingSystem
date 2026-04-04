package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.service.MoMoService;
import J2EE.SportBooingSystem.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import J2EE.SportBooingSystem.service.NotificationService;
import J2EE.SportBooingSystem.enums.NotificationType;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    @Value("${booking.payment-timeout-minutes:10}")
    private int paymentTimeoutMinutes;

    private final VNPayService vnPayService;
    private final MoMoService moMoService;
    private final BookingRepository bookingRepo;
    private final NotificationService notificationService;

    // ── 1. Trang checkout ────────────────────────────────────────

    /**
     * GET /payment/checkout?bookingCode=BKxxxxxxxx
     * Hiển thị tóm tắt đơn hàng và nút "Thanh toán qua VNPay"
     */
    @GetMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public String checkoutPage(@RequestParam String bookingCode,
                               @AuthenticationPrincipal UserDetails ud,
                               Model model) {
        Booking booking = bookingRepo.findByBookingCodeEager(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));

        // Chỉ user đặt sân mới xem được
        if (!booking.getUser().getEmail().equals(ud.getUsername())) {
            return "redirect:/bookings?error=forbidden";
        }
        // Nếu đã thanh toán → chuyển về lịch sử
        if (booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            return "redirect:/bookings?info=already-paid";
        }
        // Nếu bị hủy → thông báo
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return "redirect:/bookings?error=cancelled";
        }

        model.addAttribute("booking", BookingResponse.from(booking));
        model.addAttribute("paymentTimeoutMinutes", paymentTimeoutMinutes);
        return "payment/checkout";
    }

    // ── 2. Redirect tới VNPay ────────────────────────────────────

    /**
     * POST /payment/vnpay?bookingCode=BKxxxxxxxx
     * Tạo URL thanh toán và redirect user tới VNPay
     */
    @PostMapping("/vnpay")
    @PreAuthorize("isAuthenticated()")
    public String redirectToVNPay(@RequestParam String bookingCode,
                                  @AuthenticationPrincipal UserDetails ud,
                                  HttpServletRequest request) {
        Booking booking = bookingRepo.findByBookingCodeEager(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));

        if (!booking.getUser().getEmail().equals(ud.getUsername())) {
            return "redirect:/bookings?error=forbidden";
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return "redirect:/bookings";
        }

        String clientIp = getClientIp(request);
        String paymentUrl = vnPayService.createPaymentUrl(booking, clientIp);
        log.info("Redirecting user {} to VNPay for booking {}",
                ud.getUsername(), bookingCode);
        return "redirect:" + paymentUrl;
    }

    @PostMapping("/momo")
    @PreAuthorize("isAuthenticated()")
// 1. ĐÃ XÓA @Transactional ở đây
    public String createMomoPayment(@RequestParam String bookingCode,
                                    @AuthenticationPrincipal UserDetails ud, // 2. Đồng bộ cách lấy User
                                    RedirectAttributes ra) {

        Booking booking = bookingRepo.findByBookingCodeEager(bookingCode)
                .orElseThrow(() -> new IllegalArgumentException("Booking không tồn tại"));
        if (!booking.getUser().getEmail().equals(ud.getUsername())) {
            return "redirect:/bookings?error=forbidden";
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return "redirect:/payment/checkout?bookingCode=" + bookingCode;
        }
        try {
            String payUrl = moMoService.createPaymentUrl(booking);
            log.info("Redirecting user {} to MoMo for booking {}", ud.getUsername(), bookingCode);
            return "redirect:" + payUrl;
        } catch (Exception e) {
            log.error("Lỗi tạo thanh toán MoMo cho booking {}: {}", bookingCode, e.getMessage());
            ra.addFlashAttribute("error", "Hệ thống MoMo đang bận hoặc lỗi mạng. Vui lòng thử lại sau!");
            return "redirect:/payment/checkout?bookingCode=" + bookingCode;
        }
    }

    // ── 3. Nhận kết quả từ VNPay (Return URL) ───────────────────

    /**
     * GET /payment/vnpay-return
     * VNPay redirect user về đây sau khi thanh toán
     */
    @GetMapping("/vnpay-return")
    @Transactional(readOnly = true)
    public String vnpayReturn(@RequestParam Map<String, String> params,
                              Model model) {
        log.info("VNPay return received: vnp_TxnRef={}, vnp_ResponseCode={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        boolean success = vnPayService.processPaymentReturn(params);
        String bookingCode = params.get("vnp_TxnRef");

        // Parse số tiền: VNPay gửi amount * 100, chia lại để hiển thị
        String rawAmount = params.get("vnp_Amount");
        Long amountVnd = null;
        if (rawAmount != null && !rawAmount.isBlank()) {
            try { amountVnd = Long.parseLong(rawAmount) / 100; } catch (NumberFormatException ignored) {}
        }

        model.addAttribute("success", success);
        model.addAttribute("bookingCode", bookingCode);
        model.addAttribute("responseCode", params.get("vnp_ResponseCode"));
        model.addAttribute("bankCode", params.get("vnp_BankCode"));
        model.addAttribute("amountVnd", amountVnd);
        model.addAttribute("transactionNo", params.get("vnp_TransactionNo"));
        model.addAttribute("payDate", params.get("vnp_PayDate"));

        if (bookingCode != null) {
            bookingRepo.findByBookingCodeEager(bookingCode).ifPresent(b -> {
                model.addAttribute("booking", BookingResponse.from(b));
                // Gửi thông báo sau khi xử lý thanh toán
                if (success) {
                    notificationService.send(
                            b.getUser(),
                            NotificationType.PAYMENT_SUCCESS,
                            "Thanh toán thành công",
                            "Đã thanh toán booking " + b.getBookingCode()
                                    + " - " + b.getField().getName()
                                    + " ngày " + b.getBookingDate() + " qua VNPay.",
                            "/bookings"
                    );
                    notificationService.send(
                            b.getField().getFacility().getOwner(),
                            NotificationType.PAYMENT_RECEIVED,
                            "Nhận được thanh toán",
                            "Booking " + b.getBookingCode() + " của " + b.getUser().getFullName()
                                    + " đã được thanh toán qua VNPay.",
                            "/owner/bookings"
                    );
                } else {
                    notificationService.send(
                            b.getUser(),
                            NotificationType.PAYMENT_FAILED,
                            "Thanh toán thất bại",
                            "Thanh toán booking " + b.getBookingCode() + " qua VNPay không thành công. "
                                    + "Mã lỗi: " + params.getOrDefault("vnp_ResponseCode", "?"),
                            "/payment/checkout?bookingCode=" + b.getBookingCode()
                    );
                }
            });
        }
        return "payment/result";
    }

    @GetMapping("/momo-return")
    @Transactional(readOnly = true)
    public String momoReturn(@RequestParam Map<String, String> params, Model model) {
        boolean success = moMoService.processPaymentReturn(params);

        // orderId có dạng "BK202604038364-1743685667415" — cắt suffix timestamp để lấy bookingCode
        String orderId     = params.getOrDefault("orderId", "");
        String bookingCode = orderId.contains("-") ? orderId.substring(0, orderId.lastIndexOf('-')) : orderId;
        int    resultCode  = Integer.parseInt(params.getOrDefault("resultCode", "-1"));

        model.addAttribute("success",     success);
        model.addAttribute("bookingCode", bookingCode);
        model.addAttribute("resultCode",  resultCode);
        model.addAttribute("message",     params.getOrDefault("message", ""));
        model.addAttribute("transId",     params.getOrDefault("transId", ""));
        model.addAttribute("amount",      params.getOrDefault("amount", "0"));
        model.addAttribute("payType",     params.getOrDefault("payType", ""));

        // Lấy thêm thông tin booking để hiển thị
        if (!bookingCode.isBlank()) {
            bookingRepo.findByBookingCodeEager(bookingCode).ifPresent(b -> {
                model.addAttribute("booking", BookingResponse.from(b));
                if (success) {
                    notificationService.send(
                            b.getUser(),
                            NotificationType.PAYMENT_SUCCESS,
                            "Thanh toán thành công",
                            "Đã thanh toán booking " + b.getBookingCode()
                                    + " - " + b.getField().getName()
                                    + " ngày " + b.getBookingDate() + " qua MoMo.",
                            "/bookings"
                    );
                    notificationService.send(
                            b.getField().getFacility().getOwner(),
                            NotificationType.PAYMENT_RECEIVED,
                            "Nhận được thanh toán",
                            "Booking " + b.getBookingCode() + " của " + b.getUser().getFullName()
                                    + " đã được thanh toán qua MoMo.",
                            "/owner/bookings"
                    );
                } else {
                    notificationService.send(
                            b.getUser(),
                            NotificationType.PAYMENT_FAILED,
                            "Thanh toán thất bại",
                            "Thanh toán booking " + b.getBookingCode() + " qua MoMo không thành công. "
                                    + "Mã lỗi: " + resultCode,
                            "/payment/checkout?bookingCode=" + b.getBookingCode()
                    );
                }
            });
        }

        return "payment/momo-result";
    }

    // ── 4. IPN – Server-to-Server từ VNPay ─────────────────────

    /**
     * POST /payment/vnpay-ipn
     * VNPay gọi endpoint này (không cần auth) để thông báo kết quả giao dịch.
     * Chỉ cần thiết khi dùng ngrok hoặc server có IP public.
     */
    @PostMapping("/vnpay-ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params) {
        log.info("VNPay IPN received: txnRef={}", params.get("vnp_TxnRef"));
        String rspCode = vnPayService.processIpn(params);
        Map<String, String> result = new HashMap<>();
        result.put("RspCode", rspCode);
        result.put("Message", "00".equals(rspCode) ? "Confirm Success" : "Confirm Fail");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/momo-ipn")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> momoIpn(
            @RequestBody Map<String, Object> body) {
        log.info("MoMo IPN nhận được: {}", body);
        String result = moMoService.processIpn(body);
        Map<String, Object> response = Map.of(
                "partnerCode", body.getOrDefault("partnerCode", ""),
                "requestId",   body.getOrDefault("requestId", ""),
                "orderId",     body.getOrDefault("orderId", ""),
                "resultCode",  result.equals("0") ? 0 : Integer.parseInt(result),
                "message",     result.equals("0") ? "Thành công" : "Lỗi"
        );
        return ResponseEntity.ok(response);
    }

    // ── Helper ───────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        // Lấy IP đầu tiên nếu có nhiều (X-Forwarded-For có thể chứa nhiều IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
