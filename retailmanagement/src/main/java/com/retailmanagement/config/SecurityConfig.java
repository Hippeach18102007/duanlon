package com.retailmanagement.config;

import com.retailmanagement.security.filter.JwtAuthenticationFilter;
import com.retailmanagement.security.handler.CustomAccessDeniedHandler;
import com.retailmanagement.security.handler.CustomAuthenticationEntryPoint;
import com.retailmanagement.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableAspectJAutoProxy
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager chuẩn cho Spring Boot 3+
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .anonymous(anon -> anon.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/error",
                                "/api/auth/**",
                                "/actuator/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/tags/**",
                                "/uploads/**",
                                "/api/auth/payments/vnpay-ipn",
                                "/api/auth/payments/vnpay-return",
                                "/api/orders/public/**",
                                "/api/public/**",
                                "/api/chat/customer",
                                "/admin/migrate-images"
                        ).permitAll()

                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/sales/**").hasAnyRole("SALES", "ADMIN")
                        .requestMatchers("/api/inventory/**").hasAnyRole("INVENTORY", "ADMIN")
                        .requestMatchers("/api/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN", "INVENTORY", "SALES")
                        .requestMatchers("/api/chat/staff/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}