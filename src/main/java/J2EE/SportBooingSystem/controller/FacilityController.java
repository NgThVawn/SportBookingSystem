package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.repository.FavoriteRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.ExtraServiceService;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.FieldService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;
    private final FieldService fieldService;

    private final ExtraServiceService extraServiceService;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) SportType sport,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Page<Facility> facilities = facilityService.search(city, sport, name,
            PageRequest.of(page, 9));

        Set<Long> favIds = Collections.emptySet();
        if (userDetails != null) {
            favIds = userRepository.findByEmail(userDetails.getUsername())
                .map(u -> favoriteRepository.findByUser(u).stream()
                    .map(f -> f.getFacility().getId())
                    .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
        }

        model.addAttribute("facilities", facilities);
        model.addAttribute("sportTypes", SportType.values());
        model.addAttribute("selectedCity", city);
        model.addAttribute("selectedSport", sport);
        model.addAttribute("searchName", name);
        model.addAttribute("favoriteFacilityIds", favIds);
        return "facilities/list";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) String date,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        Facility facility = facilityService.findById(id);
        model.addAttribute("facility", facility);
        model.addAttribute("fields", fieldService.findByFacility(id));

        model.addAttribute("services", extraServiceService.findByFacility(id));

        LocalDate selectedDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedFieldId", fieldId);
        model.addAttribute("today", LocalDate.now());

        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("isFavorite",
                    favoriteRepository.existsByUserAndFacility(user, facility));
            });
        }
        return "facilities/detail";
    }
}
