package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.Contact;
import com.socialnetwork.social.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    boolean existsByUserAndContactUser(User user, User contactUser);
    Optional<Contact> findByUserAndContactUser(User user, User contactUser);
    List<Contact> findAllByUser(User user);
    List<Contact> findAllByContactUser(User contactUser);
}
