package by.bsuir.employeeservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private String login;

    private String password;

    private String title;

    private String phone;

    private String firstName;

    private String secondName;

    private String surname;

    private Integer warehouseId;

    private Integer organizationId;

}
