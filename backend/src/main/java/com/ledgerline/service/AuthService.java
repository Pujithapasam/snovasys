package com.ledgerline.service;

import com.ledgerline.config.JwtTokenProvider;
import com.ledgerline.dto.AuthRequest;
import com.ledgerline.dto.AuthResponse;
import com.ledgerline.model.Role;
import com.ledgerline.model.User;
import com.ledgerline.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());
        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }

    @Transactional
    public AuthResponse register(String username, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User user = new User(username, passwordEncoder.encode(password), role != null ? role : Role.ACCOUNT_HOLDER);
        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());
        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }
}

