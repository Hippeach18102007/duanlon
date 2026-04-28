package com.retailmanagement.controller;

import com.retailmanagement.dto.request.UserLoginFilterRequest;
import com.retailmanagement.dto.response.UserLoginResponse;
import com.retailmanagement.entity.UserLogin;
import com.retailmanagement.service.UserLoginLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/auth/userLogins")
@RequiredArgsConstructor
public class UserLoginController {

    private final UserLoginLogService userLoginLogService;

    @GetMapping("")
    public List<UserLoginResponse> findAll() {
        return userLoginLogService.getAllUserLogins();
    }

    @PostMapping("/filter")
    public Page<UserLoginResponse> filter(
            @RequestBody UserLoginFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        return userLoginLogService.filterUserLogins(
                request,
                page,
                size,
                sortBy,
                sortDir
        );
    }

    @PostMapping("/export")
    public void exportUserLogin(
            @RequestBody UserLoginFilterRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");

        String fileName = "login_logs_" + LocalDate.now() + ".csv";

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=" + fileName
        );

        userLoginLogService.exportCsv(request, response.getWriter());
    }
}
