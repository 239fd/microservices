package by.bsuir.warehouseservice.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WarehouseDTO {
    private Integer id;
    private String name;
    private String address;
    private Integer organizationId;
    private List<RackDTO> racks;
}