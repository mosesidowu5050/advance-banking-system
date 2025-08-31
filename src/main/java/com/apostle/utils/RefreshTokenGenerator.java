package com.apostle.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class RefreshTokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public static String generateRefreshToken() {
        byte[] randomBytes = new byte[32]; // 256-bit token
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public static void main(String[] args) {
        System.out.println("Refresh Token: " + generateRefreshToken());
    }
}
