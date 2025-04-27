package by.bsuir.warehouseservice.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CellDTO {
    private Integer id;
    private Double length;
    private Double width;
    private Double height;
}
