package by.bsuir.productservice.entity;

import by.bsuir.productservice.DTO.ProductDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "height", nullable = false)
    private Double height;

    @Column(name = "length", nullable = false)
    private Double length;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "width", nullable = false)
    private Double width;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "bestbeforedate")
    private LocalDate bestBeforeDate;
}
