package com.AJJ.ms_security.Repositories;

import com.AJJ.ms_security.Models.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    @Query("{'token': ?0}")
    PasswordResetToken findByToken(String token);

    @Query("{'user.$id': ObjectId(?0), 'used': false}")
    PasswordResetToken findActiveTokenByUserId(String userId);
}
