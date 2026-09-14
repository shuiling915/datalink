package com.datalink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/workspace.html");
        registry.addRedirectViewController("/vue", "/vue/index.html");
        registry.addRedirectViewController("/vue/", "/vue/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vue SPA 静态资源
        registry.addResourceHandler("/vue/**")
                .addResourceLocations("classpath:/static/vue/");
    }
}