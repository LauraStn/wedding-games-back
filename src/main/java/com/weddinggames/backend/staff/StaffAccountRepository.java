package com.weddinggames.backend.staff;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, UUID> {

    Optional<StaffAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
