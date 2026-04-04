package J2EE.SportBooingSystem.dto.request;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long bookingId;
    private Integer rating;
    private String comment;
}