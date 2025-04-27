package by.bsuir.warehouseservice.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateWarehouseRequest {
    private String name;
    private String address;
}