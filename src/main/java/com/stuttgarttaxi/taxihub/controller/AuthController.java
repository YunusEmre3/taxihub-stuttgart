package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.ForgotPasswordForm;
import com.stuttgarttaxi.taxihub.dto.RegisterForm;
import com.stuttgarttaxi.taxihub.dto.ResetPasswordForm;
import com.stuttgarttaxi.taxihub.dto.VerifyEmailForm;
import com.stuttgarttaxi.taxihub.exception.EmailAlreadyRegisteredException;
import com.stuttgarttaxi.taxihub.exception.EmailDomainNotAllowedException;
import com.stuttgarttaxi.taxihub.exception.InvalidResetTokenException;
import com.stuttgarttaxi.taxihub.exception.InvalidVerificationCodeException;
import com.stuttgarttaxi.taxihub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================== LOGIN ====================

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "logout", required = false) String logout,
                             @RequestParam(value = "email", required = false) String email,
                             Model model) {
        if ("unverified".equals(error)) {
            model.addAttribute("errorMessageKey", "auth.error.unverified");
            model.addAttribute("unverifiedEmail", email);
        } else if (error != null) {
            model.addAttribute("errorMessageKey", "auth.error.invalidCredentials");
        }
        if (logout != null) {
            model.addAttribute("infoMessageKey", "auth.info.loggedOut");
        }
        return "login";
    }

    // ==================== REGISTER ====================

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        model.addAttribute("emailDomain", authService.companyEmailDomain());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                            BindingResult bindingResult,
                            Model model) {
        model.addAttribute("emailDomain", authService.companyEmailDomain());

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Şifreler eşleşmiyor");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            authService.register(form);
        } catch (EmailDomainNotAllowedException | EmailAlreadyRegisteredException ex) {
            bindingResult.rejectValue("email", "email.invalid", ex.getMessage());
            return "register";
        }

        return "redirect:/verify-email?email=" + form.getEmail().trim().toLowerCase();
    }

    // ==================== VERIFY EMAIL ====================

    @GetMapping("/verify-email")
    public String verifyEmailPage(@RequestParam("email") String email, Model model) {
        VerifyEmailForm form = new VerifyEmailForm();
        form.setEmail(email);
        model.addAttribute("verifyEmailForm", form);
        model.addAttribute("codeValidityMinutes", authService.verificationCodeValidityMinutes());
        return "verify-email";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(@Valid @ModelAttribute("verifyEmailForm") VerifyEmailForm form,
                               BindingResult bindingResult,
                               Model model) {
        model.addAttribute("codeValidityMinutes", authService.verificationCodeValidityMinutes());

        if (bindingResult.hasErrors()) {
            return "verify-email";
        }

        try {
            authService.verifyEmail(form.getEmail(), form.getCode());
        } catch (InvalidVerificationCodeException ex) {
            bindingResult.rejectValue("code", "code.invalid", ex.getMessage());
            return "verify-email";
        }

        return "redirect:/login?verified=true";
    }

    @PostMapping("/verify-email/resend")
    public String resendVerificationCode(@RequestParam("email") String email, Model model) {
        try {
            authService.resendVerificationCode(email);
        } catch (InvalidVerificationCodeException ignored) {
            // E-posta bulunamasa bile aynı nötr mesaj gösterilir (kullanıcı varlığı ifşa edilmez).
        }

        VerifyEmailForm form = new VerifyEmailForm();
        form.setEmail(email);
        model.addAttribute("verifyEmailForm", form);
        model.addAttribute("codeValidityMinutes", authService.verificationCodeValidityMinutes());
        model.addAttribute("successMessageKey", "auth.success.codeResent");
        return "verify-email";
    }

    // ==================== FORGOT PASSWORD ====================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                                  BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "forgot-password";
        }

        try {
            authService.initiatePasswordReset(form.getEmail());
        } catch (EmailDomainNotAllowedException ex) {
            bindingResult.rejectValue("email", "email.invalid", ex.getMessage());
            return "forgot-password";
        }

        model.addAttribute("successMessageKey", "auth.success.resetLinkSent");
        return "forgot-password";
    }

    // ==================== RESET PASSWORD ====================

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        try {
            authService.validateResetToken(token);
        } catch (InvalidResetTokenException ex) {
            model.addAttribute("invalidToken", true);
            model.addAttribute("errorMessage", ex.getMessage());
            return "reset-password";
        }

        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                 BindingResult bindingResult,
                                 Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Şifreler eşleşmiyor");
        }

        if (bindingResult.hasErrors()) {
            return "reset-password";
        }

        try {
            authService.resetPassword(form.getToken(), form.getPassword());
        } catch (InvalidResetTokenException ex) {
            model.addAttribute("invalidToken", true);
            model.addAttribute("errorMessage", ex.getMessage());
            return "reset-password";
        }

        return "redirect:/login?resetSuccess=true";
    }
}
