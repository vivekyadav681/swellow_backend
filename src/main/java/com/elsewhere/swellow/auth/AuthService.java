package com.elsewhere.swellow.auth;

import com.elsewhere.swellow.common.CryptoUtil;
import com.elsewhere.swellow.config.JwtService;
import com.elsewhere.swellow.profile.Profile;
import com.elsewhere.swellow.user.User;
import com.elsewhere.swellow.user.UserRepository;
import com.elsewhere.swellow.wallet.Wallet;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 1. Create User
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .createdAt(Instant.now())
                .build();

        // 2. Create Profile
        String displayName = request.getDisplayName() != null && !request.getDisplayName().isBlank()
                ? request.getDisplayName() : request.getUsername();
        Profile profile = Profile.builder()
                .user(user)
                .displayName(displayName)
                .bio(request.getBio())
                .avatarUrl(request.getAvatarUrl())
                .build();
        user.setProfile(profile);

        // 3. Create Wallet with EC KeyPair
        CryptoUtil.KeyPairStrings keys = CryptoUtil.generateKeyPair();
        Wallet wallet = Wallet.builder()
                .user(user)
                .cashBalance(new BigDecimal("1000.0000")) // default 1000 USD
                .swlBalance(BigDecimal.ZERO)
                .publicKey(keys.getPublicKey())
                .privateKey(keys.getPrivateKey())
                .walletType(com.elsewhere.swellow.wallet.WalletType.USER)
                .build();
        user.setWallet(wallet);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }
}
