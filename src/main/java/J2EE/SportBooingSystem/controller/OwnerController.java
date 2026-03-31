package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.*;
import J2EE.SportBooingSystem.entity.*;
import J2EE.SportBooingSystem.enums.SlotStatus;
import J2EE.SportBooingSystem.enums.SportType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@RequiredArgsConstructor
public class OwnerController {

    private final FacilityService facilityService;
    private final FieldService fieldService;
    private final TimeSlotService timeSlotService;
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
        req.setSlotDuration(f.getSlotDuration());

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

    // ─── TIME SLOT ─────────────────────────

    @GetMapping("/facilities/{facilityId}/fields/{fieldId}/slots")
    public String fieldSlots(@PathVariable Long facilityId, @PathVariable Long fieldId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        Field field = fieldService.findById(fieldId);
        if (!field.getFacility().getId().equals(facilityId)) return "redirect:/owner/facilities";

        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("field", field);
        model.addAttribute("selectedDate", queryDate);
        model.addAttribute("slots", timeSlotService.getAllSlots(fieldId, queryDate));

        TimeSlotGenerateRequest genReq = new TimeSlotGenerateRequest();
        genReq.setFieldId(fieldId);
        genReq.setStartDate(queryDate);
        genReq.setEndDate(queryDate.plusDays(7));
        model.addAttribute("genRequest", genReq);

        return "owner/fields/timeslot";
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/slots/generate")
    public String generateSlots(@PathVariable Long facilityId, @PathVariable Long fieldId, @Valid @ModelAttribute("genRequest") TimeSlotGenerateRequest request, BindingResult result, @AuthenticationPrincipal UserDetails ud, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Dữ liệu nhập vào không hợp lệ!");
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/slots";
        }
        try {
            request.setFieldId(fieldId);
            timeSlotService.generateSlots(request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Hệ thống đã tự động chia khung giờ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Không thể tạo lịch: " + e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/slots?date=" + request.getStartDate();
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/slots/{slotId}/update")
    @ResponseBody
    public ResponseEntity<?> updateQuickSlot(
            @PathVariable Long slotId,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) SlotStatus status,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            timeSlotService.updateSlotDetail(slotId, price, status, ud.getUsername());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
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