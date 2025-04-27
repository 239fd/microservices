package by.bsuir.authservice.DTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
@RequiredArgsConstructor
public class EmployeeDto {
    private UUID id;
    private String login;
    private String encodedPassword;
    private String title;
    private String firstName;
    private String secondName;
    private Integer warehouseId;
    private Integer organizationId;
}
