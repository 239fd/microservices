package by.bsuir.warehouseservice.repository;

import by.bsuir.warehouseservice.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    List<Warehouse> findByOrganizationId(Integer organizationId);
    void deleteByOrganizationId(Integer organizationId);
    List<Warehouse> findAllByOrganizationId(Integer organizationId);

    Warehouse getWarehouseById(int id);
}
