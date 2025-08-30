package com.apostle.security;


import com.apostle.services.jwtService.JwtService;
import com.apostle.services.redisService.RedisService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter  extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisService redisService;

    public JwtAuthenticationFilter(JwtService jwtService, RedisService redisService) {
        this.jwtService = jwtService;
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull  HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer")){
            filterChain.doFilter(request, response);
            return;
        }
        String jwtToken = authorizationHeader.substring(7);

        try {
            if (redisService.isTokenBlacklisted(jwtToken)) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Claims claims = jwtService.extractAllClaims(jwtToken);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);


        }catch (Exception exception){
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
