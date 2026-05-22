package org.example.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.demo.config.SecurityConfig;
import org.example.demo.model.AuthResponse;
import org.example.demo.model.AuthUserResponse;
import org.example.demo.security.SessionAuthenticationFilter;
import org.example.demo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({TravelPlannerExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private SessionAuthenticationFilter sessionAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerReturnsCreatedUser() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse(true, new AuthUserResponse(1L, "traveler", "旅行者")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                            put("username", "traveler");
                            put("password", "123456");
                            put("confirmPassword", "123456");
                            put("nickname", "旅行者");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.username").value("traveler"));
    }

    @Test
    void loginReturnsAuthenticatedUser() throws Exception {
        when(authService.login(any(), any())).thenReturn(new AuthResponse(true, new AuthUserResponse(1L, "traveler", "旅行者")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
                            put("username", "traveler");
                            put("password", "123456");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void meReturnsAnonymousWhenNotLoggedIn() throws Exception {
        when(authService.currentUser(any())).thenReturn(new AuthResponse(false, null));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void logoutReturnsAnonymous() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
