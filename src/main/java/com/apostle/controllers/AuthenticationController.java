package com.apostle.controllers;

import com.apostle.data.model.Role;
import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RefreshTokenRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.services.refreshService.RefreshTokenService;
import com.apostle.services.authService.AuthenticationService;
import com.apostle.services.jwtService.JwtService;
import com.apostle.services.redisService.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RedisService redisService;
    private final RefreshTokenService refreshTokenService;

//    public AuthenticationController(AuthenticationService authenticationService,
//                                    JwtService jwtService, RedisService redisService) {
//        this.authenticationService = authenticationService;
//        this.jwtService = jwtService;
//        this.redisService = redisService;
//    }

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


    @PostMapping("/token-refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            return ResponseEntity.ok(refreshTokenService
                    .refreshAccessToken(request.getRefreshToken()));
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        String accessToken = authHeader.substring(7);
        authenticationService.logout(accessToken);

        return ResponseEntity.ok("Logged out successfully");
    }


}
