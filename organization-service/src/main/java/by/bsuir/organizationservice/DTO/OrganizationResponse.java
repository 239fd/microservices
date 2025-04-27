package by.bsuir.organizationservice.DTO;

import lombok.*;

@Getter
@Builder
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    private Integer id;
    private String inn;
    private String name;
    private String address;
}