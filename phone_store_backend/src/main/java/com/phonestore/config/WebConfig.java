package com.phonestore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path baseUploadPath = Paths.get("uploads").toAbsolutePath().normalize();
        String baseUploadUri = baseUploadPath.toUri().toString();
        if (!baseUploadUri.endsWith("/")) {
            baseUploadUri += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(baseUploadUri);
    }
}
