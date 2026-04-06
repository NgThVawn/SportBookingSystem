package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.BookingRequest;
import J2EE.SportBooingSystem.dto.request.BookingExtraItemRequest;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse;
import J2EE.SportBooingSystem.dto.response.SelectedExtraItemView;
import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.ExtraService;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.service.BookingService;
import J2EE.SportBooingSystem.service.ExtraServiceService;
import J2EE.SportBooingSystem.service.FieldService;
import J2EE.SportBooingSystem.service.PriceRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private final BookingService bookingService;
    private final PriceRuleService priceRuleService;
    private final FieldService fieldService;
    private final ExtraServiceService extraServiceService;

    /** Trang lịch sử đặt sân của user */
    @GetMapping
    public String myBookings(@AuthenticationPrincipal UserDetails ud, Model model) {
        List<BookingResponse> bookings = bookingService.getBookingsByUser(ud.getUsername());
        model.addAttribute("bookings", bookings);
        return "booking/history";
    }

    /**
     * Trang đặt sân: GET /bookings/new?fieldId=1&date=2024-12-31
     * Hiển thị form chọn giờ + tính giá
     */
    @GetMapping("/new")
    public String newBookingPage(@RequestParam Long fieldId,
                                 @RequestParam String date,
                                 @RequestParam(required = false) String start,
                                 @RequestParam(required = false) String end,
                                 @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                                 HttpServletRequest request,
                                 Model model) {
        LocalDate bookingDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
        LocalTime startTime = (start != null && !start.isBlank()) ? LocalTime.parse(start) : null;
        LocalTime endTime = (end != null && !end.isBlank()) ? LocalTime.parse(end) : null;

        bookingRequest.setFieldId(fieldId);
        bookingRequest.setBookingDate(bookingDate);
        bookingRequest.setStartTime(startTime);
        bookingRequest.setEndTime(endTime);

        List<ExtraService> availableServices = extraServiceService.findByField(fieldId);
        Map<Long, ExtraService> availableServiceMap = availableServices.stream()
                .collect(java.util.stream.Collectors.toMap(ExtraService::getId, s -> s));

        List<BookingExtraItemRequest> parsedItems = parseExtraItemsFromRequest(request);
        if (!parsedItems.isEmpty()) {
            bookingRequest.setExtraItems(parsedItems);
        } else if (bookingRequest.getExtraItems() != null) {
            bookingRequest.setExtraItems(bookingRequest.getExtraItems().stream()
                    .filter(item -> item != null
                            && item.getServiceId() != null
                            && item.getQuantity() != null
                            && item.getQuantity() > 0)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        }

        populateBookingPageModel(model, fieldId, bookingDate, startTime, endTime, bookingRequest.getExtraItems());
        return "booking/create";
    }

    /** Xem giá trước khi xác nhận (AJAX) */
    @PostMapping("/preview-price")
    @ResponseBody
    public PriceCalculationResponse previewPrice(@RequestBody BookingRequest req) {
        return priceRuleService.calculatePrice(
                req.getFieldId(), req.getBookingDate(), req.getStartTime(), req.getEndTime());
    }

    /** Xác nhận đặt sân */
    @PostMapping
    public String createBooking(@Valid @ModelAttribute BookingRequest req,
                                BindingResult br,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra,
                                HttpServletRequest request,
                                Model model) {
        List<BookingExtraItemRequest> parsedItems = parseExtraItemsFromRequest(request);
        List<ExtraService> availableServices = extraServiceService.findByField(req.getFieldId());
        Map<Long, ExtraService> availableServiceMap = availableServices.stream()
                .collect(java.util.stream.Collectors.toMap(ExtraService::getId, s -> s));

        List<BookingExtraItemRequest> invalidItems = filterInvalidSelectedItems(parsedItems.isEmpty() ? req.getExtraItems() : parsedItems, availableServiceMap);
        if (!invalidItems.isEmpty()) {
            model.addAttribute("error", buildInvalidServiceMessage(invalidItems, availableServiceMap));
            List<BookingExtraItemRequest> itemsToDisplay = parsedItems.isEmpty() ? req.getExtraItems() : parsedItems;
            populateBookingPageModel(model, req.getFieldId(), req.getBookingDate(), req.getStartTime(), req.getEndTime(), itemsToDisplay);
            return "booking/create";
        }

        if (!parsedItems.isEmpty()) {
            req.setExtraItems(parsedItems);
        } else if (req.getExtraItems() != null) {
            req.setExtraItems(req.getExtraItems().stream()
                    .filter(item -> item != null
                            && item.getServiceId() != null
                            && item.getQuantity() != null
                            && item.getQuantity() > 0)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        }

        if (br.hasErrors()) {
            populateBookingPageModel(model, req.getFieldId(), req.getBookingDate(), req.getStartTime(), req.getEndTime(), req.getExtraItems());
            return "booking/create";
        }
        try {
            Booking b = bookingService.createBooking(req, ud.getUsername());
            // Redirect tới trang checkout để thanh toán VNPay
            return "redirect:/payment/checkout?bookingCode=" + b.getBookingCode();
        }  catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            populateBookingPageModel(model, req.getFieldId(), req.getBookingDate(), req.getStartTime(), req.getEndTime(), req.getExtraItems());
            return "booking/create";
        }
    }

    private void populateBookingPageModel(Model model,
                                          Long fieldId,
                                          LocalDate bookingDate,
                                          LocalTime startTime,
                                          LocalTime endTime,
                                          List<BookingExtraItemRequest> selectedItems) {
        Field field = fieldService.findById(fieldId);
        BigDecimal fieldPrice = BigDecimal.ZERO;
        if (bookingDate != null && startTime != null && endTime != null) {
            fieldPrice = priceRuleService.getTotalPrice(fieldId, bookingDate, startTime, endTime);
        }

        List<ExtraService> services = extraServiceService.findByField(fieldId);
        Map<Long, ExtraService> serviceMap = services.stream()
            .collect(java.util.stream.Collectors.toMap(ExtraService::getId, s -> s));

        List<Long> selectedServiceIds = (selectedItems == null ? List.<Long>of() : selectedItems.stream()
            .filter(item -> item != null && item.getServiceId() != null)
            .map(BookingExtraItemRequest::getServiceId)
            .distinct()
            .toList());
        Map<Long, ExtraService> selectedServiceMap = extraServiceService.findAllByIds(selectedServiceIds).stream()
            .collect(java.util.stream.Collectors.toMap(ExtraService::getId, s -> s));

        List<SelectedExtraItemView> selectedExtraItems = new ArrayList<>();
        if (selectedItems != null) {
            for (BookingExtraItemRequest item : selectedItems) {
                if (item == null || item.getServiceId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                    continue;
                }

                ExtraService service = selectedServiceMap.get(item.getServiceId());
                String serviceName = service != null ? service.getName() : "Dịch vụ #" + item.getServiceId();
                String unit = service != null ? service.getUnit() : "đơn vị";
                BigDecimal unitPrice = service != null && service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                selectedExtraItems.add(SelectedExtraItemView.builder()
                        .serviceId(item.getServiceId())
                        .serviceName(serviceName)
                        .quantity(item.getQuantity())
                        .unit(unit)
                        .unitPrice(unitPrice)
                        .subtotal(subtotal)
                        .build());
            }
        }

        model.addAttribute("fieldId", fieldId);
        model.addAttribute("field", field);
        model.addAttribute("facility", field.getFacility());
        model.addAttribute("date", bookingDate);
        model.addAttribute("start", startTime);
        model.addAttribute("end", endTime);
        model.addAttribute("fieldPrice", fieldPrice);
        model.addAttribute("services", services);
        model.addAttribute("serviceMap", serviceMap);
        model.addAttribute("selectedServiceMap", selectedServiceMap);
        model.addAttribute("selectedExtraItems", selectedExtraItems);
    }

    private List<BookingExtraItemRequest> parseExtraItemsFromRequest(HttpServletRequest request) {
        Pattern keyPattern = Pattern.compile("^extraItems\\[(\\d+)]\\.(serviceId|quantity)$");
        Map<Integer, BookingExtraItemRequest> itemByIndex = new HashMap<>();

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            Matcher matcher = keyPattern.matcher(entry.getKey());
            if (!matcher.matches()) {
                continue;
            }
            String[] values = entry.getValue();
            if (values == null || values.length == 0 || values[0] == null || values[0].isBlank()) {
                continue;
            }

            Integer index = Integer.parseInt(matcher.group(1));
            String field = matcher.group(2);
            BookingExtraItemRequest item = itemByIndex.computeIfAbsent(index, i -> new BookingExtraItemRequest());

            if ("serviceId".equals(field)) {
                item.setServiceId(parseLongSafe(values[0]));
            } else if ("quantity".equals(field)) {
                item.setQuantity(parseIntSafe(values[0]));
            }
        }

        // Fallback: parse trực tiếp từ query string để xử lý mọi kiểu encode key
        String rawQuery = request.getQueryString();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                String[] parts = pair.split("=", 2);
                String rawKey = parts.length > 0 ? parts[0] : "";
                String rawValue = parts.length > 1 ? parts[1] : "";
                String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);

                Matcher matcher = keyPattern.matcher(key);
                if (!matcher.matches() || value.isBlank()) {
                    continue;
                }

                Integer index = Integer.parseInt(matcher.group(1));
                String field = matcher.group(2);
                BookingExtraItemRequest item = itemByIndex.computeIfAbsent(index, i -> new BookingExtraItemRequest());

                if ("serviceId".equals(field)) {
                    item.setServiceId(parseLongSafe(value));
                } else if ("quantity".equals(field)) {
                    item.setQuantity(parseIntSafe(value));
                }
            }
        }

        return itemByIndex.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .filter(item -> item.getServiceId() != null && item.getQuantity() != null && item.getQuantity() > 0)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<BookingExtraItemRequest> filterInvalidSelectedItems(List<BookingExtraItemRequest> items,
                                                                     Map<Long, ExtraService> availableServiceMap) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<BookingExtraItemRequest> invalidItems = new ArrayList<>();
        for (BookingExtraItemRequest item : items) {
            if (item == null || item.getServiceId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            if (!availableServiceMap.containsKey(item.getServiceId())) {
                invalidItems.add(item);
            }
        }
        return invalidItems;
    }

    private String buildInvalidServiceMessage(List<BookingExtraItemRequest> invalidItems,
                                              Map<Long, ExtraService> availableServiceMap) {
        StringBuilder sb = new StringBuilder("Dịch vụ bạn chọn không thuộc cơ sở/sân hiện tại: ");
        for (int i = 0; i < invalidItems.size(); i++) {
            BookingExtraItemRequest item = invalidItems.get(i);
            ExtraService service = availableServiceMap.get(item.getServiceId());
            String name = service != null ? service.getName() : ("#" + item.getServiceId());
            sb.append(name);
            if (i < invalidItems.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(". Vui lòng quay lại chọn dịch vụ đúng của cơ sở này.");
        return sb.toString();
    }

    private Long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

   /** Hủy booking (User) */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "Khách hàng đổi ý") String reason,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra) {
        try {
            // Hứng trạng thái trả về từ Service
            J2EE.SportBooingSystem.enums.BookingStatus newStatus = 
                    bookingService.cancelBooking(id, ud.getUsername(), reason);

            // Báo câu thông báo phù hợp
            if (newStatus == J2EE.SportBooingSystem.enums.BookingStatus.CANCEL_PENDING) {
                ra.addFlashAttribute("success", "Do bạn hủy sát giờ (dưới 24h), yêu cầu đã được gửi đến Chủ sân để chờ duyệt!");
            } else {
                ra.addFlashAttribute("success", "Đã hủy đơn đặt sân thành công!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings";
    }
}
