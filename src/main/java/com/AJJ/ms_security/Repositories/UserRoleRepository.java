package com.AJJ.ms_security.Repositories;

import com.AJJ.ms_security.Models.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRoleRepository  extends MongoRepository<UserRole,String> {
    @Query("{'user._id': ?0}")
    List<UserRole> getRolesByUser(String userId);
}
