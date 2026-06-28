package com.socialnetwork.social.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // غیرفعال کردن CSRF برای APIها
                .authorizeHttpRequests(auth -> auth
                        // مسیرهایی که نیاز به توکن ندارند:
                        .requestMatchers("/api/users/register", "/api/users/login", "/ws-chat/**").permitAll()
                        // بقیه مسیرها نیاز به احراز هویت دارند:
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}