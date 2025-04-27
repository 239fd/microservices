package by.bsuir.employeeservice.DTO;

import lombok.*;

@Getter
@Builder
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    private int id;
    private String inn;
    private String name;
    private String address;
}
