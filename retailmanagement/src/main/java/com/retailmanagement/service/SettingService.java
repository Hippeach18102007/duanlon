package com.retailmanagement.service;

import com.retailmanagement.entity.SystemSetting;
import com.retailmanagement.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SettingService {

    private final SystemSettingRepository settingRepo;

    public SettingService(SystemSettingRepository settingRepo) {
        this.settingRepo = settingRepo;
    }

    /**
     * Get default currency from system settings
     * Falls back to "VND" if not set
     */
    public String getDefaultCurrency() {
        return settingRepo.findById("DEFAULT_CURRENCY")
                .map(SystemSetting::getValue)
                .orElse("VND");
    }

    /**
     * Set default currency
     */
    public void setDefaultCurrency(String currencyCode) {
        SystemSetting setting = settingRepo.findById("DEFAULT_CURRENCY")
                .orElse(new SystemSetting());
        setting.setKey("DEFAULT_CURRENCY");
        setting.setValue(currencyCode.toUpperCase());
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepo.save(setting);
    }

    /**
     * Get any setting by key
     */
    public String getSetting(String key, String defaultValue) {
        return settingRepo.findById(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    /**
     * Set any setting
     */
    public void setSetting(String key, String value) {
        SystemSetting setting = settingRepo.findById(key)
                .orElse(new SystemSetting());
        setting.setKey(key);
        setting.setValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepo.save(setting);
    }

    public Map<String, String> getAllSettings() {
        Map<String, String> map = new java.util.HashMap<>();
        settingRepo.findAll().forEach(s -> map.put(s.getKey(), s.getValue()));
        return map;
    }
}
