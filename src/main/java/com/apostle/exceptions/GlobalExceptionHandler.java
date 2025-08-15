package com.apostle.exceptions;


import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException ex){
        Map<String,Object> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> errors.put(
                violation.getPropertyPath().toString(),
                violation.getMessage()
        )  );
        return new ResponseEntity<>(buildErrorResponse(errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex){
        return new ResponseEntity<>(buildErrorResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<?> handleUserAlreadyExistException(UserAlreadyExistException exception){
        return new ResponseEntity<>(buildErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
    }
    private Map<String, Object> buildErrorResponse(Object detailedErrors) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("Time stamp", LocalDateTime.now());
        errors.put("message", "Validation failed");
        errors.put("detailedErrors", detailedErrors);
        return errors;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMsg = error.getDefaultMessage();
            errors.put(fieldName, errorMsg);
        });

        return ResponseEntity.badRequest().body(errors);
    }


}
