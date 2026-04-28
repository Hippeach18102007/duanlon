package com.retailmanagement.controller;

import com.retailmanagement.service.MigrateImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class MigrateController {

    @Autowired
    private MigrateImageService migrateImageService;

    // Gọi 1 lần rồi xóa endpoint này đi
    @PostMapping("/migrate-images")
    public ResponseEntity<String> migrate() {
        migrateImageService.migrateAllImages();
        return ResponseEntity.ok("Migration started! Check logs.");
    }
}