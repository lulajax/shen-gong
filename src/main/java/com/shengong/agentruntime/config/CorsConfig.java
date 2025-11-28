package com.shengong.agentruntime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 配置 - 允许跨域访问
 * 支持子域名嵌入聊天组件
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许子域名通配符（生产环境）
        config.addAllowedOriginPattern("*");
        config.addAllowedOriginPattern("*");

        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许携带凭证（Cookie）
        config.setAllowCredentials(true);

        // 预检请求缓存时间（1小时）
        config.setMaxAge(3600L);

        // 应用到所有 API 路径
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/widget/**", config);
        source.registerCorsConfiguration("/embed/**", config);

        return new CorsFilter(source);
    }
}
