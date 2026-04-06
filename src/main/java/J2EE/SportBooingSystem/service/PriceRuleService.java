package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.PriceRuleRequest;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse;
import J2EE.SportBooingSystem.entity.PriceRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PriceRuleService {
    List<PriceRule> getRulesByField(Long fieldId);
    PriceRule createRule(Long fieldId, PriceRuleRequest request, String ownerEmail);
    PriceRule updateRule(Long ruleId, PriceRuleRequest request, String ownerEmail);
    void deleteRule(Long ruleId, String ownerEmail);

    /**
     * Tính giá đặt sân từ startTime đến endTime vào ngày bookingDate.
     * Mỗi 30 phút sẽ tìm rule phù hợp (priority cao nhất), nếu không có dùng pricePerHour cơ sở.
     */
    PriceCalculationResponse calculatePrice(Long fieldId, LocalDate bookingDate,
                                            LocalTime startTime, LocalTime endTime);

    BigDecimal getTotalPrice(Long fieldId, LocalDate bookingDate,
                             LocalTime startTime, LocalTime endTime);
}
