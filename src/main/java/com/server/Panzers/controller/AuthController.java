package com.server.Panzers.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.server.Panzers.model.GameStatistics;
import com.server.Panzers.model.User;
import com.server.Panzers.service.StatisticsService;
import com.server.Panzers.service.UserService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private static final String ERROR_ATTRIBUTE = "error";
    private static final String AUTH_REGISTER_VIEW = "auth/register";

    private final UserService userService;
    private final StatisticsService statisticsService;

    public AuthController(UserService userService, StatisticsService statisticsService) {
        this.userService = userService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return AUTH_REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return AUTH_REGISTER_VIEW;
        }

        try {
            // Проверяем, существует ли пользователь с таким именем или email
            if (userService.existsByUsername(user.getUsername())) {
                model.addAttribute(ERROR_ATTRIBUTE, "Пользователь с таким именем уже существует");
                return AUTH_REGISTER_VIEW;
            }

            if (userService.existsByEmail(user.getEmail())) {
                model.addAttribute(ERROR_ATTRIBUTE, "Пользователь с таким email уже существует");
                return AUTH_REGISTER_VIEW;
            }

            // Сохраняем пользователя
            User savedUser = userService.save(user);

            // Создаем начальную статистику для пользователя
            GameStatistics initialStats = new GameStatistics(savedUser);
            statisticsService.saveStatistics(initialStats);

            redirectAttributes.addFlashAttribute("success", "Регистрация прошла успешно! Теперь вы можете войти в систему.");
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute(ERROR_ATTRIBUTE, "Произошла ошибка при регистрации: " + e.getMessage());
            return AUTH_REGISTER_VIEW;
        }
    }
}
