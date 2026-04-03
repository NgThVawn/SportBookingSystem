package J2EE.SportBooingSystem.entity;

import J2EE.SportBooingSystem.enums.PaymentMethod;
import J2EE.SportBooingSystem.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Liên kết 1-1 với Booking */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /** Mã giao dịch do VNPay cấp (vnp_TransactionNo) */
    @Column(length = 50)
    private String vnpTransactionNo;

    /** Mã đơn hàng gửi sang cổng thanh toán (bookingCode, hoặc bookingCode-timestamp với MoMo) */
    @Column(nullable = false, length = 50)
    private String txnRef;

    /** Số tiền (VND) */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Mã ngân hàng user chọn (vnp_BankCode) */
    @Column(length = 20)
    private String bankCode;

    /** Loại thẻ: ATM / QRCODE / VISA... (vnp_CardType) */
    @Column(length = 20)
    private String cardType;

    /** Ngày giờ thanh toán theo VNPay (vnp_PayDate) */
    @Column(length = 20)
    private String payDate;

    /** Mã phản hồi VNPay: 00 = thành công */
    @Column(length = 10)
    private String responseCode;

    /** Mã trạng thái giao dịch VNPay (vnp_TransactionStatus) */
    @Column(length = 10)
    private String transactionStatus;

    /** Nội dung đơn hàng gửi VNPay */
    @Column(length = 255)
    private String orderInfo;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.VNPAY;

    @Column(length = 50)
    private String momoTransId;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
