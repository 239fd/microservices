package by.bsuir.employeeservice.service.Impl;

import by.bsuir.employeeservice.DTO.EmployeeDto;
import by.bsuir.employeeservice.DTO.OrganizationSearchDTO;
import by.bsuir.employeeservice.DTO.RegisterRequest;
import by.bsuir.employeeservice.entity.Employee;
import by.bsuir.employeeservice.exception.AppException;
import by.bsuir.employeeservice.feign.OrganizationClient;
import by.bsuir.employeeservice.feign.WarehouseClient;
import by.bsuir.employeeservice.repository.EmployeeRepository;
import by.bsuir.employeeservice.service.EmployeeService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationClient organizationClient;
    private final WarehouseClient warehouseClient;


    @Override
    public Employee findByLogin(String login) {
        return employeeRepository.findByLogin(login)
                .orElseThrow(() -> new AppException("Employee not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public boolean existsByLogin(String login) {
        return employeeRepository.existsByLogin(login);
    }

    @Override
    @Transactional
    public void create(RegisterRequest dto) {

        OrganizationSearchDTO organizationSearchDTO = new OrganizationSearchDTO();
        organizationSearchDTO.setInn(String.valueOf(dto.getOrganizationId()));

        if(!dto.getTitle().equalsIgnoreCase("director")){
            System.out.println(organizationClient.getOrganizationById(dto.getOrganizationId()));
            if(organizationClient.getOrganizationById(dto.getOrganizationId()) == null) {
                throw new AppException("Organization not found", HttpStatus.NOT_FOUND);
            }
            if(!warehouseClient.getById(dto.getWarehouseId())){
                throw new AppException("Warehouse not found", HttpStatus.NOT_FOUND);
            }
        }

        Employee employee = new Employee();
        employee.setLogin(dto.getLogin());
        employee.setFirstName(dto.getFirstName());
        employee.setSecondName(dto.getSecondName());
        employee.setSurname(dto.getSurname());
        employee.setPhone(dto.getPhone());
        employee.setTitle(dto.getTitle());
        employee.setPassword(dto.getPassword());
        employee.setOrganizationId(dto.getOrganizationId());
        employee.setWarehouseId(dto.getWarehouseId());

        employeeRepository.save(employee);
    }

    @Override
    public EmployeeDto getEmployee(String id) {
        Employee emp = employeeRepository.findByLogin(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        return new EmployeeDto(emp.getId(), emp.getTitle().toLowerCase(), emp.getOrganizationId());
    }

    @Override
    public void assignOrganization(String id, Integer organizationId) {
        Employee emp = employeeRepository.findByLogin(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        emp.setOrganizationId(organizationId);
        employeeRepository.save(emp);
    }

    @Override
    @Transactional
    public void deleteByOrganization(Integer organizationId) {
        employeeRepository.deleteByOrganizationId(organizationId);
    }

    @Override
    public void assignWarehouse(String employeeLogin, Integer warehouseId) {
        Employee emp = employeeRepository.findByLogin(employeeLogin)
                .orElseThrow(() -> new AppException("Employee not found", HttpStatus.NOT_FOUND));
        emp.setWarehouseId(warehouseId);
        employeeRepository.save(emp);
    }

    @Override
    public void unassignWarehouseByWarehouseId(Integer warehouseId) {
        employeeRepository.findAllByWarehouseId(warehouseId)
                .forEach(emp -> {
                    emp.setWarehouseId(null);
                    employeeRepository.save(emp);
                });
    }

    @Override
    public void deleteByWarehouse(Integer warehouseId) {
        employeeRepository.deleteByWarehouseId(warehouseId);
    }
}
