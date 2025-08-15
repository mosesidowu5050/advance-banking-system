package com.apostle.services;

import com.apostle.data.repositories.BankAccountRepository;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.responses.LoginResponse;
import com.apostle.dtos.responses.RegisterResponses;
import com.apostle.exceptions.InvalidLoginException;
import com.apostle.exceptions.UserAlreadyExistException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AuthenticationServiceImplTest {

    @Autowired
    private AuthenticationServiceImpl authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @BeforeEach
    public void beforeEach() {
        userRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    private RegisterRequest createRegisterRequest(String email, String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    public void testRegisterSuccess() {
        RegisterRequest request = createRegisterRequest("john.doe1@example.com", "johndoe", "Password@2024");
        RegisterResponses response = authenticationService.register(request);
        System.out.println(response.getAccountNumber());
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("User Registration Successful", response.getMessage());
        assertNotNull(response.getAccountNumber());
    }

    @Test
    public void testDuplicateEmailThrowsException() {
        RegisterRequest request = createRegisterRequest("john.doe2@example.com", "johndoe", "Password@2024");
        authenticationService.register(request);

        assertThrows(UserAlreadyExistException.class, () -> authenticationService.register(request));
    }

    @Test
    public void testBlankUsernameThrowsValidationError() {
        RegisterRequest request = createRegisterRequest("john.doe3@example.com", "", "Password@2024");
        assertThrows(ConstraintViolationException.class, () -> authenticationService.register(request));
    }

    @Test
    public void testInvalidEmailFormatThrowsValidationError() {
        RegisterRequest request = createRegisterRequest("not-an-email", "johndoe", "Password@2024");
        assertThrows(ConstraintViolationException.class, () -> authenticationService.register(request));
    }

    @Test
    public void testWeakPasswordThrowsValidationError() {
        RegisterRequest request = createRegisterRequest("john.doe4@example.com", "johndoe", "weakpass");
        assertThrows(ConstraintViolationException.class, () -> authenticationService.register(request));
    }

    @Test
    public void testLogin_works(){
        RegisterRequest request = createRegisterRequest("person@gmail.com", "person_one", "Person@2002");
        authenticationService.register(request);
        assertNotNull(request);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());

        LoginResponse loginResponse = authenticationService.login(loginRequest);
        assertNotNull(loginResponse);
        assertTrue(loginResponse.isSuccess());
        assertFalse(loginResponse.getToken().isEmpty());
    }

    @Test
    public void testThatLoginThrowExceptionFor_wrongEmail(){
        RegisterRequest registerRequest = createRegisterRequest("johndoe@gmail.com", "johndoe", "Password@2024");
        authenticationService.register(registerRequest);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("johndoe@gmail.com");
        loginRequest.setPassword("wrongpassword");

        Exception exception = assertThrows(InvalidLoginException.class, () -> authenticationService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    public void testThatLoginThrowExceptionFor_nonExistingEmail(){
        RegisterRequest registerRequest = createRegisterRequest("correctemail@gmail.com", "Mikeel_1", "CorrectPassword@2024");
        authenticationService.register(registerRequest);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongEmail@gmail.com");
        loginRequest.setPassword("CorrectPassword@2024");

        Exception exception = assertThrows(InvalidLoginException.class, () -> authenticationService.login(loginRequest));
        assertEquals("User with provided credential does not exist", exception.getMessage());
    }

    @Test
    public void testLoginWithEmailInDifferentCase() {
        RegisterRequest registerRequest = createRegisterRequest("caseTest@gmail.com", "caseuser", "Password@2002");
        authenticationService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("CASETesT@GMAIL.com");
        loginRequest.setPassword("Password@2002");

        LoginResponse loginResponse = authenticationService.login(loginRequest);
        assertNotNull(loginResponse);
        assertTrue(loginResponse.isSuccess());
    }

    @Test
    public void testThatLoginFailsForEmptyPassword() {
        RegisterRequest registerRequest = createRegisterRequest("test@gmail.com", "test_user", "Password@123");
        authenticationService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(registerRequest.getEmail());
        loginRequest.setPassword("");

        Exception exception = assertThrows(InvalidLoginException.class, () -> authenticationService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    public void testThatLoginFailsForEmptyEmail() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("");
        loginRequest.setPassword("SomePassword@2023");

        Exception exception = assertThrows(InvalidLoginException.class, () -> authenticationService.login(loginRequest));
        assertEquals("User with provided credential does not exist", exception.getMessage());
    }




}
