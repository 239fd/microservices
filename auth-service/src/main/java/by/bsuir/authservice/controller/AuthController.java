package by.bsuir.authservice.controller;

import by.bsuir.authservice.DTO.*;
import by.bsuir.authservice.exception.AppException;
import by.bsuir.authservice.feign.EmployeeClient;
import by.bsuir.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request);
        ApiResponse<LoginResponse> responseBody = ApiResponse.<LoginResponse>builder()
                .status(true)
                .message("Logged in")
                .data(tokens)
                .build();
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestBody TokenRefreshRequest request) {
        JwtResponse tokens = authService.refreshToken(request.getRefreshToken());
        ApiResponse<JwtResponse> responseBody = ApiResponse.<JwtResponse>builder()
                .status(true)
                .message("Token refreshed")
                .data(tokens)
                .build();
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> register(@RequestBody RegisterRequest request) {

        JwtResponse tokens = authService.register(request);
        return ResponseEntity.ok(ApiResponse.<JwtResponse>builder()
                .status(true)
                .message("Регистрация успешна")
                .data(tokens)
                .build());
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        ApiResponse<Void> responseBody = ApiResponse.<Void>builder()
                .status(true)
                .message("Logged out")
                .build();
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        UserDetails user = (UserDetails) authentication.getPrincipal();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("login", user.getUsername());
        userInfo.put("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<String> oauth2Success() {
        return ResponseEntity.ok("OAuth2 login successful!");
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<ApiResponse<JwtResponse>> completeProfile(@RequestBody RegisterRequest request) {
        try {
            JwtResponse jwtResponse = authService.completeOAuthRegistration(request);
            return ResponseEntity.ok(ApiResponse.<JwtResponse>builder()
                    .status(true)
                    .message("Регистрация завершена")
                    .data(jwtResponse)
                    .build());
        } catch (AppException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<JwtResponse>builder()
                    .status(false)
                    .message(ex.getMessage())
                    .build());
        }
    }

}
