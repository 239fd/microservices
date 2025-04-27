package by.bsuir.warehouseservice.repository;

import by.bsuir.warehouseservice.entity.Cell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CellRepository extends JpaRepository<Cell, Integer> {
    List<Cell> findByRackId(Integer rackId);
    void deleteByRackId(Integer rackId);

    void deleteAllByRackId(Integer id);

    List<Cell> findAllByRackId(Integer rackId);
}