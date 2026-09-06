package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.blindtest.dto.BlindTestFormatRequest;
import com.weddinggames.backend.blindtest.dto.BlindTestFormatResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/games/{gameId}/blind-test-format")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Blind test", description = "Configuration du format des manches de blind test (reserve a l'administrateur)")
public class BlindTestFormatController {

    private final BlindTestFormatService formatService;

    public BlindTestFormatController(BlindTestFormatService formatService) {
        this.formatService = formatService;
    }

    @GetMapping
    public BlindTestFormatResponse get(@PathVariable UUID gameId) {
        return BlindTestFormatResponse.from(formatService.getOrCreate(gameId));
    }

    @PutMapping
    public BlindTestFormatResponse update(
            @PathVariable UUID gameId, @Valid @RequestBody BlindTestFormatRequest request) {
        return BlindTestFormatResponse.from(formatService.update(gameId, request));
    }
}
