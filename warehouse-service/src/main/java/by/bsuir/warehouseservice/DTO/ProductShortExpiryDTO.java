package by.bsuir.warehouseservice.DTO;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProductShortExpiryDTO {
    private Integer productId;
    private String name;
    private LocalDate bestBeforeDate;
    private int daysLeft;
    private Integer warehouseId;
    private String warehouseName;
}
