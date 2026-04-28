package com.retailmanagement.controller;

import com.retailmanagement.dto.request.SetDefaultCurrencyRequest;
import com.retailmanagement.dto.response.ApiResponse;
import com.retailmanagement.service.SettingService;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/currency/default")
    public ApiResponse<String> getDefaultCurrency() {
        return ApiResponse.success(settingService.getDefaultCurrency());
    }

    @PutMapping("/currency/default")
    public ApiResponse<String> setDefaultCurrency(@RequestBody SetDefaultCurrencyRequest req) {
        settingService.setDefaultCurrency(req.getCurrencyCode());
        return ApiResponse.success("OK");
    }

    @GetMapping
    public ApiResponse<Map<String, String>> getAllSettings() {
        return ApiResponse.success(settingService.getAllSettings());
    }
}
