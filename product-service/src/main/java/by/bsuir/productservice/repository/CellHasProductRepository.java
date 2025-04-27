package by.bsuir.productservice.repository;

import by.bsuir.productservice.entity.CellHasProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CellHasProductRepository extends JpaRepository<CellHasProduct, Integer> {
    @Query("SELECT c FROM CellHasProduct c WHERE c.id.cellId = :cellId")
    List<CellHasProduct> findAllByCellId(@Param("cellId") Integer cellId);

    List<CellHasProduct> findAllByIdProductId(Integer productId);
}

