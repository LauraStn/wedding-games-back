package com.weddinggames.backend.staff;

import com.weddinggames.backend.common.exception.ConflictException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.staff.dto.StaffAccountCreateRequest;
import com.weddinggames.backend.staff.dto.StaffAccountUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAccountService {

    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountService(StaffAccountRepository staffAccountRepository, PasswordEncoder passwordEncoder) {
        this.staffAccountRepository = staffAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StaffAccount> list() {
        return staffAccountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public StaffAccount get(UUID id) {
        return staffAccountRepository.findById(id).orElseThrow(() -> new NotFoundException("Compte introuvable."));
    }

    @Transactional
    public StaffAccount create(StaffAccountCreateRequest request) {
        if (staffAccountRepository.existsByUsername(request.username())) {
            throw new ConflictException("USERNAME_TAKEN", "Ce nom d'utilisateur est deja utilise.");
        }
        StaffAccount account = new StaffAccount(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.role());
        return staffAccountRepository.save(account);
    }

    @Transactional
    public StaffAccount update(UUID id, StaffAccountUpdateRequest request) {
        StaffAccount account = get(id);
        account.setDisplayName(request.displayName());
        account.setRole(request.role());
        account.setActive(request.active());
        if (request.password() != null && !request.password().isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return account;
    }

    @Transactional
    public void delete(UUID id) {
        if (!staffAccountRepository.existsById(id)) {
            throw new NotFoundException("Compte introuvable.");
        }
        staffAccountRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public StaffAccount authenticate(String username, String rawPassword) {
        StaffAccount account = staffAccountRepository
                .findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides."));
        if (!account.isActive() || !passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides.");
        }
        return account;
    }

    /** Idempotent: does nothing if an account with that username already exists. */
    @Transactional
    public void ensureAdminExists(String username, String rawPassword, String displayName) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return;
        }
        if (staffAccountRepository.existsByUsername(username)) {
            return;
        }
        staffAccountRepository.save(
                new StaffAccount(username, passwordEncoder.encode(rawPassword), displayName, StaffRole.ADMIN));
    }
}
