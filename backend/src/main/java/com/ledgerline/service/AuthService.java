package com.ledgerline.service;

import com.ledgerline.config.JwtTokenProvider;
import com.ledgerline.dto.AuthRequest;
import com.ledgerline.dto.AuthResponse;
import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.model.AccountType;
import com.ledgerline.model.Role;
import com.ledgerline.model.User;
import com.ledgerline.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AccountService accountService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       @Lazy AccountService accountService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.accountService = accountService;
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
        RegisterRequest req = new RegisterRequest(username, password, null, null, role, new BigDecimal("1000.00"));
        return registerUser(req);
    }

    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        String username = request.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ACCOUNT_HOLDER;
        User user = new User(
                username,
                passwordEncoder.encode(request.getPassword()),
                request.getEmail(),
                request.getFullName(),
                role
        );
        user = userRepository.save(user);

        // Auto-provision initial primary checking account for newly onboarded account holders
        if (role == Role.ACCOUNT_HOLDER) {
            BigDecimal initialDeposit = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;
            accountService.createAccount(user.getUsername(), "Primary Checking", initialDeposit, AccountType.CUSTOMER_CHECKING);
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());
        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }
}
