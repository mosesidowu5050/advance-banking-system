package com.apostle.dtos.responses;

import com.apostle.data.model.Role;
import java.io.Serializable;

public record CachedUser(
        String id,
        String email,
        String username,
        Role role
)
        implements Serializable {

}
