package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.*;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.entity.*;
import J2EE.SportBooingSystem.enums.DayType;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.exception.ForbiddenException;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import J2EE.SportBooingSystem.dto.request.BlockedTimeRequest;
import J2EE.SportBooingSystem.dto.request.PriceRuleRequest;
import J2EE.SportBooingSystem.entity.BlockedTime;
import J2EE.SportBooingSystem.entity.PriceRule;
import J2EE.SportBooingSystem.repository.BlockedTimeRepository;
import J2EE.SportBooingSystem.service.PriceRuleService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@RequiredArgsConstructor
public class OwnerController {

    private final FacilityService facilityService;
    private final FieldService fieldService;
    private final PriceRuleService priceRuleService;
    private final BlockedTimeRepository blockedRepo;
    private final BookingRepository bookingRepo;
    private final BookingService bookingService;
    private final UserService userService;

    // ─── FACILITY ─────────────────────────

    @GetMapping("/facilities")
    public String facilities(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("facilities", facilityService.findByOwner(ud.getUsername()));
        return "owner/facilities/list";
    }

    @GetMapping("/facilities/create")
    public String createFacilityPage(Model model) {
        model.addAttribute("facilityRequest", new FacilityRequest());
        return "owner/facilities/create";
    }

    @PostMapping("/facilities/create")
    public String createFacility(
            @Valid @ModelAttribute FacilityRequest request,
            BindingResult result,
            @AuthenticationPrincipal UserDetails ud,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            return "owner/facilities/create";
        }

        try {
            facilityService.create(request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Facility created successfully!");
            return "redirect:/owner/facilities";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "owner/facilities/create";
        }
    }

    @GetMapping("/facilities/{id}/edit")
    public String editFacility(@PathVariable Long id, Model model) {
        Facility f = facilityService.findById(id);
        FacilityRequest req = new FacilityRequest();
        req.setName(f.getName());
        req.setDescription(f.getDescription());
        req.setAddress(f.getAddress());
        req.setCity(f.getCity());
        req.setDistrict(f.getDistrict());
        req.setPhone(f.getPhone());
        req.setEmail(f.getEmail());
        req.setOpenTime(f.getOpenTime().toString());
        req.setCloseTime(f.getCloseTime().toString());

        model.addAttribute("facility", f);
        model.addAttribute("facilityRequest", req);
        return "owner/facilities/edit";
    }

    @PostMapping("/facilities/{id}/edit")
    public String updateFacility(
            @PathVariable Long id,
            @Valid @ModelAttribute FacilityRequest request,
            BindingResult result,
            @AuthenticationPrincipal UserDetails ud,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("facility", facilityService.findById(id));
            return "owner/facilities/edit";
        }

        try {
            facilityService.update(id, request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Updated successfully!");
            return "redirect:/owner/facilities";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "owner/facilities/edit";
        }
    }

    @PostMapping("/facilities/{id}/delete")
    public String deleteFacility(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
        try {
            facilityService.delete(id, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Facility deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete: " + e.getMessage());
        }
        return "redirect:/owner/facilities";
    }

    @PostMapping("/facilities/{id}/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleFacility(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            facilityService.toggleActive(id, ud.getUsername());
            Facility f = facilityService.findById(id);
            return ResponseEntity.ok(Map.of("success", true, "isActive", f.getIsActive()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ─── FIELD ─────────────────────────

    @GetMapping("/facilities/{facilityId}/fields")
    public String fields(@PathVariable Long facilityId, Model model) {
        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("fields", fieldService.findByFacility(facilityId));
        return "owner/fields/list";
    }

    @GetMapping("/facilities/{facilityId}/fields/create")
    public String createFieldPage(@PathVariable Long facilityId, Model model) {
        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("fieldRequest", new FieldRequest());
        model.addAttribute("sportTypes", SportType.values());
        return "owner/fields/create";
    }

    @PostMapping("/facilities/{facilityId}/fields/create")
    public String createField(
            @PathVariable Long facilityId,
            @Valid @ModelAttribute FieldRequest request,
            BindingResult result,
            @AuthenticationPrincipal UserDetails ud,
            Model model,
            RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/create";
        }

        try {
            fieldService.create(facilityId, request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Field created successfully!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/create";
        }
    }

    @GetMapping("/facilities/{facilityId}/fields/{id}/edit")
    public String editField(@PathVariable Long facilityId, @PathVariable Long id, Model model) {
        Field f = fieldService.findById(id);

        FieldRequest req = new FieldRequest();
        req.setName(f.getName());
        req.setSportType(f.getSportType());
        req.setDescription(f.getDescription());
        req.setSurfaceType(f.getSurfaceType());
        req.setCapacity(f.getCapacity());
        req.setPricePerHour(f.getPricePerHour());

        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("field", f);
        model.addAttribute("fieldRequest", req);
        model.addAttribute("sportTypes", SportType.values());
        return "owner/fields/edit";
    }

    @PostMapping("/facilities/{facilityId}/fields/{id}/edit")
    public String updateField(
            @PathVariable Long facilityId,
            @PathVariable Long id,
            @Valid @ModelAttribute FieldRequest request,
            BindingResult result,
            @AuthenticationPrincipal UserDetails ud,
            Model model,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("field", fieldService.findById(id));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/edit";
        }
        try {
            fieldService.update(id, request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Updated successfully!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("field", fieldService.findById(id));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/edit";
        }
    }

    // API đổi trạng thái Field (AJAX)
    @PostMapping("/facilities/{facilityId}/fields/{id}/status")
    @ResponseBody
    public ResponseEntity<?> changeFieldStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            fieldService.changeStatus(id, status, ud.getUsername());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Xóa Field
    @PostMapping("/facilities/{facilityId}/fields/{id}/delete")
    public String deleteField(
            @PathVariable Long facilityId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
        try {
            fieldService.delete(id, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields";
    }

    /** ── Quản lý giá (PriceRule) ────────────────────────────────── */

    @GetMapping("/facilities/{facilityId}/fields/{fieldId}/price-rules")
    public String priceRules(@PathVariable Long facilityId,
                             @PathVariable Long fieldId,
                             @AuthenticationPrincipal UserDetails ud,
                             Model model) {
        Facility facility = facilityService.findById(facilityId);
        Field field = fieldService.findById(fieldId);
        List<PriceRule> rules = priceRuleService.getRulesByField(fieldId);
        model.addAttribute("facility", facility);
        model.addAttribute("field", field);
        model.addAttribute("rules", rules);
        model.addAttribute("newRule", new PriceRuleRequest());
        model.addAttribute("dayTypes", DayType.values());
        return "owner/price-rules/manage";
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/price-rules/create")
    public String createPriceRule(@PathVariable Long facilityId,
                                  @PathVariable Long fieldId,
                                  @Valid @ModelAttribute("newRule") PriceRuleRequest req,
                                  BindingResult br,
                                  @AuthenticationPrincipal UserDetails ud,
                                  RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Dữ liệu không hợp lệ");
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/price-rules";
        }
        try {
            priceRuleService.createRule(fieldId, req, ud.getUsername());
            ra.addFlashAttribute("success", "Thêm quy tắc giá thành công");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/price-rules";
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/price-rules/{ruleId}/delete")
    public String deletePriceRule(@PathVariable Long facilityId,
                                  @PathVariable Long fieldId,
                                  @PathVariable Long ruleId,
                                  @AuthenticationPrincipal UserDetails ud,
                                  RedirectAttributes ra) {
        try {
            priceRuleService.deleteRule(ruleId, ud.getUsername());
            ra.addFlashAttribute("success", "Đã xóa quy tắc giá");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/price-rules";
    }

    /** ── Quản lý chặn giờ (BlockedTime) ────────────────────────── */

    @GetMapping("/facilities/{facilityId}/fields/{fieldId}/blocked-times")
    public String blockedTimes(@PathVariable Long facilityId,
                               @PathVariable Long fieldId,
                               @RequestParam(required = false)
                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                               @AuthenticationPrincipal UserDetails ud,
                               Model model) {
        if (date == null) date = LocalDate.now();
        Field field = fieldService.findById(fieldId);
        List<BlockedTime> blocks = blockedRepo.findByFieldAndDateOrderByStartTime(field, date);
        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("field", field);
        model.addAttribute("blocks", blocks);
        model.addAttribute("selectedDate", date);
        model.addAttribute("newBlock", new BlockedTimeRequest());
        return "owner/blocked-times/manage";
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/blocked-times/create")
    public String createBlockedTime(@PathVariable Long facilityId,
                                    @PathVariable Long fieldId,
                                    @Valid @ModelAttribute("newBlock") BlockedTimeRequest req,
                                    BindingResult br,
                                    @AuthenticationPrincipal UserDetails ud,
                                    RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Dữ liệu không hợp lệ");
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times";
        }
        try {
            Field field = fieldService.findById(fieldId);
            // Xác nhận owner
            if (!field.getFacility().getOwner().getEmail().equals(ud.getUsername()))
                throw new ForbiddenException("Không có quyền");
            if (bookingRepo.existsConflict(field, req.getDate(), req.getStartTime(), req.getEndTime()))
                throw new IllegalStateException("Khung giờ này đã có người đặt, không thể chặn");
            BlockedTime bt = BlockedTime.builder()
                    .field(field).date(req.getDate())
                    .startTime(req.getStartTime()).endTime(req.getEndTime())
                    .reason(req.getReason()).build();
            blockedRepo.save(bt);
            ra.addFlashAttribute("success", "Đã chặn khung giờ thành công");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId
                + "/blocked-times?date=" + req.getDate();
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/blocked-times/{id}/delete")
    public String deleteBlockedTime(@PathVariable Long facilityId,
                                    @PathVariable Long fieldId,
                                    @PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud,
                                    RedirectAttributes ra) {
        try {
            BlockedTime bt = blockedRepo.findById(id).orElseThrow();
            if (!bt.getField().getFacility().getOwner().getEmail().equals(ud.getUsername()))
                throw new ForbiddenException("Không có quyền");
            blockedRepo.delete(bt);
            ra.addFlashAttribute("success", "Đã xóa khung giờ bị chặn");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times";
    }

    /** ── Quản lý booking (Owner xem danh sách) ──────────────────── */

    @GetMapping("/bookings")
    public String ownerBookings(@RequestParam(required = false)
                                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @AuthenticationPrincipal UserDetails ud,
                                Model model) {
        List<BookingResponse> bookings = date != null
                ? bookingService.getBookingsByOwner(ud.getUsername()).stream()
                  .filter(b -> b.getBookingDate().isEqual(date)).toList()
                : bookingService.getBookingsByOwner(ud.getUsername());
        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedDate", date);
        return "owner/bookings/list";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String ownerCancelBooking(@PathVariable Long id,
                                     @RequestParam(required = false) String reason,
                                     @AuthenticationPrincipal UserDetails ud,
                                     RedirectAttributes ra) {
        try {
            bookingService.cancelBooking(id, ud.getUsername(), reason);
            ra.addFlashAttribute("success", "Đã hủy booking");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/bookings";
    }
    @GetMapping("")
    public String ownerDashboard(Model model, Authentication authentication) {
        // 1. Lấy thông tin Chủ sân đang đăng nhập hiện tại
        String email = authentication.getName();
        User currentOwner = userService.findByEmail(email);

        // 2. LẤY CÁC CON SỐ THỐNG KÊ (Dùng cho các thẻ Card ở trên cùng trang)
        // Giả sử bạn có các hàm này trong Service, nếu chưa có thì nhờ Backend viết thêm nhé
//        long totalFacilities = facilityService.countByOwnerId(currentOwner.getId());
//        long pendingBookings = bookingService.countPendingBookingsByOwnerId(currentOwner.getId());
//        double monthlyRevenue = bookingService.calculateMonthlyRevenueByOwnerId(currentOwner.getId());

        // 3. LẤY DANH SÁCH SÂN CỦA RIÊNG CHỦ SÂN NÀY (Để hiển thị bảng quản lý nhanh)
        // Sắp xếp sân mới tạo lên đầu, lấy 5 sân để giao diện không bị quá dài
//        var myFacilities = facilityService.findByOwnerId(
//                currentOwner.getId(),
//                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
//        );

        // 4. LẤY DANH SÁCH LỊCH ĐẶT SÂN MỚI NHẤT (Để duyệt/từ chối nhanh)
//        var recentBookings = bookingService.findRecentBookingsByOwnerId(
//                currentOwner.getId(),
//                PageRequest.of(0, 5)
//        );

//        // 5. Đẩy toàn bộ dữ liệu ra View (Thymeleaf)
//        model.addAttribute("totalFacilities", totalFacilities);
//        model.addAttribute("pendingBookings", pendingBookings);
//        model.addAttribute("monthlyRevenue", monthlyRevenue);
//
//        model.addAttribute("myFacilities", myFacilities);
//        model.addAttribute("recentBookings", recentBookings);

        // Vẫn giữ lại SportType nếu form Thêm Sân Nhanh (Modal) trên Dashboard cần dùng
        model.addAttribute("sportTypes", SportType.values());

        // Trả về file HTML (Lưu ý: Bỏ dấu gạch chéo ở đầu đi để Thymeleaf chạy chuẩn nhất)
        return "owner/index";
    }
}