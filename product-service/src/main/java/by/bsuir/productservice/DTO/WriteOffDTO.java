package by.bsuir.productservice.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WriteOffDTO {
    private List<Integer> productId;
    private List<Integer> quantity;
    private String reason;
    private String date;
}
