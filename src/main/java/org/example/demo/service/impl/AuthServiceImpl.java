package org.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.demo.entity.UserEntity;
import org.example.demo.mapper.UserMapper;
import org.example.demo.model.AuthResponse;
import org.example.demo.model.AuthUserResponse;
import org.example.demo.model.LoginRequest;
import org.example.demo.model.RegisterRequest;
import org.example.demo.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    public static final String SESSION_USER_ID = "auth.userId";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        UserEntity user = new UserEntity();
        Instant now = Instant.now();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(normalizeNickname(request.nickname(), username));
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return new AuthResponse(true, toResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        String username = normalizeUsername(request.username());
        UserEntity user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("当前账号已被禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(SESSION_USER_ID, user.getId());
        return new AuthResponse(true, toResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse currentUser(HttpServletRequest httpServletRequest) {
        HttpSession session = httpServletRequest.getSession(false);
        if (session == null) {
            return new AuthResponse(false, null);
        }

        Object userId = session.getAttribute(SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            return new AuthResponse(false, null);
        }

        UserEntity user = userMapper.selectById(id);
        if (user == null || !user.isEnabled()) {
            return new AuthResponse(false, null);
        }
        return new AuthResponse(true, toResponse(user));
    }

    @Override
    public void logout(HttpServletRequest httpServletRequest) {
        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private boolean existsByUsername(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
        return count != null && count > 0;
    }

    private Optional<UserEntity> findByUsername(String username) {
        return Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1")));
    }

    private AuthUserResponse toResponse(UserEntity user) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getNickname());
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeNickname(String nickname, String username) {
        if (nickname == null || nickname.isBlank()) {
            return username;
        }
        return nickname.trim();
    }
}
