package by.bsuir.productservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CellHasProductKey implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "cell_id")
    private Integer cellId;

    @Column(name = "product_id")
    private Integer productId;
}
