package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.*;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.entity.*;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.enums.DayType;
import J2EE.SportBooingSystem.enums.FacilityStatus;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.exception.ForbiddenException;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import J2EE.SportBooingSystem.repository.BlockedTimeRepository;
import J2EE.SportBooingSystem.repository.FieldRepository;
import J2EE.SportBooingSystem.repository.PriceRuleRepository;
import J2EE.SportBooingSystem.service.PriceRuleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasAnyRole('OWNER')")
@RequiredArgsConstructor
public class OwnerController {

    private final FacilityService facilityService;
    private final FieldService fieldService;
    private final PriceRuleService priceRuleService;
    private final PriceRuleRepository priceRuleRepository;
    private final BlockedTimeRepository blockedRepo;
    private final BookingRepository bookingRepo;
    private final FieldRepository fieldRepo;
    private final BookingService bookingService;
    private final UserService userService;
    private final ExtraServiceService extraServiceService;

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
    public String editFacility(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Facility f = facilityService.findById(id);
        
        if (f.getStatus() == FacilityStatus.PENDING_APPROVAL || f.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Không thể chỉnh sửa thông tin khi cơ sở đang chờ duyệt hoặc bị khóa!");
            return "redirect:/owner/facilities";
        }

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

        Facility f = facilityService.findById(id);
        if (f.getStatus() == FacilityStatus.PENDING_APPROVAL || f.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Hành động bị chặn do trạng thái cơ sở không cho phép!");
            return "redirect:/owner/facilities";
        }

        if (result.hasErrors()) {
            model.addAttribute("facility", f);
            return "owner/facilities/edit";
        }

        try {
            facilityService.update(id, request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Cập nhật thông tin thành công!");
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

    @PostMapping("/facilities/{id}/status")
    @ResponseBody
    public ResponseEntity<?> changeFacilityStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            J2EE.SportBooingSystem.enums.FacilityStatus newStatus = 
                J2EE.SportBooingSystem.enums.FacilityStatus.valueOf(status.toUpperCase());
                
            facilityService.changeStatus(id, newStatus, ud.getUsername());
            return ResponseEntity.ok(Map.of("success", true));
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
    public String createFieldPage(@PathVariable Long facilityId, Model model, RedirectAttributes ra) {
        Facility f = facilityService.findById(facilityId);
        
        if (f.getStatus() == FacilityStatus.PENDING_APPROVAL || f.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Bạn cần đợi Admin duyệt cơ sở trước khi tạo sân tập con!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        }

        model.addAttribute("facility", f);
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

        Facility f = facilityService.findById(facilityId);
        
        if (f.getStatus() == FacilityStatus.PENDING_APPROVAL || f.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Hành động không hợp lệ!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        }

        if (result.hasErrors()) {
            model.addAttribute("facility", f);
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/create";
        }
        try {
            fieldService.create(facilityId, request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Tạo sân tập thành công!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("facility", f);
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/create";
        }
    }

    @GetMapping("/facilities/{facilityId}/fields/{id}/edit")
    public String editField(@PathVariable Long facilityId, @PathVariable Long id, Model model, RedirectAttributes ra) {
        Facility facility = facilityService.findById(facilityId);
        if (facility.getStatus() == FacilityStatus.PENDING_APPROVAL || facility.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Bạn không thể chỉnh sửa khi cơ sở chưa được duyệt!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        }

        Field f = fieldService.findById(id);

        FieldRequest req = new FieldRequest();
        req.setName(f.getName());
        req.setSportType(f.getSportType());
        req.setDescription(f.getDescription());
        req.setSurfaceType(f.getSurfaceType());
        req.setCapacity(f.getCapacity());
        req.setPricePerHour(f.getPricePerHour());

        model.addAttribute("facility", facility);
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
        
        Facility facility = facilityService.findById(facilityId);
        if (facility.getStatus() == FacilityStatus.PENDING_APPROVAL || facility.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Hành động bị chặn!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        }

        if (result.hasErrors()) {
            model.addAttribute("facility", facility);
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
            model.addAttribute("facility", facility);
            model.addAttribute("field", fieldService.findById(id));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/edit";
        }
    }

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

    /** ── Quản lý dịch vụ đi kèm (ExtraService) ────────────────── */

    @GetMapping("/facilities/{facilityId}/services")
    @Transactional(readOnly = true)
    public String extraServices(@PathVariable Long facilityId,
                                @AuthenticationPrincipal UserDetails ud,
                                Model model,
                                RedirectAttributes ra) {
        Facility facility = facilityService.findById(facilityId);
        if (!facility.getOwner().getEmail().equals(ud.getUsername())) {
            ra.addFlashAttribute("error", "Bạn không có quyền truy cập cơ sở này");
            return "redirect:/owner/facilities";
        }

        model.addAttribute("facility", facility);
        model.addAttribute("services", extraServiceService.findAllByFacility(facilityId, ud.getUsername()));
        model.addAttribute("newService", new ExtraServiceRequest());
        model.addAttribute("sportTypes", SportType.values());
        return "owner/services/manage";
    }

    @PostMapping("/facilities/{facilityId}/services/create")
    public String createExtraService(@PathVariable Long facilityId,
                                     @Valid @ModelAttribute("newService") ExtraServiceRequest req,
                                     BindingResult br,
                                     @AuthenticationPrincipal UserDetails ud,
                                     RedirectAttributes ra,
                                     Model model) {
        if (br.hasErrors()) {
            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("services", extraServiceService.findAllByFacility(facilityId, ud.getUsername()));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/services/manage";
        }

        try {
            extraServiceService.create(facilityId, req, ud.getUsername());
            ra.addFlashAttribute("success", "Đã thêm dịch vụ đi kèm");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/services";
    }

    @GetMapping("/facilities/{facilityId}/services/{serviceId}/edit")
    public String editExtraService(@PathVariable Long facilityId,
                                   @PathVariable Long serviceId,
                                   @AuthenticationPrincipal UserDetails ud,
                                   Model model,
                                   RedirectAttributes ra) {
        try {
            Facility facility = facilityService.findById(facilityId);
            if (!facility.getOwner().getEmail().equals(ud.getUsername())) {
                throw new ForbiddenException("Bạn không có quyền truy cập cơ sở này");
            }

            ExtraService service = extraServiceService.findById(serviceId);
            if (!service.getFacility().getId().equals(facilityId)) {
                throw new IllegalArgumentException("Dịch vụ không thuộc cơ sở này");
            }

            ExtraServiceRequest req = new ExtraServiceRequest();
            req.setName(service.getName());
            req.setDescription(service.getDescription());
            req.setPrice(service.getPrice());
            req.setUnit(service.getUnit());
            req.setStock(service.getStock());
            req.setIsActive(service.getIsActive());
            req.setAppliesToSportType(service.getAppliesToSportType());

            model.addAttribute("facility", facility);
            model.addAttribute("service", service);
            model.addAttribute("editService", req);
            model.addAttribute("sportTypes", SportType.values());
            return "owner/services/edit";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể mở trang chỉnh sửa dịch vụ. Vui lòng thử lại.");
            return "redirect:/owner/facilities/" + facilityId + "/services";
        }
    }

    @PostMapping("/facilities/{facilityId}/services/{serviceId}/edit")
    public String updateExtraService(@PathVariable Long facilityId,
                                     @PathVariable Long serviceId,
                                     @Valid @ModelAttribute("editService") ExtraServiceRequest req,
                                     BindingResult br,
                                     @AuthenticationPrincipal UserDetails ud,
                                     RedirectAttributes ra,
                                     Model model) {
        if (br.hasErrors()) {
            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("service", extraServiceService.findById(serviceId));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/services/edit";
        }

        try {
            extraServiceService.update(serviceId, facilityId, req, ud.getUsername());
            ra.addFlashAttribute("success", "Đã cập nhật dịch vụ");
            return "redirect:/owner/facilities/" + facilityId + "/services";
        } catch (Exception e) {
            model.addAttribute("error", "Không thể cập nhật dịch vụ lúc này. Vui lòng kiểm tra dữ liệu và thử lại.");
            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("service", extraServiceService.findById(serviceId));
            model.addAttribute("sportTypes", SportType.values());
            return "owner/services/edit";
        }
    }

    @PostMapping("/facilities/{facilityId}/services/{serviceId}/status")
    public String toggleExtraServiceStatus(@PathVariable Long facilityId,
                                           @PathVariable Long serviceId,
                                           @RequestParam boolean active,
                                           @AuthenticationPrincipal UserDetails ud,
                                           RedirectAttributes ra) {
        try {
            extraServiceService.toggleStatus(serviceId, facilityId, active, ud.getUsername());
            ra.addFlashAttribute("success", active ? "Đã bật dịch vụ" : "Đã tạm ẩn dịch vụ");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/services";
    }

    @PostMapping("/facilities/{facilityId}/services/{serviceId}/delete")
    public String deleteExtraService(@PathVariable Long facilityId,
                                     @PathVariable Long serviceId,
                                     @AuthenticationPrincipal UserDetails ud,
                                     RedirectAttributes ra) {
        try {
            extraServiceService.delete(serviceId, ud.getUsername());
            ra.addFlashAttribute("success", "Đã xóa dịch vụ đi kèm");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/facilities/" + facilityId + "/services";
    }

    /** ── Quản lý giá (PriceRule) ────────────────────────────────── */

    @GetMapping("/facilities/{facilityId}/fields/{fieldId}/price-rules")
    public String priceRules(@PathVariable Long facilityId,
                             @PathVariable Long fieldId,
                             @AuthenticationPrincipal UserDetails ud,
                             Model model, RedirectAttributes ra) {
        Facility facility = facilityService.findById(facilityId);
        if (facility.getStatus() == FacilityStatus.PENDING_APPROVAL || facility.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Bạn không thể quản lý giá khi cơ sở chưa được phê duyệt!");
            return "redirect:/owner/facilities";
        }
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
        Facility facility = facilityService.findById(facilityId);
        if (facility.getStatus() == FacilityStatus.PENDING_APPROVAL || facility.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("error", "Không được phép!");
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/price-rules";
        }

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

    @GetMapping("/facilities/{facilityId}/fields/{fieldId}/price-rules/{ruleId}/edit")
    public String editPriceRule(@PathVariable Long facilityId,
                                @PathVariable Long fieldId,
                                @PathVariable Long ruleId,
                                Model model) {
        Facility facility = facilityService.findById(facilityId);
        Field field = fieldService.findById(fieldId);
        PriceRule rule = priceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc giá"));

        if (!rule.getField().getId().equals(fieldId)) {
            throw new IllegalArgumentException("Quy tắc giá không thuộc sân này");
        }

        PriceRuleRequest req = new PriceRuleRequest();
        req.setName(rule.getName());
        req.setDayType(rule.getDayType());
        req.setStartTime(rule.getStartTime());
        req.setEndTime(rule.getEndTime());
        req.setPricePerHour(rule.getPricePerHour());
        req.setPriority(rule.getPriority());

        model.addAttribute("facility", facility);
        model.addAttribute("field", field);
        model.addAttribute("rule", rule);
        model.addAttribute("dayTypes", DayType.values());
        model.addAttribute("editRule", req);
        return "owner/price-rules/edit";
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/price-rules/{ruleId}/edit")
    public String updatePriceRule(@PathVariable Long facilityId,
                                  @PathVariable Long fieldId,
                                  @PathVariable Long ruleId,
                                  @Valid @ModelAttribute("editRule") PriceRuleRequest req,
                                  BindingResult br,
                                  @AuthenticationPrincipal UserDetails ud,
                                  RedirectAttributes ra,
                                  Model model) {
        if (br.hasErrors()) {
            PriceRule rule = priceRuleRepository.findById(ruleId).orElse(null);
            if (rule == null) {
                ra.addFlashAttribute("error", "Quy tắc giá không tồn tại hoặc đã bị xóa");
                return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/price-rules";
            }

            model.addAttribute("facility", facilityService.findById(facilityId));
            model.addAttribute("field", fieldService.findById(fieldId));
            model.addAttribute("rule", rule);
            model.addAttribute("dayTypes", DayType.values());
            return "owner/price-rules/edit";
        }

        try {
            priceRuleService.updateRule(ruleId, req, ud.getUsername());
            ra.addFlashAttribute("success", "Cập nhật quy tắc giá thành công");
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
                       @RequestParam(defaultValue = "false") boolean all,
                               @AuthenticationPrincipal UserDetails ud,
                               Model model, RedirectAttributes ra) {
        Facility facility = facilityService.findById(facilityId);
        if (facility.getStatus() == FacilityStatus.PENDING_APPROVAL || facility.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("errorMsg", "Cơ sở chưa sẵn sàng!");
            return "redirect:/owner/facilities";
        }

        if (date == null) date = LocalDate.now();
        Field field = fieldService.findById(fieldId);
        List<BlockedTime> blocks = all
            ? blockedRepo.findByFieldOrderByDateAscStartTimeAsc(field)
            : blockedRepo.findByFieldAndDateOrderByStartTime(field, date);
        BlockedTimeRequest newBlock = new BlockedTimeRequest();
        newBlock.setFieldId(fieldId);
        newBlock.setDate(date);

        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("field", field);
        model.addAttribute("blocks", blocks);
        model.addAttribute("selectedDate", date);
        model.addAttribute("viewingAll", all);
        model.addAttribute("newBlock", newBlock);
        return "owner/blocked-times/manage";
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/blocked-times/create")
    public String createBlockedTime(@PathVariable Long facilityId,
                                    @PathVariable Long fieldId,
                                    @Valid @ModelAttribute("newBlock") BlockedTimeRequest req,
                                    BindingResult br,
                                    @RequestParam(defaultValue = "false") boolean all,
                                    @AuthenticationPrincipal UserDetails ud,
                                    RedirectAttributes ra) {
        Facility facility = facilityService.findById(facilityId);
        if (facility.getStatus() == FacilityStatus.PENDING_APPROVAL || facility.getStatus() == FacilityStatus.BLOCKED) {
            ra.addFlashAttribute("error", "Không được phép!");
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times";
        }
        if (br.hasErrors()) {
            String message = br.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Dữ liệu không hợp lệ")
                .orElse("Dữ liệu không hợp lệ");
            ra.addFlashAttribute("error", message);

            LocalDate redirectDate = req.getDate() != null ? req.getDate() : LocalDate.now();
            if (all) {
                return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId
                        + "/blocked-times?all=true";
            }
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId
                + "/blocked-times?date=" + redirectDate;
        }
        try {
            if (!fieldRepo.existsByIdAndFacility_IdAndFacility_Owner_Email(fieldId, facilityId, ud.getUsername())) {
                throw new ForbiddenException("Không có quyền");
            }

            Field field = fieldService.findById(fieldId);
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
        if (all) {
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId
                    + "/blocked-times?all=true";
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId
                + "/blocked-times?date=" + req.getDate();
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/blocked-times/{id}/delete")
    public String deleteBlockedTime(@PathVariable Long facilityId,
                                    @PathVariable Long fieldId,
                                    @PathVariable Long id,
                                    @RequestParam(defaultValue = "false") boolean all,
                                    @AuthenticationPrincipal UserDetails ud,
                                    RedirectAttributes ra) {
        try {
            if (!blockedRepo.ownedBy(id, fieldId, facilityId, ud.getUsername())) {
                throw new ForbiddenException("Không có quyền");
            }
            blockedRepo.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa khung giờ bị chặn");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        if (all) {
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times?all=true";
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times";
    }

    @GetMapping("/facilities/{facilityId}/fields/{fieldId}/blocked-times/{id}/edit")
    public String editBlockedTime(@PathVariable Long facilityId,
                                  @PathVariable Long fieldId,
                                  @PathVariable Long id,
                                  @RequestParam(defaultValue = "false") boolean all,
                                  @AuthenticationPrincipal UserDetails ud,
                                  RedirectAttributes ra,
                                  Model model) {
        try {
            BlockedTime blockedTime = blockedRepo.findOwnedForEdit(id, fieldId, facilityId, ud.getUsername())
                    .orElseThrow(() -> new ForbiddenException("Không có quyền"));

            BlockedTimeRequest editBlock = new BlockedTimeRequest();
            editBlock.setFieldId(fieldId);
            editBlock.setDate(blockedTime.getDate());
            editBlock.setStartTime(blockedTime.getStartTime());
            editBlock.setEndTime(blockedTime.getEndTime());
            editBlock.setReason(blockedTime.getReason());

            model.addAttribute("facility", blockedTime.getField().getFacility());
            model.addAttribute("field", blockedTime.getField());
            model.addAttribute("blocked", blockedTime);
            model.addAttribute("viewingAll", all);
            model.addAttribute("editBlock", editBlock);
            return "owner/blocked-times/edit";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (all) {
                return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times?all=true";
            }
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times";
        }
    }

    @PostMapping("/facilities/{facilityId}/fields/{fieldId}/blocked-times/{id}/edit")
    public String updateBlockedTime(@PathVariable Long facilityId,
                                    @PathVariable Long fieldId,
                                    @PathVariable Long id,
                                    @Valid @ModelAttribute("editBlock") BlockedTimeRequest req,
                                    BindingResult br,
                                    @RequestParam(defaultValue = "false") boolean all,
                                    @AuthenticationPrincipal UserDetails ud,
                                    RedirectAttributes ra,
                                    Model model) {
        BlockedTime blockedTime = blockedRepo.findOwnedForEdit(id, fieldId, facilityId, ud.getUsername())
                .orElseThrow(() -> new ForbiddenException("Không có quyền"));

        if (br.hasErrors()) {
            model.addAttribute("facility", blockedTime.getField().getFacility());
            model.addAttribute("field", blockedTime.getField());
            model.addAttribute("blocked", blockedTime);
            model.addAttribute("viewingAll", all);
            return "owner/blocked-times/edit";
        }

        try {
            if (req.getStartTime() != null && req.getEndTime() != null
                    && !req.getStartTime().isBefore(req.getEndTime())) {
                throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
            }
            if (bookingRepo.existsConflict(blockedTime.getField(), req.getDate(), req.getStartTime(), req.getEndTime())) {
                throw new IllegalStateException("Khung giờ này đã có người đặt, không thể chặn");
            }
            if (blockedRepo.existsConflictExcludingId(blockedTime.getField(), req.getDate(), req.getStartTime(), req.getEndTime(), id)) {
                throw new IllegalStateException("Khung giờ này đã bị chặn bởi quy tắc khác");
            }

            blockedTime.setDate(req.getDate());
            blockedTime.setStartTime(req.getStartTime());
            blockedTime.setEndTime(req.getEndTime());
            blockedTime.setReason(req.getReason());
            blockedRepo.save(blockedTime);
            ra.addFlashAttribute("success", "Đã cập nhật khung giờ bị chặn");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId
                    + "/blocked-times/" + id + "/edit" + (all ? "?all=true" : "");
        }

        if (all) {
            return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times?all=true";
        }
        return "redirect:/owner/facilities/" + facilityId + "/fields/" + fieldId + "/blocked-times?date=" + req.getDate();
    }

    /** ── Quản lý booking (Owner xem danh sách) ──────────────────── */

    /** ── Quản lý booking (Owner) ──────────────────── */

    @GetMapping("/bookings")
    public String ownerBookings(@RequestParam(required = false)
                                @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                @RequestParam(required = false) String status, // Thêm filter theo trạng thái
                                @AuthenticationPrincipal UserDetails ud,
                                Model model) {
        List<BookingResponse> bookings = bookingService.getBookingsByOwner(ud.getUsername());
        
        // Lọc theo ngày nếu có
        if (date != null) {
            bookings = bookings.stream()
                .filter(b -> b.getBookingDate().isEqual(date))
                .toList();
        }
        
        // Lọc theo trạng thái (PENDING, CONFIRMED, CANCELLED) nếu có
        if (status != null && !status.isEmpty()) {
            bookings = bookings.stream()
                .filter(b -> b.getStatus().name().equalsIgnoreCase(status))
                .toList();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedStatus", status);
        return "owner/bookings/list";
    }

    // Nút Duyệt Đơn (Từ PENDING -> CONFIRMED)
    @PostMapping("/bookings/{id}/confirm")
    public String ownerConfirmBooking(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails ud,
                                      RedirectAttributes ra) {
        try {
            bookingService.confirmBooking(id, ud.getUsername());
            ra.addFlashAttribute("success", "Duyệt đơn đặt sân thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/bookings";
    }

    // Nút Từ chối / Hủy Đơn (Chuyển sang CANCELLED)
    @PostMapping("/bookings/{id}/cancel")
    public String ownerCancelBooking(@PathVariable Long id,
                                     @RequestParam(required = false, defaultValue = "Chủ sân từ chối/hủy đơn") String reason,
                                     @AuthenticationPrincipal UserDetails ud,
                                     RedirectAttributes ra) {
        try {
            bookingService.cancelBooking(id, ud.getUsername(), reason);
            ra.addFlashAttribute("success", "Hủy đơn đặt sân thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/bookings";
    }
    @PostMapping("/bookings/{id}/approve-cancel")
    public String approveCancel(@PathVariable Long id, 
                                @RequestParam boolean approve,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra) {
        try {
            bookingService.approveCancelRequest(id, ud.getUsername(), approve);
            String msg = approve ? "Đã chấp nhận yêu cầu hủy đơn." : "Đã từ chối yêu cầu hủy.";
            ra.addFlashAttribute("success", msg);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/owner/bookings";
    }

    @GetMapping("")
    public String ownerDashboard(Model model, Authentication authentication) {
        String email = authentication.getName();
        User currentOwner = userService.findByEmail(email);

        List<Facility> facilities = facilityService.findByOwner(email);
        List<Booking> bookings = bookingRepo.findByOwnerEmail(email);
        LocalDate today = LocalDate.now();

        long totalFacilities = facilities.size();
        long pendingFacilities = countFacilitiesByStatus(facilities, FacilityStatus.PENDING_APPROVAL);
        long blockedFacilities = countFacilitiesByStatus(facilities, FacilityStatus.BLOCKED);
        long openFacilities = countFacilitiesByStatus(facilities, FacilityStatus.OPEN);
        long totalFields = fieldRepo.countByFacility_Owner_Email(email);
        long openFields = fieldRepo.countByFacility_Owner_EmailAndStatus(email, J2EE.SportBooingSystem.enums.FieldStatus.OPEN);

        long totalBookings = bookings.size();
        long pendingBookings = countBookingsByStatus(bookings, BookingStatus.PENDING);
        long cancelPendingBookings = countBookingsByStatus(bookings, BookingStatus.CANCEL_PENDING);
        long confirmedBookings = countBookingsByStatus(bookings, BookingStatus.CONFIRMED);
        long completedBookings = countBookingsByStatus(bookings, BookingStatus.COMPLETED);
        long todayBookings = bookings.stream()
            .filter(booking -> booking.getBookingDate().isEqual(today))
            .count();

        BigDecimal monthlyRevenue = bookings.stream()
            .filter(booking -> booking.getBookingDate().getYear() == today.getYear())
            .filter(booking -> booking.getBookingDate().getMonth() == today.getMonth())
            .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED)
            .map(Booking::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BookingResponse> recentBookings = bookings.stream()
            .filter(booking -> booking.getStatus() == BookingStatus.PENDING)
            .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(5)
            .map(BookingResponse::from)
            .toList();

        List<Facility> attentionFacilities = facilities.stream()
            .filter(facility -> facility.getStatus() == FacilityStatus.PENDING_APPROVAL || facility.getStatus() == FacilityStatus.BLOCKED)
            .sorted(Comparator.comparing(Facility::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(6)
            .toList();

        Map<Long, Long> openFieldCounts = facilities.stream()
            .collect(LinkedHashMap::new,
                (map, facility) -> map.put(facility.getId(), fieldRepo.countByFacility_IdAndStatus(facility.getId(), J2EE.SportBooingSystem.enums.FieldStatus.OPEN)),
                Map::putAll);

        model.addAttribute("sportTypes", SportType.values());
        model.addAttribute("currentOwner", currentOwner);
        model.addAttribute("totalFacilities", totalFacilities);
        model.addAttribute("pendingFacilities", pendingFacilities);
        model.addAttribute("blockedFacilities", blockedFacilities);
        model.addAttribute("openFacilities", openFacilities);
        model.addAttribute("totalFields", totalFields);
        model.addAttribute("openFields", openFields);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("pendingBookings", pendingBookings);
        model.addAttribute("cancelPendingBookings", cancelPendingBookings);
        model.addAttribute("confirmedBookings", confirmedBookings);
        model.addAttribute("completedBookings", completedBookings);
        model.addAttribute("todayBookings", todayBookings);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("attentionFacilities", attentionFacilities);
        model.addAttribute("openFieldCounts", openFieldCounts);
        return "owner/index";
    }

    private long countFacilitiesByStatus(List<Facility> facilities, FacilityStatus status) {
    return facilities.stream()
        .filter(facility -> facility.getStatus() == status)
        .count();
    }

    private long countBookingsByStatus(List<Booking> bookings, BookingStatus status) {
    return bookings.stream()
        .filter(booking -> booking.getStatus() == status)
        .count();
    }

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public String notificationsPage() {
        return "notifications/list";
    }

}