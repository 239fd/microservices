package by.bsuir.warehouseservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cell")
public class Cell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Double length;

    @Column(nullable = false)
    private Double width;

    private Double height;

    @Column(name = "rack_id", nullable = false)
    private Integer rackId;

}