package J2EE.SportBooingSystem.enums;

public enum BookingStatus {
    PENDING,     // Chờ xác nhận (chưa thanh toán)
    CONFIRMED,   // Đã xác nhận
    CANCELLED,   // Đã hủy
    COMPLETED    // Đã hoàn thành (sau giờ chơi)
}
