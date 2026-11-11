package com.zbkj.overseas.config;

import com.zbkj.overseas.interceptor.AdminTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web configuration — global CORS for the public storefront API
 * (the PC frontend may be served from a CDN domain), and the admin
 * interceptor guarding /api/admin/**, mirroring tjmall's interceptor usage.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AdminTokenInterceptor adminTokenInterceptor;

    @Value("${overseas.upload-dir:./data/uploads}")
    private String uploadDir;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // The admin UI is a static page (classpath:/static/admin/index.html).
        registry.addRedirectViewController("/admin", "/admin/index.html");
        registry.addRedirectViewController("/admin/", "/admin/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Uploaded product images live outside the jar so they survive redeploys.
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminTokenInterceptor).addPathPatterns("/api/admin/**");
    }
}
