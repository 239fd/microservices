package by.bsuir.employeeservice.repository;

import by.bsuir.employeeservice.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByLogin(String login);
    boolean existsByLogin(String login);

    List<Employee> findByOrganizationId(Integer organizationId);
    void deleteByOrganizationId(Integer organizationId);

    List<Employee> findAllByWarehouseId(Integer warehouseId);

    void deleteByWarehouseId(Integer warehouseId);
}
