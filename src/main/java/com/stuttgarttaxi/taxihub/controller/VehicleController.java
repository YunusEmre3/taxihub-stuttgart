package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.VehicleForm;
import com.stuttgarttaxi.taxihub.dto.VehicleRow;
import com.stuttgarttaxi.taxihub.entity.VehicleStatus;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.exception.VehicleConflictException;
import com.stuttgarttaxi.taxihub.exception.VehicleNotFoundException;
import com.stuttgarttaxi.taxihub.service.VehicleService;
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
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping("/vehicles")
    public String list(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("kpis", vehicleService.getVehicleKpis());
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("availableDrivers", vehicleService.getDriversWithoutVehicle(null));
        if (!model.containsAttribute("vehicleForm")) {
            model.addAttribute("vehicleForm", new VehicleForm());
        }
        return "vehicles/list";
    }

    @PostMapping("/vehicles")
    public String create(@Valid @ModelAttribute("vehicleForm") VehicleForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return reRenderList(model, true);
        }

        try {
            vehicleService.createVehicle(form);
        } catch (VehicleConflictException ex) {
            bindingResult.reject("vehicle.conflict", ex.getMessage());
            return reRenderList(model, true);
        }

        redirectAttributes.addFlashAttribute("successMessageKey", "vehicles.success.created");
        return "redirect:/vehicles";
    }

    @GetMapping("/vehicles/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        VehicleRow vehicle;
        try {
            vehicle = vehicleService.getVehicleRowById(id);
        } catch (VehicleNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/vehicles";
        }

        if (!model.containsAttribute("vehicleForm")) {
            VehicleForm form = new VehicleForm();
            form.setPlateNumber(vehicle.plateNumber());
            form.setModel(vehicle.model());
            form.setYear(vehicle.year());
            form.setVehicleType(vehicle.vehicleType());
            form.setVin(vehicle.vin());
            form.setAssignedDriverId(vehicle.assignedDriverId());
            model.addAttribute("vehicleForm", form);
        }

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("availableDrivers", vehicleService.getDriversWithoutVehicle(id));
        return "vehicles/edit";
    }

    @PostMapping("/vehicles/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("vehicleForm") VehicleForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return reRenderEdit(id, model);
        }

        try {
            vehicleService.updateVehicle(id, form);
        } catch (VehicleConflictException ex) {
            bindingResult.reject("vehicle.conflict", ex.getMessage());
            return reRenderEdit(id, model);
        }

        redirectAttributes.addFlashAttribute("successMessageKey", "vehicles.success.updated");
        return "redirect:/vehicles";
    }

    private String reRenderList(Model model, boolean openAddModal) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("kpis", vehicleService.getVehicleKpis());
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("availableDrivers", vehicleService.getDriversWithoutVehicle(null));
        model.addAttribute("openAddModal", openAddModal);
        return "vehicles/list";
    }

    private String reRenderEdit(Long id, Model model) {
        model.addAttribute("vehicle", vehicleService.getVehicleRowById(id));
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("availableDrivers", vehicleService.getDriversWithoutVehicle(id));
        return "vehicles/edit";
    }
}
