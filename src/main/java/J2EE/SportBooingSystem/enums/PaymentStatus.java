package J2EE.SportBooingSystem.enums;

public enum PaymentStatus {
    PENDING,   // Chưa thanh toán
    SUCCESS,   // Thanh toán thành công
    FAILED,    // Thanh toán thất bại
    REFUNDED   // Đã hoàn tiền (dùng cho trường hợp cancel sau khi đã thanh toán)
}
