package by.bsuir.employeeservice.DTO;

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
    private Integer organizationId;
    private Integer warehouseId;


}
