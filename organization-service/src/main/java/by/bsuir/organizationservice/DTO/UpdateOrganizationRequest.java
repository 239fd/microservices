package by.bsuir.organizationservice.DTO;

import lombok.*;

@Getter
@Builder
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationRequest {
    private String name;
    private String address;
    // getters and setters
}