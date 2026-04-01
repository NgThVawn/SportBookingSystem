package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);
    Optional<Payment> findByTxnRef(String txnRef);
}