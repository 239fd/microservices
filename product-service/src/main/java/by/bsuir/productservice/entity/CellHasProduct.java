package by.bsuir.productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(name = "cell_has_product")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class CellHasProduct {
    @EmbeddedId
    private CellHasProductKey id;

    @Column(name = "barcode_pdf")
    private String barcodePdf;

    public Integer getProductId() {
        return id.getProductId();
    }

}