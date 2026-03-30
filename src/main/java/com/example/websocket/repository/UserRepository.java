package com.example.websocket.repository;


import com.example.websocket.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);


    @Query("select u from User u " +
            "where lower(u.email) like lower(concat('%', :q, '%')) " +
            "or lower(u.username) like lower(concat('%', :q, '%'))")
    List<User> searchByEmailOrUsername(@Param("q") String q );


}
