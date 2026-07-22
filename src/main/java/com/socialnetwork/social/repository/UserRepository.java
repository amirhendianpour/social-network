package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByEmail(String email);

    // برای لاگین: شناسه ورودی می‌تواند ایمیل یا شماره موبایل باشد
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.phoneNumber = :identifier")
    Optional<User> findByEmailOrPhoneNumber(@Param("identifier") String identifier);

    List<User> findByPhoneNumberIn(List<String> phoneNumbers);
    List<User> findByUsernameIn(List<String> usernames);
}