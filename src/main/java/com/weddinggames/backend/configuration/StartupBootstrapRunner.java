package com.weddinggames.backend.configuration;

import com.weddinggames.backend.event.EventService;
import com.weddinggames.backend.staff.StaffAccountService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first WeddingEvent and the first ADMIN account from environment
 * variables, if configured. Idempotent: safe to run on every startup. Leaves
 * everything untouched when the relevant env vars are blank (e.g. in tests).
 */
@Component
public class StartupBootstrapRunner implements ApplicationRunner {

    private final EventService eventService;
    private final StaffAccountService staffAccountService;
    private final BootstrapEventProperties eventProperties;
    private final BootstrapAdminProperties adminProperties;

    public StartupBootstrapRunner(
            EventService eventService,
            StaffAccountService staffAccountService,
            BootstrapEventProperties eventProperties,
            BootstrapAdminProperties adminProperties) {
        this.eventService = eventService;
        this.staffAccountService = staffAccountService;
        this.eventProperties = eventProperties;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (eventProperties.getSlug() != null && !eventProperties.getSlug().isBlank()) {
            eventService.ensureEventExists(
                    eventProperties.getSlug(), eventProperties.getTitle(), eventProperties.getLanguage());
        }
        staffAccountService.ensureAdminExists(
                adminProperties.getUsername(), adminProperties.getPassword(), adminProperties.getDisplayName());
    }
}
