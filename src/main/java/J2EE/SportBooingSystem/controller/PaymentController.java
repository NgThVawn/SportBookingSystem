package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final BookingRepository bookingRepo;

    // ── 1. Trang checkout ────────────────────────────────────────

    /**
     * GET /payment/checkout?bookingCode=BKxxxxxxxx
     * Hiển thị tóm tắt đơn hàng và nút "Thanh toán qua VNPay"
     */
    @GetMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
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

    // ── 3. Nhận kết quả từ VNPay (Return URL) ───────────────────

    /**
     * GET /payment/vnpay-return
     * VNPay redirect user về đây sau khi thanh toán
     */
    @GetMapping("/vnpay-return")
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
            bookingRepo.findByBookingCodeEager(bookingCode)
                    .ifPresent(b -> model.addAttribute("booking", BookingResponse.from(b)));
        }
        return "payment/result";
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
