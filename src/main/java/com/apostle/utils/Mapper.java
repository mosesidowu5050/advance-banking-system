package com.apostle.utils;

import com.apostle.data.model.Role;
import com.apostle.data.model.User;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.responses.TransactionResponse;

public class Mapper {

    public static User mapToRegisterRequest(RegisterRequest registerRequest) {
        User user = new User();
        user.setEmail(registerRequest.getEmail().toLowerCase());
        user.setPassword(registerRequest.getPassword());
        user.setUsername(registerRequest.getUsername().toLowerCase());
        user.setRole(Role.CUSTOMER);
        return user;
    }

//    public static TransactionResponse mapToTransactionResponse(TransactionResponse transactionResponse)

//    public static RegisterResponses mapToRegisterResponses() {
//        RegisterResponses registerResponses = new RegisterResponses();
//        registerResponses.setMessage("User Registration Successful");
//        registerResponses.setSuccess(true);
//        registerResponses.setAccountNumber();
//        return registerResponses;
//    }


}
