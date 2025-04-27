package by.bsuir.productservice.DTO;


import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DispatchDTO {
    private List<Integer> productIds;
    private List<Integer> amounts;
    private String customerName;
    private String customerUnp;
    private String customerAddress;
    private String driverFullName;
    private String carNumber;
    private String documentType;
    private String address;
}