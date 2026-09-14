package com.datalink.config;

import com.alibaba.fastjson.JSON;
import com.datalink.model.R;
import com.datalink.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.http.HttpServletResponse;

/**
 * Spring Security 配置
 * <p>
 * 当 datalink.auth.password 为空时，放行所有请求（方便本地开发）；
 * 密码非空时，/api/** 需要认证，静态资源和 Swagger 等放行。
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthProperties authProperties;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.authenticationProvider(authenticationProvider());
        return builder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 禁用 CSRF（SPA + Session 认证）
        http.csrf().disable();

        // CORS：委托给已有的 CorsFilter Bean
        http.cors();

        if (!authProperties.isEnabled()) {
            // 密码为空 → 放行所有请求
            http.authorizeRequests().anyRequest().permitAll();
        } else {
            // 密码非空 → 启用认证
            http.authorizeRequests()
                    // 静态资源
                    .antMatchers("/*.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                    // Swagger / OpenAPI
                    .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // WebSocket
                    .antMatchers("/ws/**").permitAll()
                    // 认证接口
                    .antMatchers("/api/auth/login", "/api/auth/status").permitAll()
                    // 数据服务API调用接口（通过API Key鉴权，非登录态）
                    .antMatchers("/api/open/**").permitAll()
                    // 健康检查（供负载均衡/监控使用）
                    .antMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    // 根路径重定向
                    .antMatchers("/").permitAll()
                    // 其他 API 需要认证
                    .antMatchers("/api/**").authenticated()
                    // 其他资源放行
                    .anyRequest().permitAll();

            // 未认证时返回 JSON 401（不重定向到登录页）
            http.exceptionHandling()
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding("UTF-8");
                        R<Object> result = R.fail(R.CODE_UNAUTHORIZED, "未登录或登录已过期");
                        response.getWriter().write(JSON.toJSONString(result));
                    });
        }

        // 启用 Session 管理（默认行为，显式声明确保清晰）
        http.sessionManagement();

        // 禁用默认登录表单和 HTTP Basic
        http.formLogin().disable();
        http.httpBasic().disable();

        return http.build();
    }
}