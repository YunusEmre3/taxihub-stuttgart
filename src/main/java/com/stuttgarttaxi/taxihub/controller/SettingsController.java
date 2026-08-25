package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.PricingRuleForm;
import com.stuttgarttaxi.taxihub.dto.PricingUpdateForm;
import com.stuttgarttaxi.taxihub.dto.ProfileForm;
import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.PricingRule;
import com.stuttgarttaxi.taxihub.service.AuthService;
import com.stuttgarttaxi.taxihub.service.EmployeeUserDetails;
import com.stuttgarttaxi.taxihub.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final PricingService pricingService;
    private final AuthService authService;

    @Value("${ors.api-key}")
    private String orsApiKey;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @GetMapping("/settings")
    public String settings(Model model, @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        addSharedModelAttributes(model, userDetails);
        return "settings/dashboard";
    }

    @PostMapping("/settings/pricing")
    public String updatePricing(@Valid @ModelAttribute("pricingUpdateForm") PricingUpdateForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            addSharedModelAttributes(model, userDetails);
            model.addAttribute("pricingUpdateForm", form);
            return "settings/dashboard";
        }

        pricingService.updateRules(form.getRules());
        redirectAttributes.addFlashAttribute("successMessageKey", "settings.success.pricingUpdated");
        return "redirect:/settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            addSharedModelAttributes(model, userDetails);
            model.addAttribute("profileForm", form);
            return "settings/dashboard";
        }

        Employee updated = authService.updateOwnProfile(
                userDetails.getEmployee().getId(), form.getFirstName(), form.getLastName());

        // The session's cached principal still holds the old name until we
        // swap it - without this, the topbar keeps showing the old name
        // until the admin logs out and back in.
        EmployeeUserDetails refreshedPrincipal = new EmployeeUserDetails(updated);
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        Authentication refreshedAuth = new UsernamePasswordAuthenticationToken(
                refreshedPrincipal, currentAuth.getCredentials(), refreshedPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(refreshedAuth);

        redirectAttributes.addFlashAttribute("successMessageKey", "settings.success.profileUpdated");
        return "redirect:/settings";
    }

    private void addSharedModelAttributes(Model model, EmployeeUserDetails userDetails) {
        Employee employee = userDetails.getEmployee();

        model.addAttribute("employeeName", employee.getFirstName());

        PricingUpdateForm pricingForm = new PricingUpdateForm();
        for (PricingRule rule : pricingService.getAllRules()) {
            PricingRuleForm ruleForm = new PricingRuleForm();
            ruleForm.setVehicleType(rule.getVehicleType());
            ruleForm.setBaseFare(rule.getBaseFare());
            ruleForm.setPricePerKm(rule.getPricePerKm());
            pricingForm.getRules().add(ruleForm);
        }
        model.addAttribute("pricingUpdateForm", pricingForm);

        ProfileForm profileForm = new ProfileForm();
        profileForm.setFirstName(employee.getFirstName());
        profileForm.setLastName(employee.getLastName());
        model.addAttribute("profileForm", profileForm);

        model.addAttribute("companyEmailDomain", authService.companyEmailDomain());
        model.addAttribute("appBaseUrl", appBaseUrl);
        model.addAttribute("orsKeyConfigured", orsApiKey != null && !orsApiKey.isBlank());
        model.addAttribute("orsKeyMasked", maskKey(orsApiKey));
        model.addAttribute("resendKeyConfigured", resendApiKey != null && !resendApiKey.isBlank());
        model.addAttribute("resendKeyMasked", maskKey(resendApiKey));
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "(tanımlı değil)";
        }
        if (key.length() <= 16) {
            return "*".repeat(key.length());
        }
        return key.substring(0, 8) + "..." + key.substring(key.length() - 6);
    }
}
