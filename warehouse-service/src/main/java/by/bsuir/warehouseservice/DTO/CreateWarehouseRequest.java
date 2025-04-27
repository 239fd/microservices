package by.bsuir.warehouseservice.DTO;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateWarehouseRequest {
    private String name;
    private String address;
    private Integer organizationId;
    private Integer rackCount;
    private Integer rackCapacity;
    private Integer cellsPerRack;
    private Double cellLength;
    private Double cellWidth;
    private Double cellHeight;
}