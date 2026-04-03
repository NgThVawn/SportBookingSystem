package J2EE.SportBooingSystem.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class MoMoConfig {

    /**
     * Tạo chữ ký HMAC-SHA256.
     *
     * @param data      Chuỗi dữ liệu đã được sắp xếp theo thứ tự chuẩn MoMo
     * @param secretKey SecretKey từ application.properties
     * @return Chuỗi hex của chữ ký
     */
    public static String hmacSHA256(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký HMAC-SHA256", e);
        }
    }
}
