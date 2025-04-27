package by.bsuir.productservice.DTO;

import lombok.*;

import java.util.List;

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