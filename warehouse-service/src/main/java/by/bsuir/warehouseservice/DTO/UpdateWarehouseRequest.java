package by.bsuir.warehouseservice.DTO;

import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWarehouseRequest {
    private String name;
    private String address;
}
