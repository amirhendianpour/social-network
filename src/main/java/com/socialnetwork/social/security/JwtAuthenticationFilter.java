package com.socialnetwork.social.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. دریافت هدر Authorization از درخواست
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 2. بررسی اینکه آیا هدر وجود دارد و با Bearer شروع می‌شود
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // حذف "Bearer " برای استخراج توکن خام
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                System.out.println("JWT Token Extraction Failed: " + e.getMessage());
            }
        }

        // 3. اگر نام کاربری استخراج شد و قبلاً در کانتکست امنیتی تنظیم نشده بود
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // (در یک سیستم واقعی، اینجا معمولاً کاربر را از دیتابیس هم می‌خوانند تا چک کنند حسابش فعال است یا نه)
            // اما برای سادگی، ما به خود توکن اعتماد می‌کنیم (چون قبلاً با کلید مخفی ما امضا شده)

            // ایجاد آبجکت احراز هویت اسپرینگ
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    username, null, new ArrayList<>());

            usernamePasswordAuthenticationToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 4. تنظیم کاربر به عنوان "تایید شده" در کانتکست امنیتی اسپرینگ
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        }

        // ادامه مسیر فیلترها
        chain.doFilter(request, response);
    }
}