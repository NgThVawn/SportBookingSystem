package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.BookingRequest;
import J2EE.SportBooingSystem.dto.response.AvailabilityResponse;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.entity.Booking;
import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    Booking createBooking(BookingRequest request, String userEmail);
    void cancelBooking(Long bookingId, String userEmail, String reason);
    List<BookingResponse> getBookingsByUser(String userEmail);
    List<BookingResponse> getBookingsByOwner(String ownerEmail);
    BookingResponse getBookingByCode(String code, String requestorEmail);
    AvailabilityResponse getAvailability(Long fieldId, LocalDate date);
    void completeExpiredBookings(); // Gọi bởi scheduler
}
