package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.entity.Booking;

import java.util.Map;

public interface MoMoService {

    /**
     * Gửi yêu cầu tạo giao dịch sang MoMo, trả về payUrl để redirect người dùng.
     */
    String createPaymentUrl(Booking booking);

    /**
     * Xử lý tham số MoMo trả về trên Return URL (GET).
     * @return true nếu resultCode == 0 (thành công)
     */
    boolean processPaymentReturn(Map<String, String> params);

    /**
     * Xử lý IPN (Webhook) do MoMo gọi về server.
     * Cập nhật trạng thái Booking trong DB.
     * @return "0" nếu xử lý thành công, mã lỗi khác nếu thất bại
     */
    String processIpn(Map<String, Object> body);
}