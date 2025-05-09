package by.bsuir.warehouseservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRackRequest {
    private Integer capacity;
    private List<CreateCellRequest> cells;
}
