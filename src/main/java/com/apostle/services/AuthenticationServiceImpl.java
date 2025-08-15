package com.apostle.services;

import com.apostle.data.model.AccountType;
import com.apostle.data.model.BankAccount;
import com.apostle.data.model.User;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.responses.LoginResponse;
import com.apostle.dtos.responses.RegisterResponses;
import com.apostle.exceptions.EmailNotSentException;
import com.apostle.exceptions.InvalidLoginException;
import com.apostle.exceptions.UserAlreadyExistException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.Set;

import static com.apostle.utils.Mapper.mapToRegisterRequest;


@Validated
@Service("authenticationService")
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService{

    private final Validator validator;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;
    private final EmailServiceImpl emailService;
    private final BankAccountServiceImpl bankAccountService;

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
        Optional<User> optionalUser =  userRepository.findUserByEmail(email);
        if (optionalUser.isEmpty()){
            throw new InvalidLoginException("User with provided credential does not exist");
        }

        boolean passwordMatches = bCryptPasswordEncoder.matches(loginRequest.getPassword(), optionalUser.get().getPassword());
        if (!passwordMatches) {
            throw new InvalidLoginException("Invalid credentials");
        }

        String name = optionalUser.get().getUsername();
        User user = optionalUser.get();
        String token = jwtService.generateJwtToken(optionalUser.get().getEmail(), user.getRole());
        return new LoginResponse(token, name, "Logged in success", true);
    }
}
