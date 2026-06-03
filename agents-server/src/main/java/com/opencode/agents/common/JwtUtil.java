package com.opencode.agents.common;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private int expireHours;

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireHours * 3600_000L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("uid", userId);
        payload.put("iat", now);
        payload.put("exp", expiration);

        return JWTUtil.createToken(payload, secret.getBytes());
    }

    public Long getUserIdFromToken(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        if (!jwt.setKey(secret.getBytes()).verify()) {
            return null;
        }
        Object uid = jwt.getPayload("uid");
        if (uid == null) {
            return null;
        }
        return Long.valueOf(uid.toString());
    }

    public boolean validateToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            return jwt.setKey(secret.getBytes()).verify();
        } catch (Exception e) {
            return false;
        }
    }

}
