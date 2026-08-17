package com.weddinggames.backend.staff;

import com.weddinggames.backend.staff.dto.StaffAccountCreateRequest;
import com.weddinggames.backend.staff.dto.StaffAccountResponse;
import com.weddinggames.backend.staff.dto.StaffAccountUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Comptes staff", description = "Gestion des comptes et roles de l'organisation")
public class StaffAdminController {

    private final StaffAccountService staffAccountService;

    public StaffAdminController(StaffAccountService staffAccountService) {
        this.staffAccountService = staffAccountService;
    }

    @GetMapping
    public List<StaffAccountResponse> list() {
        return staffAccountService.list().stream().map(StaffAccountResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<StaffAccountResponse> create(@Valid @RequestBody StaffAccountCreateRequest request) {
        StaffAccount created = staffAccountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(StaffAccountResponse.from(created));
    }

    @GetMapping("/{id}")
    public StaffAccountResponse get(@PathVariable UUID id) {
        return StaffAccountResponse.from(staffAccountService.get(id));
    }

    @PutMapping("/{id}")
    public StaffAccountResponse update(@PathVariable UUID id, @Valid @RequestBody StaffAccountUpdateRequest request) {
        return StaffAccountResponse.from(staffAccountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        staffAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
