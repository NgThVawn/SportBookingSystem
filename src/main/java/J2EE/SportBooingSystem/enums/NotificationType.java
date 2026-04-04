package J2EE.SportBooingSystem.enums;

public enum NotificationType {
    // Booking
    BOOKING_CREATED,       // USER: bạn đã đặt sân
    BOOKING_CONFIRMED,     // USER: chủ sân xác nhận
    BOOKING_CANCELLED,     // USER: booking bị hủy
    BOOKING_COMPLETED,     // USER: booking đã hoàn thành
    BOOKING_AUTO_CANCELLED,// USER: tự động hủy do chưa thanh toán
    // Payment
    PAYMENT_SUCCESS,       // USER: thanh toán thành công
    PAYMENT_FAILED,        // USER: thanh toán thất bại
    // Owner nhận
    NEW_BOOKING,           // OWNER: có booking mới tại sân của bạn
    BOOKING_CANCEL_REQUEST,// OWNER: khách yêu cầu hủy sát giờ
    BOOKING_CUSTOMER_CANCELLED, // OWNER: khách đã hủy (trước 24h)
    PAYMENT_RECEIVED,      // OWNER: nhận được thanh toán
    BOOKING_AUTO_CANCELLED_OWNER, // OWNER: booking hủy tự động
    // Cancel request result
    CANCEL_APPROVED,       // USER: yêu cầu hủy được chấp thuận
    CANCEL_REJECTED,       // USER: yêu cầu hủy bị từ chối
    // Facility
    FACILITY_PENDING,      // ADMIN: có cơ sở mới chờ duyệt
    FACILITY_APPROVED,     // OWNER: cơ sở được duyệt
    FACILITY_REJECTED,     // OWNER: cơ sở bị từ chối / bị khóa
}
