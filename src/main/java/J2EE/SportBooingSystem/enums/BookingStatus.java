package J2EE.SportBooingSystem.enums;

public enum BookingStatus {
    PENDING,     // Chờ xác nhận (chưa thanh toán)
    CONFIRMED,   // Đã xác nhận
    CANCELLED,   // Đã hủy
    CANCEL_PENDING, // Đang chờ hủy (khách đã yêu cầu hủy, chờ chủ sân duyệt)
    COMPLETED    // Đã hoàn thành (sau giờ chơi)
}
