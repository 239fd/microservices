package by.bsuir.authservice.service;

import by.bsuir.authservice.DTO.ApiResponse;
import by.bsuir.authservice.DTO.EmployeeDto;
import by.bsuir.authservice.DTO.JwtResponse;
import by.bsuir.authservice.DTO.TemporaryOAuthUser;
import by.bsuir.authservice.feign.EmployeeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final EmployeeClient employeeClient;

    @Value("${jwt.refresh.expiration-ms}")
    private long refreshExpirationMs;

    @Value("${oauth2.temp-user-expiration-ms:300000}")
    private long tempUserExpirationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        String email, firstName, lastName;

        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            firstName = (String) attributes.get("given_name");
            lastName = (String) attributes.get("family_name");
        } else if ("yandex".equals(registrationId)) {
            email = (String) attributes.get("default_email");
            firstName = (String) attributes.get("first_name");
            lastName = (String) attributes.get("last_name");
        } else {
            throw new RuntimeException("Unknown OAuth2 provider: " + registrationId);
        }

        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        var employee = getEmployeeIfExists(email);

        if (employee == null) {

            TemporaryOAuthUser tempUser = new TemporaryOAuthUser(email, fullName, registrationId);
            String json = objectMapper.writeValueAsString(tempUser);
            redisTemplate.opsForValue().set("oauth-temp:" + email, json, tempUserExpirationMs, TimeUnit.MILLISECONDS);

            UserDetails tempDetails = new org.springframework.security.core.userdetails.User(
                    email, "", List.of(new SimpleGrantedAuthority("ROLE_TEMP"))
            );
            Authentication tempAuth = new UsernamePasswordAuthenticationToken(tempDetails, null, tempDetails.getAuthorities());
            String accessToken = jwtUtil.generateAccessToken(tempAuth);

            Map<String, Object> body = new HashMap<>();
            body.put("status", true);
            body.put("message", "OAuth2 авторизация успешна, необходимо завершить регистрацию.");
            body.put("needProfileCompletion", true);
            body.put("email", email);
            body.put("accessToken", accessToken);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                employee.getLogin(),
                "",
                List.of(new SimpleGrantedAuthority(employee.getTitle()))
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String accessToken = jwtUtil.generateAccessToken(auth);
        String refreshToken = jwtUtil.generateRefreshToken(auth);
        redisTemplate.opsForValue().set(refreshToken, employee.getLogin(), refreshExpirationMs, TimeUnit.MILLISECONDS);

        JwtResponse jwtResponse = new JwtResponse(accessToken, refreshToken);
        ApiResponse<JwtResponse> responseBody = ApiResponse.<JwtResponse>builder()
                .status(true)
                .message("OAuth2 успешно, токены выданы")
                .data(jwtResponse)
                .build();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }

    private EmployeeDto getEmployeeIfExists(String login) {
        try {
            return employeeClient.getByLogin(login);
        } catch (Exception e) {
            return null;
        }
    }
}
