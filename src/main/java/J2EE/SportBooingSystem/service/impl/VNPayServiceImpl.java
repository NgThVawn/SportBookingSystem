package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.Payment;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.enums.PaymentStatus;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.repository.PaymentRepository;
import J2EE.SportBooingSystem.service.VNPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.payment-url}")
    private String paymentUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;

    // ════════════════════════════════════════════════════════════
    // 1. Tạo URL thanh toán
    // ════════════════════════════════════════════════════════════

    @Override
    public String createPaymentUrl(Booking booking, String clientIp) {
        // Tạo Payment record nếu chưa có
        Payment payment = paymentRepo.findByBooking(booking).orElseGet(() ->
                paymentRepo.save(Payment.builder()
                        .booking(booking)
                        .txnRef(booking.getBookingCode())
                        .amount(booking.getTotalPrice())
                        .status(PaymentStatus.PENDING)
                        .orderInfo("Dat san " + booking.getBookingCode())
                        .build())
        );

        // Số tiền × 100 (VNPay dùng đơn vị tiền × 100)
        long amount = booking.getTotalPrice()
                .multiply(new BigDecimal(100))
                .longValue();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // Dùng TreeMap để tự động sort theo key (bắt buộc với VNPay)
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version",    "2.1.0");
        vnpParams.put("vnp_Command",    "pay");
        vnpParams.put("vnp_TmnCode",    tmnCode);
        vnpParams.put("vnp_Amount",     String.valueOf(amount));
        vnpParams.put("vnp_CurrCode",   "VND");
        vnpParams.put("vnp_TxnRef",     booking.getBookingCode());
        vnpParams.put("vnp_OrderInfo",  "Dat san " + booking.getBookingCode());
        vnpParams.put("vnp_OrderType",  "other");
        vnpParams.put("vnp_Locale",     "vn");
        vnpParams.put("vnp_ReturnUrl",  returnUrl);
        vnpParams.put("vnp_IpAddr",     clientIp != null ? clientIp : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", now.format(dtf));
        vnpParams.put("vnp_ExpireDate", now.plusMinutes(15).format(dtf));

        // Build hash data và query string
        StringBuilder hashData = new StringBuilder();
        StringBuilder query    = new StringBuilder();

        vnpParams.forEach((key, value) -> {
            String encodedValue = urlEncode(value);
            // Hash data: key=encoded_value (key không cần encode vì chỉ gồm chữ thường + _)
            if (hashData.length() > 0) hashData.append('&');
            hashData.append(key).append('=').append(encodedValue);
            // Query string: encoded_key=encoded_value
            if (query.length() > 0) query.append('&');
            query.append(urlEncode(key)).append('=').append(encodedValue);
        });

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        return paymentUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    // ════════════════════════════════════════════════════════════
    // 2. Xử lý phản hồi Return URL (user redirect về từ VNPay)
    // ════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public boolean processPaymentReturn(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (!verifyHash(params, receivedHash)) {
            log.warn("VNPay return: Invalid signature for txnRef={}",
                    params.get("vnp_TxnRef"));
            return false;
        }

        String txnRef      = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");  // 00 = thành công
        String transStatus = params.get("vnp_TransactionStatus");

        return updatePaymentStatus(txnRef, responseCode, transStatus, params);
    }

    // ════════════════════════════════════════════════════════════
    // 3. Xử lý IPN (server-to-server từ VNPay)
    // ════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public String processIpn(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (!verifyHash(params, receivedHash)) {
            log.warn("VNPay IPN: Invalid signature, txnRef={}", params.get("vnp_TxnRef"));
            return "97"; // Sai chữ ký
        }

        String txnRef      = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transStatus = params.get("vnp_TransactionStatus");

        try {
            updatePaymentStatus(txnRef, responseCode, transStatus, params);
            return "00"; // Thành công
        } catch (Exception e) {
            log.error("VNPay IPN processing error for txnRef={}: {}", txnRef, e.getMessage());
            return "01"; // Lỗi không xác định
        }
    }

    // ════════════════════════════════════════════════════════════
    // Helpers chung
    // ════════════════════════════════════════════════════════════

    /**
     * Cập nhật Payment và Booking dựa trên kết quả từ VNPay.
     * @return true nếu thanh toán thành công
     */
    private boolean updatePaymentStatus(String txnRef, String responseCode,
                                        String transStatus, Map<String, String> params) {
        // Tìm Payment theo txnRef (= bookingCode)
        Payment payment = paymentRepo.findByTxnRef(txnRef).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for txnRef={}", txnRef);
            return false;
        }

        // Tránh xử lý trùng lặp
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already SUCCESS for txnRef={}, skip", txnRef);
            return true;
        }

        boolean success = "00".equals(responseCode) && "00".equals(transStatus);

        // Cập nhật Payment
        payment.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        payment.setBankCode(params.get("vnp_BankCode"));
        payment.setCardType(params.get("vnp_CardType"));
        payment.setPayDate(params.get("vnp_PayDate"));
        payment.setResponseCode(responseCode);
        payment.setTransactionStatus(transStatus);
        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        paymentRepo.save(payment);

        // Cập nhật Booking
        Booking booking = payment.getBooking();
        if (success) {
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("Booking {} CONFIRMED via VNPay", booking.getBookingCode());
        } else {
            log.info("VNPay payment FAILED for booking {}, responseCode={}",
                    booking.getBookingCode(), responseCode);
            // Booking vẫn PENDING — user có thể thử lại
        }
        bookingRepo.save(booking);

        return success;
    }

    /** Xác thực chữ ký HMAC-SHA512 từ VNPay */
    private boolean verifyHash(Map<String, String> params, String receivedHash) {
        if (receivedHash == null) return false;

        // Lọc bỏ vnp_SecureHash và vnp_SecureHashType
        Map<String, String> filtered = new TreeMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        // Build hash data theo thứ tự đã sort
        StringBuilder hashData = new StringBuilder();
        filtered.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                if (hashData.length() > 0) hashData.append('&');
                hashData.append(key).append('=').append(urlEncode(value));
            }
        });

        String computedHash = hmacSHA512(hashSecret, hashData.toString());
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    /** Tính HMAC-SHA512 */
    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec spec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(spec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA512 error", e);
        }
    }

    /** URL encode theo chuẩn của VNPay (US_ASCII, space → + ) */
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
