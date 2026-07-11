package com.socialnetwork.social.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // هر درخواستی که به مسیر /uploads/** بیاید را به پوشه محلی uploads/ نگاشت می‌کند
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}