package by.bsuir.authservice.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemporaryOAuthUser {
    private String email;
    private String fullName;
    private String provider;
}
