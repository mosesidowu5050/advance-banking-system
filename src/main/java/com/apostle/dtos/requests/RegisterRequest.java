package com.apostle.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,20}$", message = "Username must be alphanumeric and between 3 and 20 characters")
    private String username;
    @Email(message = "Email is invalid")
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Please provide a valid email address")
    private String email;
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Password must be at least 8 characters long and contain at least one digit, one lowercase, one uppercase, and one special character")
    private String password;




}
