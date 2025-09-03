package com.apostle.services.jwtService;


import com.apostle.data.model.Role;
import com.apostle.services.redisService.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {


    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private String expiration;

    private final TokenBlacklistService tokenBlacklistService;

    public JwtService(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }


    public String generateJwtToken(String email, Role role){
        return Jwts.builder()
                .setSubject(email)
                .claim("role", "ROLE_" + role.name())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(expiration)))
                .compact();
    }

    public Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractAllClaims(String token) {
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            throw new RuntimeException("Token has been revoked");
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (claims.getExpiration().before(new Date())) throw new RuntimeException("Token has expired");

        if (!"access".equals(claims.get("type", String.class))) throw new RuntimeException("Invalid token type");

        return claims;
    }

    public long getExpiration(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration().getTime() / 1000; // return as UNIX timestamp in seconds
    }

    public String extractUserId(String accessToken) {
        Claims claims = extractAllClaims(accessToken);
        return claims.getSubject();
    }

    public long getRemainingValidity(String accessToken) {
        Claims claims = extractAllClaims(accessToken);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}
