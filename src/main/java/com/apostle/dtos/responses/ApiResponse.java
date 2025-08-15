package com.apostle.dtos.responses;

public record ApiResponse(boolean success, String message, Object data) {
}
