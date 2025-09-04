package com.alloc64.heating.controller;

import com.alloc64.heating.model.EnableByTimeRequest;
import com.alloc64.heating.model.EnableOnTemperatureChangeRequest;
import com.alloc64.heating.model.ManualOverrideRequest;
import com.alloc64.heating.model.State;
import com.alloc64.heating.service.HeatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class HeatingController {
    private final HeatingService heatingService;

    @GetMapping("/")
    public String index(Model model) throws Exception {
        model.addAttribute("state", heatingService.getState());
        return "index";
    }

    @PostMapping("/manual-override/{pumpId}")
    public String manualOverride(@PathVariable("pumpId") String pumpId,
                                 @ModelAttribute ManualOverrideRequest request,
                                 Model model) {

        try {
            heatingService.manualOverride(pumpId, request.isEnabled());
            return "redirect:/";
        }
        catch (Exception e) {
            logException(e, model);
            return "index";
        }
    }

    @PostMapping("/temperature-trigger/{pumpId}")
    public String temperatureTigger(@PathVariable("pumpId") String pumpId,
                              @ModelAttribute EnableOnTemperatureChangeRequest request,
                              Model model) {

        try {
            heatingService.setTempTrigger(pumpId, request.getTriggerTemp());

            return "redirect:/";
        }
        catch (Exception e) {
            logException(e, model);
            return "index";
        }
    }

    @PostMapping("/time-trigger/{pumpId}")
    public String timeTrigger(@PathVariable("pumpId") String pumpId,
                              @ModelAttribute EnableByTimeRequest request,
                              Model model) {

        try {
            heatingService.setTimeTrigger(pumpId, request.getTimeRange(), request.getMinTemp());
            return "redirect:/";
        }
        catch (Exception e) {
            logException(e, model);
            return "index";
        }
    }

    private void logException(Exception e, Model model) {
        State state = heatingService.getState();
        model.addAttribute("state", state);
        state.setMessage(e.getMessage());
    }
}
