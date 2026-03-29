package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final FacilityService facilityService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("facilities",
            facilityService.search(null, null, null, PageRequest.of(0, 6)));
        model.addAttribute("sportTypes", SportType.values());
        return "index";
    }
}
