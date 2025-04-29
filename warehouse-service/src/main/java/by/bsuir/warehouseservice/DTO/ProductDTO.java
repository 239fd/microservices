package by.bsuir.warehouseservice.DTO;


import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO {
    private int id;
    private String name;
    private String unit;
    private double price;
    private int amount;
    private double height;
    private double width;
    private double length;
    private double weight;
    private LocalDate bestBeforeDate;
    private Integer warehouseId;
    private Integer cellId;
    private String warehouseName;
}