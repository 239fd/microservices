package by.bsuir.warehouseservice.DTO;

import lombok.Data;

@Data
public class WarehouseAnalyticsDTO {
    private Integer warehouseId;
    private String warehouseName;
    private int rackCount;
    private int cellCount;
    private int filledCellCount;
    private double averageRackCapacity;
    private int productCount;
    private double totalProductValue;
}
