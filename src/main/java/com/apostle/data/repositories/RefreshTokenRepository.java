package com.apostle.data.repositories;

import com.apostle.data.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserId(String userId);

    void deleteByUserId(String userId);

    List<RefreshToken> findAllByUserId(String userId);
}
