package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.config.MoMoConfig;
import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.Payment;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.enums.PaymentMethod;
import J2EE.SportBooingSystem.enums.PaymentStatus;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.repository.PaymentRepository;
import J2EE.SportBooingSystem.service.MoMoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoMoServiceImpl implements MoMoService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.payment-url}")
    private String paymentUrl;

    @Value("${momo.return-url}")
    private String returnUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final RestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // TẠO YÊU CẦU THANH TOÁN
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String createPaymentUrl(Booking booking) {
        // orderId phải unique mỗi lần gửi sang MoMo — dùng timestamp tránh lỗi "trùng orderId"
        String suffix     = String.valueOf(System.currentTimeMillis());
        String orderId    = booking.getBookingCode() + "-" + suffix;
        String requestId  = orderId;
        long   amount     = booking.getTotalPrice().longValue();
        String orderInfo  = "Dat san " + booking.getBookingCode();
        String requestType = "captureWallet";
        String extraData  = "";

        // 1. Tạo hoặc cập nhật Payment record; txnRef luôn khớp với orderId gửi sang MoMo
        Payment payment = paymentRepo.findByBooking(booking).orElseGet(() ->
                Payment.builder()
                        .booking(booking)
                        .amount(booking.getTotalPrice())
                        .status(PaymentStatus.PENDING)
                        .orderInfo(orderInfo)
                        .paymentMethod(PaymentMethod.MOMO)
                        .build()
        );
        payment.setTxnRef(orderId);   // cập nhật mỗi lần thử lại để IPN tìm được
        payment.setPaymentMethod(PaymentMethod.MOMO);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepo.save(payment);

        // 2. Tạo chuỗi ký theo đúng thứ tự bảng chữ cái (chuẩn MoMo)
        String rawSignature = "accessKey="   + accessKey
                + "&amount="      + amount
                + "&extraData="   + extraData
                + "&ipnUrl="      + ipnUrl
                + "&orderId="     + orderId
                + "&orderInfo="   + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + returnUrl
                + "&requestId="   + requestId
                + "&requestType=" + requestType;

        String signature = MoMoConfig.hmacSHA256(rawSignature, secretKey);

        // 3. Đóng gói body JSON
        // captureWallet yêu cầu gửi kèm accessKey trong body
        Map<String, Object> requestBody = Map.ofEntries(
                Map.entry("partnerCode",  partnerCode),
                Map.entry("accessKey",    accessKey),
                Map.entry("requestId",    requestId),
                Map.entry("amount",       amount),
                Map.entry("orderId",      orderId),
                Map.entry("orderInfo",    orderInfo),
                Map.entry("redirectUrl",  returnUrl),
                Map.entry("ipnUrl",       ipnUrl),
                Map.entry("extraData",    extraData),
                Map.entry("requestType",  requestType),
                Map.entry("signature",    signature),
                Map.entry("lang",         "vi")
        );

        // 4. Gửi POST sang MoMo và lấy payUrl
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(paymentUrl, entity, Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) throw new RuntimeException("MoMo trả về body rỗng");

            int resultCode = ((Number) body.get("resultCode")).intValue();
            if (resultCode != 0) {
                log.error("MoMo tạo thanh toán thất bại: {}", body.get("message"));
                throw new RuntimeException("MoMo lỗi: " + body.get("message"));
            }
            return (String) body.get("payUrl");

        } catch (Exception e) {
            log.error("Lỗi gọi API MoMo: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối đến MoMo", e);
        }
    }

    // -------------------------------------------------------------------------
    // XỬ LÝ RETURN URL (người dùng quay về sau khi thanh toán)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public boolean processPaymentReturn(Map<String, String> params) {
        if (!verifyReturnSignature(params)) {
            log.warn("MoMo return: chữ ký không hợp lệ, params={}", params);
            return false;
        }

        int    resultCode = Integer.parseInt(params.getOrDefault("resultCode", "-1"));
        String orderId    = params.getOrDefault("orderId", "");
        String transId    = params.getOrDefault("transId", "");

        // Cập nhật DB ngay tại Return URL vì IPN không hoạt động với localhost.
        // Idempotent: nếu IPN đã xử lý trước thì bỏ qua.
        paymentRepo.findByTxnRef(orderId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setMomoTransId(transId);
                payment.setResponseCode(String.valueOf(resultCode));
                if (resultCode == 0) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    Booking booking = payment.getBooking();
                    booking.setStatus(BookingStatus.CONFIRMED);
                    booking.setUpdatedAt(LocalDateTime.now());
                    bookingRepo.save(booking);
                    log.info("MoMo return: booking {} → CONFIRMED", orderId);
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                    log.info("MoMo return: booking {} thất bại, resultCode={}", orderId, resultCode);
                }
                paymentRepo.save(payment);
            }
        });

        return resultCode == 0;
    }

    // -------------------------------------------------------------------------
    // XỬ LÝ IPN / WEBHOOK (MoMo gọi server-to-server)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String processIpn(Map<String, Object> body) {
        try {
            // 1. Xác minh chữ ký
            if (!verifyIpnSignature(body)) {
                log.warn("MoMo IPN: chữ ký không hợp lệ");
                return "97"; // Invalid signature
            }

            int resultCode = ((Number) body.get("resultCode")).intValue();
            String orderId = (String) body.get("orderId");
            String transId = String.valueOf(body.get("transId"));

            // 2. Tìm Payment
            Payment payment = paymentRepo.findByTxnRef(orderId).orElse(null);
            if (payment == null) {
                log.warn("MoMo IPN: không tìm thấy payment với orderId={}", orderId);
                return "01";
            }

            // 3. Chống xử lý trùng lặp
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("MoMo IPN: payment {} đã được xử lý trước đó", orderId);
                return "0";
            }

            // 4. Cập nhật Payment
            payment.setMomoTransId(transId);
            payment.setResponseCode(String.valueOf(resultCode));
            payment.setUpdatedAt(LocalDateTime.now());

            if (resultCode == 0) {
                payment.setStatus(PaymentStatus.SUCCESS);
                // Cập nhật Booking → CONFIRMED
                Booking booking = payment.getBooking();
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepo.save(booking);
                log.info("MoMo IPN: booking {} đã CONFIRMED", orderId);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                log.info("MoMo IPN: thanh toán {} thất bại, resultCode={}", orderId, resultCode);
            }

            paymentRepo.save(payment);
            return "0";

        } catch (Exception e) {
            log.error("Lỗi xử lý MoMo IPN: {}", e.getMessage(), e);
            return "01";
        }
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    /**
     * Xác minh chữ ký trên Return URL (tham số GET).
     * Chuỗi ký của MoMo trả về:
     * accessKey=&amount=&extraData=&message=&orderId=&orderInfo=&orderType=
     * &partnerCode=&payType=&requestId=&responseTime=&resultCode=&transId=
     */
    private boolean verifyReturnSignature(Map<String, String> params) {
        try {
            String rawSignature = "accessKey="    + accessKey
                    + "&amount="       + params.get("amount")
                    + "&extraData="    + params.getOrDefault("extraData", "")
                    + "&message="      + params.getOrDefault("message", "")
                    + "&orderId="      + params.get("orderId")
                    + "&orderInfo="    + params.getOrDefault("orderInfo", "")
                    + "&orderType="    + params.getOrDefault("orderType", "")
                    + "&partnerCode="  + params.get("partnerCode")
                    + "&payType="      + params.getOrDefault("payType", "")
                    + "&requestId="    + params.get("requestId")
                    + "&responseTime=" + params.get("responseTime")
                    + "&resultCode="   + params.get("resultCode")
                    + "&transId="      + params.get("transId");

            String expected = MoMoConfig.hmacSHA256(rawSignature, secretKey);
            String received = params.getOrDefault("signature", "");
            return expected.equalsIgnoreCase(received);
        } catch (Exception e) {
            log.error("Lỗi xác minh chữ ký MoMo return: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Xác minh chữ ký trong body IPN (JSON).
     */
    private boolean verifyIpnSignature(Map<String, Object> body) {
        try {
            String rawSignature = "accessKey="    + accessKey
                    + "&amount="       + body.get("amount")
                    + "&extraData="    + body.getOrDefault("extraData", "")
                    + "&message="      + body.getOrDefault("message", "")
                    + "&orderId="      + body.get("orderId")
                    + "&orderInfo="    + body.getOrDefault("orderInfo", "")
                    + "&orderType="    + body.getOrDefault("orderType", "")
                    + "&partnerCode="  + body.get("partnerCode")
                    + "&payType="      + body.getOrDefault("payType", "")
                    + "&requestId="    + body.get("requestId")
                    + "&responseTime=" + body.get("responseTime")
                    + "&resultCode="   + body.get("resultCode")
                    + "&transId="      + body.get("transId");

            String expected = MoMoConfig.hmacSHA256(rawSignature, secretKey);
            String received = String.valueOf(body.getOrDefault("signature", ""));
            return expected.equalsIgnoreCase(received);
        } catch (Exception e) {
            log.error("Lỗi xác minh chữ ký MoMo IPN: {}", e.getMessage());
            return false;
        }
    }
}