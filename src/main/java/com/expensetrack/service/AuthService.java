package com.expensetrack.service;

import com.expensetrack.domain.User;
import com.expensetrack.dto.auth.AuthResponse;
import com.expensetrack.dto.auth.LoginRequest;
import com.expensetrack.dto.auth.RegisterRequest;
import com.expensetrack.exception.DuplicateEmailException;
import com.expensetrack.repository.UserRepository;
import com.expensetrack.security.JwtTokenProvider;
import java.time.LocalDateTime;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().trim())) {
            throw new DuplicateEmailException("Email is already registered");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim())
                .password(passwordEncoder.encode(request.password()))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));

        String token = jwtTokenProvider.generateToken(authentication);
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getName());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getName());
    }
}
