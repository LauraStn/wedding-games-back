package com.weddinggames.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weddinggames.backend.security.SessionService;
import com.weddinggames.backend.staff.StaffAccount;
import com.weddinggames.backend.staff.StaffAccountRepository;
import com.weddinggames.backend.staff.StaffRole;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base for full-stack integration tests: boots the real Spring context against a
 * Testcontainers PostgreSQL instance (so Flyway migrations and Hibernate schema validation
 * both run for real) and exposes MockMvc plus small helpers for the cookie-based auth flow.
 *
 * The container is started once per JVM and intentionally never stopped here: Testcontainers'
 * Ryuk reaper cleans it up when the test JVM exits, which is the standard "singleton container"
 * pattern for sharing one instance across many test classes without paying startup cost per class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String SESSION_COOKIE_NAME = "wg_session";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected StaffAccountRepository staffAccountRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected SessionService sessionService;

    private int staffCounter = 0;

    @BeforeEach
    void resetStaffCounter() {
        staffCounter = 0;
    }

    /** Creates a fresh staff account directly (bypassing HTTP) and logs in, returning the session cookie. */
    protected Cookie loginAsNewStaff(StaffRole role) throws Exception {
        String username = "test-" + role.name().toLowerCase() + "-" + (staffCounter++) + "-" + System.nanoTime();
        String rawPassword = "Password123!";
        staffAccountRepository.save(
                new StaffAccount(username, passwordEncoder.encode(rawPassword), "Test " + role.name(), role));

        String body = objectMapper.writeValueAsString(new LoginPayload(username, rawPassword));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        if (cookie == null) {
            throw new IllegalStateException("Aucun cookie de session recu apres connexion staff.");
        }
        return cookie;
    }

    /** Creates a participant session directly (bypassing the invitation flow) for a given participant id. */
    protected Cookie loginAsParticipant(UUID participantId) {
        String rawToken = sessionService.createParticipantSession(participantId);
        return new Cookie(SESSION_COOKIE_NAME, rawToken);
    }

    private record LoginPayload(String username, String password) {}
}
