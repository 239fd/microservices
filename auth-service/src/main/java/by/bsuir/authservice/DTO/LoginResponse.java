package by.bsuir.authservice.DTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class LoginResponse {

    private EmployeeDto user;
    private String accessToken;
    private String refreshToken;
}
