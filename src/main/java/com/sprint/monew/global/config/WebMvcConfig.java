package com.sprint.monew.global.config;

import com.sprint.monew.global.web.RequestIdInterceptor;
import com.sprint.monew.global.web.StringToSortOrderConverterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestIdInterceptor requestIdInterceptor;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToSortOrderConverterFactory());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestIdInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/users", "/api/users/login");
    }
}
