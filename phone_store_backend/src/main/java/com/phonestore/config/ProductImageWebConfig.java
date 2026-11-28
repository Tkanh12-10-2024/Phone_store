package com.phonestore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ProductImageWebConfig implements WebMvcConfigurer {

    private final String uploadDirectory;

    public ProductImageWebConfig(
            @Value("${phone-store.upload-dir:uploads/products}") String uploadDirectory
    ) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDirectory.endsWith("/") ? uploadDirectory : uploadDirectory + "/";
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(location);
    }
}
