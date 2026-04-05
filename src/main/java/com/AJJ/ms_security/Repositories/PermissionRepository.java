package com.AJJ.ms_security.Repositories;

import com.AJJ.ms_security.Models.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface PermissionRepository extends MongoRepository<Permission, String> {

    @Query("{'url': ?0, 'method': ?1}")
    Permission getPermission(String url, String method);

    @Query("{'name': ?0}")
    Permission findByName(String name);

    @Query("{'module': ?0}")
    List<Permission> findByModule(String module);

    @Query("{'url': ?0, 'method': ?1}")
    Permission findByUrlAndMethod(String url, String method);
}