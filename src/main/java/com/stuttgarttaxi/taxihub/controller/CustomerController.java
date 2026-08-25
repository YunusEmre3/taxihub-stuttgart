package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.CustomerForm;
import com.stuttgarttaxi.taxihub.dto.CustomerRow;
import com.stuttgarttaxi.taxihub.entity.AccountType;
import com.stuttgarttaxi.taxihub.exception.CustomerEmailAlreadyExistsException;
import com.stuttgarttaxi.taxihub.exception.CustomerNotFoundException;
import com.stuttgarttaxi.taxihub.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/customers")
    public String list(Model model) {
        model.addAttribute("customers", customerService.getAllCustomersWithStats());
        model.addAttribute("accountTypes", AccountType.values());
        if (!model.containsAttribute("customerForm")) {
            model.addAttribute("customerForm", new CustomerForm());
        }
        return "customers/list";
    }

    @PostMapping("/customers")
    public String create(@Valid @ModelAttribute("customerForm") CustomerForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.getAllCustomersWithStats());
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("openAddModal", true);
            return "customers/list";
        }

        try {
            customerService.createCustomer(form);
        } catch (CustomerEmailAlreadyExistsException ex) {
            bindingResult.rejectValue("email", "email.exists", ex.getMessage());
            model.addAttribute("customers", customerService.getAllCustomersWithStats());
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("openAddModal", true);
            return "customers/list";
        }

        redirectAttributes.addFlashAttribute("successMessageKey", "customers.success.created");
        return "redirect:/customers";
    }

    @GetMapping("/customers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        CustomerRow customer;
        try {
            customer = customerService.getCustomerRowById(id);
        } catch (CustomerNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customers";
        }

        if (!model.containsAttribute("customerForm")) {
            CustomerForm form = new CustomerForm();
            form.setFirstName(customer.firstName());
            form.setLastName(customer.lastName());
            form.setEmail(customer.email());
            form.setPhoneNumber(customer.phoneNumber());
            form.setAccountType(customer.accountType());
            model.addAttribute("customerForm", form);
        }

        model.addAttribute("customer", customer);
        model.addAttribute("accountTypes", AccountType.values());
        return "customers/edit";
    }

    @PostMapping("/customers/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("customerForm") CustomerForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", customerService.getCustomerRowById(id));
            model.addAttribute("accountTypes", AccountType.values());
            return "customers/edit";
        }

        customerService.updateCustomer(id, form);
        redirectAttributes.addFlashAttribute("successMessageKey", "customers.success.updated");
        return "redirect:/customers";
    }
}
