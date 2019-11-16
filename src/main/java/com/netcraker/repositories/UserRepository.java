package com.netcraker.repositories;

import com.netcraker.model.User;

import java.util.Optional;

public interface UserRepository extends BaseOptionalRepository<User> {
    Optional<User> findByEmail(String email);
    Optional<User> activateUser(String email);
}
