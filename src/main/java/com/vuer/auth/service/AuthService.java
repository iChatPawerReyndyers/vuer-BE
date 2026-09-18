package com.vuer.auth.service;

import com.vuer.auth.dto.AuthResponse;
import com.vuer.auth.dto.LoginRequest;
import com.vuer.auth.dto.RegisterRequest;
import com.vuer.common.exception.ResourceNotFoundException;
import com.vuer.user.entity.User;
import com.vuer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();
        
        userRepository.save(user);
        
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return new AuthResponse(token, refreshToken, jwtService.getExpirationTime());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return new AuthResponse(token, refreshToken, jwtService.getExpirationTime());
    }

    public AuthResponse refreshToken(String token) {
        String email = jwtService.extractUsername(token);
        if (email != null && jwtService.isTokenValid(token, email)) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            String newToken = jwtService.generateToken(user);
            return new AuthResponse(newToken, token, jwtService.getExpirationTime());
        }
        throw new IllegalArgumentException("Invalid refresh token");
    }
}