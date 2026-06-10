package com.opencode.agents.config;

import com.opencode.agents.common.JwtUtil;
import com.opencode.agents.manager.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/auth/password-login",
            "/api/auth/register",
            "/api/health",
            "/api/files/**",
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(fileStorageService.getUploadDir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(uploadPath);
    }
}
