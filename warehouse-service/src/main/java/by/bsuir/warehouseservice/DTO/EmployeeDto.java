package by.bsuir.warehouseservice.DTO;

import lombok.*;

import java.util.UUID;


@Getter
@Builder
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
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
