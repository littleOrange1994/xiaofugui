package com.xiaofugui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${recipe.import.source-dir:/Users/pomazhangfei/Documents/CookLikeHOC}")
    private String sourceDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射菜谱图片目录
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + sourceDir + "/images/");
    }
}
