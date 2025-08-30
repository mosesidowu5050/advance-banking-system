package com.apostle.controllers;

import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.services.authService.AuthenticationService;
import com.apostle.services.jwtService.JwtService;
import com.apostle.services.redisService.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RedisService redisService;

    public AuthenticationController(AuthenticationService authenticationService, JwtService jwtService, RedisService redisService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.redisService = redisService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            return ResponseEntity.ok(authenticationService.register(registerRequest));
        }catch (Exception exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }

    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest){
        try {
            return ResponseEntity.ok(authenticationService.login(loginRequest));
        }catch (Exception exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long expiration = jwtService.getExpiration(token); // extract exp from token
            long now = System.currentTimeMillis() / 1000;
            long ttl = expiration - now;

            redisService.blacklistToken(token, ttl);
        }
        return ResponseEntity.ok("Logged out successfully.");
    }



}
