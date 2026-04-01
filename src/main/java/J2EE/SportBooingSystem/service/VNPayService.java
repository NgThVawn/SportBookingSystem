package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.entity.Booking;
import java.util.Map;

public interface VNPayService {

    /**
     * Tạo URL thanh toán VNPay cho booking.
     * @param booking  Booking cần thanh toán
     * @param clientIp IP của user (lấy từ HttpServletRequest)
     * @return URL đầy đủ để redirect user tới VNPay
     */
    String createPaymentUrl(Booking booking, String clientIp);

    /**
     * Xử lý phản hồi từ VNPay (return URL hoặc IPN).
     * Verify chữ ký và cập nhật trạng thái Booking + Payment.
     * @param params Tất cả query param từ VNPay gửi về
     * @return true nếu thanh toán thành công và hợp lệ
     */
    boolean processPaymentReturn(Map<String, String> params);

    /**
     * Xử lý IPN (Instant Payment Notification) từ VNPay server.
     * Giống processPaymentReturn nhưng trả về response code cho VNPay.
     * @return "00" nếu xử lý thành công, "97" nếu sai chữ ký, "01" nếu lỗi khác
     */
    String processIpn(Map<String, String> params);
}
