package com.socialnetwork.social.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // یک کلید مخفی و امن برای امضای توکن‌ها (در پروژه‌های واقعی این کلید در Environment Variables قرار می‌گیرد)
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // زمان انقضای توکن (مثلاً ۲۴ ساعت)
    private final long EXPIRATION_TIME = 86400000L;

    // متد ساخت توکن بر اساس نام کاربری (یا شماره موبایل)
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // متد استخراج نام کاربری از توکن
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}