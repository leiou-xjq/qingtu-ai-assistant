package com.qingtu.agent.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 
 * 功能说明：
 * - 生成JWT Token
 * - 解析Token获取用户信息
 * - 验证Token有效性
 * 
 * @author 青途智伴技术团队
 */
@Slf4j
@Data
public class JwtUtil {

    /**
     * JWT密钥
     */
    private String secret;

    /**
     * 过期时间（毫秒）
     */
    private long expiration;

    /**
     * Token Header名称
     */
    private String header;

    /**
     * Token前缀
     */
    private String prefix;

    /**
     * 生成Token
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT Token字符串
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, userId.toString());
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析Token获取Claims
     */
    public Claims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Token解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return (String) claims.get("username");
    }

    /**
     * 验证Token是否过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        return !isTokenExpired(token) && parseToken(token) != null;
    }

    /**
     * 从请求中提取Token
     * 
     * @param authHeader Authorization Header
     * @return Token字符串
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length());
        }
        return null;
    }
}