package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.*;
import J2EE.SportBooingSystem.entity.*;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@RequiredArgsConstructor
public class OwnerController {

    private final FacilityService facilityService;
    private final FieldService fieldService;

    // ─── FACILITY ─────────────────────────

    // LIST
    @GetMapping("/facilities")
    public String facilities(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("facilities", facilityService.findByOwner(ud.getUsername()));
        return "owner/facilities/list";
    }

    // CREATE 
    @GetMapping("/facilities/create")
    public String createFacilityPage(Model model) {
        model.addAttribute("facilityRequest", new FacilityRequest());
        return "owner/facilities/create";
    }

    // CREATE
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
            ra.addFlashAttribute("successMsg", "Created!");
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
    // UPDATE
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
            ra.addFlashAttribute("successMsg", "Updated!");
            return "redirect:/owner/facilities";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "owner/facilities/edit";
        }
    }

    // DELETE
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

    // TOGGLE
    @PostMapping("/facilities/{id}/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleFacility(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try {

            facilityService.toggleActive(id, ud.getUsername());
            
            Facility f = facilityService.findById(id);
            
            return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "isActive", f.getIsActive()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", e.getMessage()
            ));
        }
    }
    // ─── FIELD ─────────────────────────

    // LIST
    @GetMapping("/facilities/{facilityId}/fields")
    public String fields(@PathVariable Long facilityId, Model model) {
        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("fields", fieldService.findByFacility(facilityId));
        return "owner/fields/list";
    }

    // CREATE 
    @GetMapping("/facilities/{facilityId}/fields/create")
    public String createFieldPage(@PathVariable Long facilityId, Model model) {
        model.addAttribute("facility", facilityService.findById(facilityId));
        model.addAttribute("fieldRequest", new FieldRequest());
        model.addAttribute("sportTypes", SportType.values());
        return "owner/fields/create";
    }

    // CREATE
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
            ra.addFlashAttribute("successMsg", "Created!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "owner/fields/create";
        }
    }

    // EDIT 
    @GetMapping("/fields/{id}/edit")
    public String editField(@PathVariable Long id, Model model) {
        Field f = fieldService.findById(id);

        FieldRequest req = new FieldRequest();
        req.setName(f.getName());
        req.setSportType(f.getSportType());
        req.setDescription(f.getDescription());
        req.setSurfaceType(f.getSurfaceType());
        req.setCapacity(f.getCapacity());
        req.setPricePerHour(f.getPricePerHour());
        req.setPricePerSlot(f.getPricePerSlot());
        req.setSlotDuration(f.getSlotDuration());

        model.addAttribute("field", f);
        model.addAttribute("fieldRequest", req);
        model.addAttribute("sportTypes", SportType.values());

        return "owner/fields/edit";
    }

    // UPDATE
    @PostMapping("/fields/{id}/edit")
    public String updateField(
            @PathVariable Long id,
            @Valid @ModelAttribute FieldRequest request,
            BindingResult result,
            @AuthenticationPrincipal UserDetails ud,
            Model model,
            RedirectAttributes ra) {

        Field f = fieldService.findById(id);
        Long facilityId = f.getFacility().getId();

        if (result.hasErrors()) {
            model.addAttribute("field", f);
            model.addAttribute("sportTypes", SportType.values());
            return "owner/fields/edit";
        }

        try {
            fieldService.update(id, request, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Updated!");
            return "redirect:/owner/facilities/" + facilityId + "/fields";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "owner/fields/edit";
        }
    }
}