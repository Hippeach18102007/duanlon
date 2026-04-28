package com.retailmanagement.controller;

import com.retailmanagement.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestApiController {

    @GetMapping("/protected")
    public ApiResponse<String> protectedApi() {
        return ApiResponse.success("OK protected");
    }
}

