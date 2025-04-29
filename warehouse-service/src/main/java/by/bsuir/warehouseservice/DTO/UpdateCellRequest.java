package by.bsuir.warehouseservice.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCellRequest {
    private Double length;
    private Double width;
    private Double height;
}
