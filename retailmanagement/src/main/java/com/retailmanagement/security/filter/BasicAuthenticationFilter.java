package com.retailmanagement.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RequiredArgsConstructor
public class BasicAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Chỉ xử lý Basic Auth, bỏ qua Bearer
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Không xử lý nếu đã có authentication (ví dụ JWT đã chạy trước)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String base64Credentials = authHeader.substring(6);
            String decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            // format: username:password
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = parts[0];
            String password = parts[1];

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Sai credentials → không set authentication, để Spring Security tự trả 401
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}