package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.repository.FavoriteRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.ExtraServiceService;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.FieldService;

import J2EE.SportBooingSystem.service.ReviewService;
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
    private final ReviewService reviewService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) SportType sport,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean favoritesOnly,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Set<Long> favIds = Collections.emptySet();
        Long userId = null; // Tạo sẵn biến userId

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null) {
                userId = user.getId(); // Lấy ID của ông đang đăng nhập
                favIds = favoriteRepository.findByUser(user).stream()
                        .map(f -> f.getFacility().getId())
                        .collect(Collectors.toSet());
            }
        }

        // Gọi hàm search mới độ lại
        Page<Facility> facilities = facilityService.search(city, sport, name, favoritesOnly, userId, PageRequest.of(page, 9));

        model.addAttribute("facilities", facilities);
        model.addAttribute("sportTypes", SportType.values());
        model.addAttribute("selectedCity", city);
        model.addAttribute("selectedSport", sport);
        model.addAttribute("searchName", name);
        model.addAttribute("favoritesOnly", favoritesOnly);
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
        // Truyền danh sách đánh giá xuống View
        model.addAttribute("reviews", reviewService.getReviewsByFacility(id));

        // Khởi tạo một ReviewRequest rỗng để bind với Form đánh giá
        model.addAttribute("reviewForm", new J2EE.SportBooingSystem.dto.request.ReviewRequest());

        // Kiểm tra xem User đã thả tim sân này chưa
        boolean isFavorite = false;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null) {
                isFavorite = favoriteRepository.existsByUserAndFacility(user, facility);
            }
        }
        model.addAttribute("isFavorite", isFavorite);

        return "facilities/detail";
    }

}