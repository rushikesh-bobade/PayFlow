package com.payflow.service;

import com.payflow.dto.request.LoginRequest;
import com.payflow.dto.request.RegisterRequest;
import com.payflow.dto.response.AuthResponse;
import com.payflow.entity.Role;
import com.payflow.entity.User;
import com.payflow.entity.Wallet;
import com.payflow.exception.DuplicateRequestException;
import com.payflow.exception.ResourceNotFoundException;
import com.payflow.repository.UserRepository;
import com.payflow.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateRequestException("Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.ROLE_USER)
                .build();
        User savedUser = userRepository.save(user);

        // Provision active digital wallet automatically for the new user
        Wallet wallet = walletService.createWalletForUser(savedUser);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", savedUser.getId());
        extraClaims.put("role", savedUser.getRole().name());
        extraClaims.put("walletNumber", wallet.getWalletNumber());

        String jwtToken = jwtService.generateToken(savedUser, extraClaims);

        log.info("New user registered successfully: {} with wallet {}", savedUser.getEmail(), wallet.getWalletNumber());

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .walletNumber(wallet.getWalletNumber())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + email));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("walletNumber", wallet.getWalletNumber());

        String jwtToken = jwtService.generateToken(user, extraClaims);

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .walletNumber(wallet.getWalletNumber())
                .build();
    }
}
