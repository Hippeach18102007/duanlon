package com.retailmanagement.controller;

import com.retailmanagement.security.log.SecurityLog;
import com.retailmanagement.security.log.SecurityLogFilterRequest;
import com.retailmanagement.security.log.SecurityLogRepository;
import com.retailmanagement.security.log.SecurityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/auth/security-log")
@RequiredArgsConstructor
public class SecurityLogController {

    private final SecurityLogService securityLogService;

    @PostMapping("search")
    public Page<SecurityLog> filterLogs(
            @RequestBody SecurityLogFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return securityLogService.filterLogs(
                request, page, size, sortBy, sortDir
        );
    }
}
