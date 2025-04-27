package by.bsuir.warehouseservice.repository;

import by.bsuir.warehouseservice.entity.Rack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RackRepository extends JpaRepository<Rack, Integer> {
    List<Rack> findByWarehouseId(Integer warehouseId);
    void deleteByWarehouseId(Integer warehouseId);

    List<Rack> findAllByWarehouseId(Integer warehouseId);

    void deleteAllByWarehouseId(Integer id);
}