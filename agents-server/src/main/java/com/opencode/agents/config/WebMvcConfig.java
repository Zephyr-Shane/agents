package com.opencode.agents.config;

import com.opencode.agents.common.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/auth/login",
            "/api/health",
            "/doc.html",
            "/swagger-resources",
            "/v3/api-docs",
            "/webjars/**",
            "/favicon.ico"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtUtil))
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }

}
