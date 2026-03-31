package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final FacilityService facilityService;
    private final UserService userService;


    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("facilities",
            facilityService.search(null, null, null, PageRequest.of(0, 6)));
        model.addAttribute("sportTypes", SportType.values());
        return "index";
    }


}
