package by.bsuir.authservice.service;

import by.bsuir.authservice.DTO.*;
import by.bsuir.authservice.exception.AppException;
import by.bsuir.authservice.feign.EmployeeClient;
import by.bsuir.authservice.feign.OrganizationClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final EmployeeClient employeeClient;
    private final OrganizationClient organizationClient;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    @Value("${jwt.refresh.expiration-ms}")
    private long refreshExpirationMs;

    public JwtResponse login(LoginRequest request) {

        EmployeeDto employee = employeeClient.getByLogin(request.getLogin());

        if (employee == null || !passwordEncoder.matches(request.getPassword(), employee.getEncodedPassword())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        Authentication auth;
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw ex;
        }
        SecurityContextHolder.getContext().setAuthentication(auth);
        String accessToken = jwtUtil.generateAccessToken(auth);
        String refreshToken = jwtUtil.generateRefreshToken(auth);

        redisTemplate.opsForValue().set(refreshToken, auth.getName(), refreshExpirationMs, TimeUnit.MILLISECONDS);
        return new JwtResponse(accessToken, refreshToken);
    }

    public JwtResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !Boolean.TRUE.equals(redisTemplate.hasKey(refreshToken)) || !jwtUtil.validateToken(refreshToken)) {

            throw new BadCredentialsException("Invalid refresh token");
        }
        String username = redisTemplate.opsForValue().get(refreshToken);

        redisTemplate.delete(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        String newAccessToken = jwtUtil.generateAccessToken(authToken);
        String newRefreshToken = jwtUtil.generateRefreshToken(authToken);
        redisTemplate.opsForValue().set(newRefreshToken, username, refreshExpirationMs, TimeUnit.MILLISECONDS);
        return new JwtResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            redisTemplate.delete(refreshToken);
        }
    }

    public JwtResponse register(RegisterRequest request) {

        if (employeeClient.existsByLogin(request.getLogin())) {
            throw new AppException("User already exists", HttpStatus.CONFLICT);
        }

        if (!"director".equalsIgnoreCase(request.getTitle())) {
            String code = request.getCode();

            if (code == null || code.length() < 10) {
                throw new AppException("Некорректный код организации/склада", HttpStatus.BAD_REQUEST);
            }

            try {
                String orgIdStr = code.substring(0, 9);
                String whIdStr = code.substring(9);

                Long organizationId = (long) organizationClient.getOrganizationIdByINN(orgIdStr);
                Long warehouseId = Long.parseLong(whIdStr);

                request.setOrganizationId(organizationId);
                request.setWarehouseId(warehouseId);


            } catch (NumberFormatException e) {
                throw new AppException("Не удалось распарсить код организации/склада", HttpStatus.BAD_REQUEST);
            }
            RegisterRequest employeeToCreate = new RegisterRequest();
            employeeToCreate.setLogin(request.getLogin());
            employeeToCreate.setPassword(passwordEncoder.encode(request.getPassword()));
            employeeToCreate.setFirstName(request.getFirstName());
            employeeToCreate.setSecondName(request.getSecondName());
            employeeToCreate.setSurname(request.getSurname());
            employeeToCreate.setPhone(request.getPhone());
            employeeToCreate.setTitle(request.getTitle());
            employeeToCreate.setCode(request.getCode());
            employeeToCreate.setOrganizationId(request.getOrganizationId());
            employeeToCreate.setWarehouseId(request.getWarehouseId());

            employeeClient.createEmployee(employeeToCreate);
        }
        else{

            RegisterRequest employeeToCreate = new RegisterRequest();
            employeeToCreate.setLogin(request.getLogin());
            employeeToCreate.setPassword(passwordEncoder.encode(request.getPassword()));
            employeeToCreate.setFirstName(request.getFirstName());
            employeeToCreate.setSecondName(request.getSecondName());
            employeeToCreate.setSurname(request.getSurname());
            employeeToCreate.setPhone(request.getPhone());
            employeeToCreate.setTitle(request.getTitle());
            employeeToCreate.setCode(request.getCode());

            employeeClient.createDirector(employeeToCreate);
        }



        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getLogin());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtUtil.generateAccessToken(auth);
        String refreshToken = jwtUtil.generateRefreshToken(auth);

        redisTemplate.opsForValue().set(refreshToken, request.getLogin(), refreshExpirationMs, TimeUnit.MILLISECONDS);

        return new JwtResponse(accessToken, refreshToken);
    }


    public JwtResponse completeOAuthRegistration(RegisterRequest request) {
        String redisKey = "oauth-temp:" + request.getLogin();
        String tempJson = redisTemplate.opsForValue().get(redisKey);

        if (tempJson == null) {
            throw new AppException("OAuth2 временный профиль не найден или истёк", HttpStatus.BAD_REQUEST);
        }

        try {
            TemporaryOAuthUser tempUser = objectMapper.readValue(tempJson, TemporaryOAuthUser.class);

            assignNames(request, tempUser.getFullName());

            if (!"director".equalsIgnoreCase(request.getTitle())) {
                if (request.getCode() == null || request.getCode().length() < 10) {
                    throw new AppException("Некорректный код организации/склада", HttpStatus.BAD_REQUEST);
                }

                String orgIdStr = request.getCode().substring(0, 9);
                String whIdStr = request.getCode().substring(9);

                try {
                    request.setOrganizationId(Long.parseLong(orgIdStr));
                    request.setWarehouseId(Long.parseLong(whIdStr));
                } catch (NumberFormatException e) {
                    throw new AppException("Невозможно распарсить код организации/склада", HttpStatus.BAD_REQUEST);
                }
            }

            employeeClient.createEmployee(request);
            redisTemplate.delete(redisKey);

            var auth = new UsernamePasswordAuthenticationToken(
                    new User(request.getLogin(), "", List.of(new SimpleGrantedAuthority("ROLE_" + request.getTitle().toUpperCase()))),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + request.getTitle().toUpperCase()))
            );

            String accessToken = jwtUtil.generateAccessToken(auth);
            String refreshToken = jwtUtil.generateRefreshToken(auth);
            redisTemplate.opsForValue().set(refreshToken, request.getLogin(), refreshExpirationMs, TimeUnit.MILLISECONDS);

            return new JwtResponse(accessToken, refreshToken);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("Ошибка при завершении регистрации: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private void assignNames(RegisterRequest employee, String fullName) {
        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) {
            employee.setFirstName(parts[0]);
        } else if (parts.length == 2) {
            employee.setFirstName(parts[0]);
            employee.setSecondName(parts[1]);
        } else if (parts.length >= 3) {
            employee.setFirstName(parts[0]);
            employee.setSecondName(parts[1]);
            employee.setSurname(String.join(" ", List.of(parts).subList(2, parts.length)));
        }
    }


}

