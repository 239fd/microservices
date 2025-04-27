package by.bsuir.warehouseservice.DTO;

import java.util.List;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RackDTO {
    private Integer id;
    private Integer capacity;
    private List<CellDTO> cells;
}