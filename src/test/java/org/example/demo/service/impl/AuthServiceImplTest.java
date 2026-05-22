package org.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.demo.entity.UserEntity;
import org.example.demo.mapper.UserMapper;
import org.example.demo.model.AuthResponse;
import org.example.demo.model.LoginRequest;
import org.example.demo.model.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthServiceImpl authService = new AuthServiceImpl(userMapper, passwordEncoder);

    @Test
    void registerCreatesUserWithEncodedPassword() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        AuthResponse response = authService.register(new RegisterRequest("traveler", "123456", "123456", "旅行者"));

        assertTrue(response.authenticated());
        assertEquals("traveler", response.user().username());
        verify(userMapper).insert(any(UserEntity.class));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("traveler", "123456", "123456", "旅行者")));

        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void registerRejectsDifferentPasswords() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("traveler", "123456", "abcdef", "旅行者")));

        assertEquals("两次输入的密码不一致", exception.getMessage());
    }

    @Test
    void loginStoresUserIdInSession() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("traveler");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setNickname("旅行者");
        user.setEnabled(true);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(session);

        AuthResponse response = authService.login(new LoginRequest("traveler", "123456"), request);

        assertTrue(response.authenticated());
        verify(session).setAttribute(AuthServiceImpl.SESSION_USER_ID, 7L);
    }

    @Test
    void loginRejectsWrongPassword() {
        UserEntity user = new UserEntity();
        user.setUsername("traveler");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setEnabled(true);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("traveler", "654321"), mock(HttpServletRequest.class)));

        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void currentUserReturnsAnonymousWhenNoSessionExists() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        AuthResponse response = authService.currentUser(request);

        assertFalse(response.authenticated());
    }
}
