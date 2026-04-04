package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.RegisterRequest;
import J2EE.SportBooingSystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest; // Thêm thư viện này
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        request.getSession(true); 
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {
        request.getSession(true); 
        
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterRequest request,
            BindingResult result,
            RedirectAttributes ra,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("registerRequest", request);
            return "auth/register";
        }

        try {
            userService.register(request);
            ra.addFlashAttribute("successMsg",
                "Registration successful! Please log in.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("registerRequest", request);
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }
    }
}