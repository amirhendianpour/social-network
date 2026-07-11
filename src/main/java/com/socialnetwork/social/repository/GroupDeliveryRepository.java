package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.GroupDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupDeliveryRepository extends JpaRepository<GroupDelivery, Long> {
    List<GroupDelivery> findByRecipientUsernameAndStatus(String recipientUsername, String status);
}
