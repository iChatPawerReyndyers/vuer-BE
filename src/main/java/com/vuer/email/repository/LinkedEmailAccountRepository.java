package com.vuer.email.repository;

import com.vuer.email.entity.LinkedEmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LinkedEmailAccountRepository extends JpaRepository<LinkedEmailAccount, UUID> {

    List<LinkedEmailAccount> findAllByIsActiveTrue();

    List<LinkedEmailAccount> findAllByUserIdAndIsActiveTrue(UUID userId);
}
