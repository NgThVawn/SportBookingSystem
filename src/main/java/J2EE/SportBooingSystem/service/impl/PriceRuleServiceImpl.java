package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.PriceRuleRequest;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse.PriceSegment;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.entity.PriceRule;
import J2EE.SportBooingSystem.enums.DayType;
import J2EE.SportBooingSystem.exception.ForbiddenException;
import J2EE.SportBooingSystem.repository.FieldRepository;
import J2EE.SportBooingSystem.repository.PriceRuleRepository;
import J2EE.SportBooingSystem.service.PriceRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PriceRuleServiceImpl implements PriceRuleService {

    private final PriceRuleRepository ruleRepo;
    private final FieldRepository fieldRepo;

    @Override
    public List<PriceRule> getRulesByField(Long fieldId) {
        Field field = fieldRepo.findById(fieldId).orElseThrow();
        return ruleRepo.findByFieldOrderByPriorityDesc(field);
    }

    @Override
    @Transactional
    public PriceRule createRule(Long fieldId, PriceRuleRequest req, String ownerEmail) {
        Field field = validateOwner(fieldId, ownerEmail);
        if (!req.getStartTime().isBefore(req.getEndTime()))
            throw new IllegalArgumentException("Giờ bắt đầu phải nhỏ hơn giờ kết thúc");

        return ruleRepo.save(PriceRule.builder()
                .field(field)
                .name(req.getName())
                .dayType(req.getDayType())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .pricePerHour(req.getPricePerHour())
                .priority(req.getPriority() == null ? 0 : req.getPriority())
                .isActive(true)
                .build());
    }

    @Override
    @Transactional
    public PriceRule updateRule(Long ruleId, PriceRuleRequest req, String ownerEmail) {
        PriceRule rule = ruleRepo.findById(ruleId).orElseThrow();
        validateOwner(rule.getField().getId(), ownerEmail);
        rule.setName(req.getName());
        rule.setDayType(req.getDayType());
        rule.setStartTime(req.getStartTime());
        rule.setEndTime(req.getEndTime());
        rule.setPricePerHour(req.getPricePerHour());
        rule.setPriority(req.getPriority() == null ? 0 : req.getPriority());
        return ruleRepo.save(rule);
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId, String ownerEmail) {
        PriceRule rule = ruleRepo.findById(ruleId).orElseThrow();
        validateOwner(rule.getField().getId(), ownerEmail);
        ruleRepo.delete(rule);
    }

    @Override
    public PriceCalculationResponse calculatePrice(Long fieldId, LocalDate date,
                                                   LocalTime start, LocalTime end) {
        Field field = fieldRepo.findById(fieldId).orElseThrow();
        boolean isWeekend = (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY);
        List<PriceRule> rules = ruleRepo.findActiveRulesForDay(field, isWeekend);

        List<PriceSegment> segments = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Chia nhỏ từng 30 phút để áp dụng rule chính xác
        LocalTime cursor = start;
        while (cursor.isBefore(end)) {
            LocalTime next = cursor.plusMinutes(30);
            if (next.isAfter(end)) next = end;

            // Tìm rule có priority cao nhất áp dụng cho cursor
            final LocalTime slotStart = cursor;
            Optional<PriceRule> matched = rules.stream()
                    .filter(r -> !r.getStartTime().isAfter(slotStart) && r.getEndTime().isAfter(slotStart))
                    .findFirst(); // đã sắp xếp theo priority DESC

            BigDecimal pricePerHour = matched
                    .map(PriceRule::getPricePerHour)
                    .orElse(field.getPricePerHour());
            String ruleName = matched.map(PriceRule::getName).orElse("Giá cơ sở");

            double segHours = Duration.between(cursor, next).toMinutes() / 60.0;
            BigDecimal subtotal = pricePerHour
                    .multiply(BigDecimal.valueOf(segHours))
                    .setScale(0, RoundingMode.HALF_UP);
            total = total.add(subtotal);

            String timeRange = cursor + " – " + next;
            // Gộp với segment trước nếu cùng rule
            if (!segments.isEmpty()) {
                PriceSegment prev = segments.get(segments.size() - 1);
                if (prev.getRuleName().equals(ruleName)
                        && prev.getPricePerHour().compareTo(pricePerHour) == 0) {
                    prev.setTimeRange(prev.getTimeRange().split(" – ")[0] + " – " + next);
                    prev.setHours(prev.getHours() + segHours);
                    prev.setSubtotal(prev.getSubtotal().add(subtotal));
                    cursor = next;
                    continue;
                }
            }
            segments.add(new PriceSegment(timeRange, ruleName, pricePerHour, segHours, subtotal));
            cursor = next;
        }

        return PriceCalculationResponse.builder()
                .totalPrice(total)
                .segments(segments)
                .build();
    }

    @Override
    public BigDecimal getTotalPrice(Long fieldId, LocalDate date,
                                    LocalTime start, LocalTime end) {
        return calculatePrice(fieldId, date, start, end).getTotalPrice();
    }

    private Field validateOwner(Long fieldId, String ownerEmail) {
        Field field = fieldRepo.findById(fieldId).orElseThrow();
        if (!field.getFacility().getOwner().getEmail().equals(ownerEmail))
            throw new ForbiddenException("Bạn không có quyền thay đổi sân này");
        return field;
    }
}
