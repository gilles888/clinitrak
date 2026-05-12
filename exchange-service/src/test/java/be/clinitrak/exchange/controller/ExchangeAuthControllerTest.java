package be.clinitrak.exchange.controller;

import be.clinitrak.exchange.config.SecurityConfig;
import be.clinitrak.exchange.domain.enums.ExternalUserRole;
import be.clinitrak.exchange.dto.*;
import be.clinitrak.exchange.exception.GlobalExceptionHandler;
import be.clinitrak.exchange.security.ExchangeJwtFilter;
import be.clinitrak.exchange.security.ExchangeJwtService;
import be.clinitrak.exchange.security.JwtAuthenticationFilter;
import be.clinitrak.exchange.security.JwtService;
import be.clinitrak.exchange.service.ExternalUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests du contrôleur d'authentification des utilisateurs externes.
 *
 * <p>Vérifie les endpoints publics : inscription, connexion et vérification d'email.
 */
@WebMvcTest(ExchangeAuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@DisplayName("ExchangeAuthController — tests @WebMvcTest")
class ExchangeAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExternalUserService externalUserService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private ExchangeJwtService exchangeJwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ExchangeJwtFilter exchangeJwtFilter;

    @Test
    @DisplayName("POST /register → 201 Created")
    void register_shouldReturn201() throws Exception {
        // Given
        ExternalUserRegisterRequest req = new ExternalUserRegisterRequest(
            "investigator@pharma.com",
            "SecurePass123!",
            "Jean",
            "Martin",
            "PharmaGroup SA",
            ExternalUserRole.INVESTIGATOR
        );

        ExternalUserResponse response = new ExternalUserResponse(
            UUID.randomUUID(),
            "investigator@pharma.com",
            "Jean",
            "Martin",
            "PharmaGroup SA",
            ExternalUserRole.INVESTIGATOR,
            "Investigateur",
            false,
            LocalDateTime.now()
        );

        when(externalUserService.register(any(ExternalUserRegisterRequest.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/exchange/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("investigator@pharma.com"))
            .andExpect(jsonPath("$.role").value("INVESTIGATOR"))
            .andExpect(jsonPath("$.verifiedEmail").value(false));
    }

    @Test
    @DisplayName("POST /login → 200 OK avec token JWT")
    void login_shouldReturn200WithToken() throws Exception {
        // Given
        ExternalUserLoginRequest req = new ExternalUserLoginRequest(
            "investigator@pharma.com",
            "SecurePass123!"
        );

        ExternalUserResponse userResponse = new ExternalUserResponse(
            UUID.randomUUID(),
            "investigator@pharma.com",
            "Jean",
            "Martin",
            "PharmaGroup SA",
            ExternalUserRole.INVESTIGATOR,
            "Investigateur",
            true,
            LocalDateTime.now()
        );

        ExternalUserLoginResponse loginResponse = new ExternalUserLoginResponse(
            "eyJhbGciOiJIUzI1NiJ9.test.signature",
            "Bearer",
            userResponse
        );

        when(externalUserService.login(any(ExternalUserLoginRequest.class))).thenReturn(loginResponse);

        // When / Then
        mockMvc.perform(post("/api/v1/exchange/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("investigator@pharma.com"));
    }

    @Test
    @DisplayName("GET /verify/{token} → 200 OK avec email vérifié")
    void verifyEmail_shouldReturn200() throws Exception {
        // Given
        String verificationToken = UUID.randomUUID().toString();

        ExternalUserResponse response = new ExternalUserResponse(
            UUID.randomUUID(),
            "investigator@pharma.com",
            "Jean",
            "Martin",
            "PharmaGroup SA",
            ExternalUserRole.INVESTIGATOR,
            "Investigateur",
            true,
            LocalDateTime.now()
        );

        when(externalUserService.verifyEmail(anyString())).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/exchange/auth/verify/{token}", verificationToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verifiedEmail").value(true))
            .andExpect(jsonPath("$.email").value("investigator@pharma.com"));
    }
}
