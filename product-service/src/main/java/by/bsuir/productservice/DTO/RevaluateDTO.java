package by.bsuir.productservice.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevaluateDTO {
    private List<Integer> productIds;
    private List<Double> newPrice;
}
