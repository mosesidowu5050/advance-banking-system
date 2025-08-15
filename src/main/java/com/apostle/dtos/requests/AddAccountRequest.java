package com.apostle.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class AddAccountRequest {
    @NotNull
    private String name;
    private String description;
}
