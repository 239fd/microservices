package by.bsuir.employeeservice.service;

import by.bsuir.employeeservice.DTO.EmployeeDto;
import by.bsuir.employeeservice.DTO.RegisterRequest;
import by.bsuir.employeeservice.entity.Employee;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface EmployeeService {
    Employee findByLogin(String login);

    boolean existsByLogin(String login);

    void create(RegisterRequest dto);

    EmployeeDto getEmployee(String id);
    void assignOrganization(String id, Integer organizationId);
    void deleteByOrganization(Integer organizationId);

    void assignWarehouse(String employeeLogin, Integer warehouseId);

    void unassignWarehouseByWarehouseId(Integer warehouseId);

    void deleteByWarehouse(Integer warehouseId);

}