package by.bsuir.authservice.DTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class RegisterRequest {

    private String login;
    private String password;
    private String firstName;
    private String phone;
    private String secondName;
    private String title;
    private String surname;
    private String code;
    private Long organizationId;
    private Long warehouseId;

}
