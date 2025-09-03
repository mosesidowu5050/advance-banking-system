package com.apostle.services.authService;

import com.apostle.data.model.AccountType;
import com.apostle.data.model.BankAccount;
import com.apostle.data.model.User;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.responses.CachedUser;
import com.apostle.dtos.responses.LoginResponse;
import com.apostle.dtos.responses.RegisterResponses;
import com.apostle.exceptions.EmailNotSentException;
import com.apostle.exceptions.InvalidLoginException;
import com.apostle.exceptions.UserAlreadyExistException;
import com.apostle.services.redisService.RedisService;
import com.apostle.services.refreshService.RefreshTokenService;
import com.apostle.services.bankService.BankAccountServiceImpl;
import com.apostle.services.emailService.EmailServiceImpl;
import com.apostle.services.jwtService.JwtService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.apostle.utils.Mapper.mapToRegisterRequest;


@Validated
@Service("authenticationService")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService{

    private final Validator validator;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;
    private final EmailServiceImpl emailService;
    private final BankAccountServiceImpl bankAccountService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RefreshTokenService refreshTokenService;
    private final RedisService redisService;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final long USER_CACHE_TTL_MINUTES = 5;

    @Override
    public RegisterResponses register(RegisterRequest registerRequest) {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        boolean emailExists = userRepository.findUserByEmail(registerRequest.getEmail()).isPresent();
        if (emailExists){
            throw new UserAlreadyExistException("Email already exists");
        }

        User user = mapToRegisterRequest(registerRequest);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        String email = user.getEmail().toLowerCase();
        userRepository.save(user);
        BankAccount createdAccount = bankAccountService.createAccountForUser(user, AccountType.SAVINGS);

        try {
            emailService.sendAccountNumberEmail(email, createdAccount.getAccountNumber());
        } catch (Exception e) {
            userRepository.delete(user);
            throw new EmailNotSentException("Error sending email" + e);
        }

        RegisterResponses registerResponses = new RegisterResponses();
        registerResponses.setMessage("User Registration Successful");
        registerResponses.setSuccess(true);
        registerResponses.setAccountNumber(createdAccount.getAccountNumber());
        return registerResponses;

    }


    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().toLowerCase();
        String cacheKey = USER_CACHE_PREFIX + email;

        CachedUser cachedUser = (CachedUser) redisTemplate.opsForValue().get(cacheKey);
        User user;

        if (cachedUser != null) {
            log.info("✅ User fetched from Redis: {}", email);
            user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new InvalidLoginException("User with provided credential does not exist"));
        } else {
            user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new InvalidLoginException("User with provided credential does not exist"));

            CachedUser safeCache = new CachedUser(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getRole()
            );

            redisTemplate.opsForValue().set(cacheKey, safeCache, USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("✅ User cached in Redis (without password): {}", safeCache);
        }

        boolean passwordMatches = bCryptPasswordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!passwordMatches) throw new InvalidLoginException("Invalid credentials");

        String accessToken = jwtService.generateJwtToken(user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), user.getRole());

        return new LoginResponse(accessToken, refreshToken, user.getUsername(), "Log in successful", true);
    }


    @Override
    public void logout(String accessToken) {
        String userId = jwtService.extractUserId(accessToken);

        long ttl = jwtService.getRemainingValidity(accessToken);
        redisService.blacklistToken(accessToken, ttl);
        log.info("Access token blacklisted for userId={}", userId);

        refreshTokenService.revokeAllRefreshTokensForUser(userId);
        refreshTokenService.revokeAllAccessTokensForUser(userId);
        log.info("All tokens revoked for userId={}", userId);
    }


}
