package by.bsuir.employeeservice.controller;

import by.bsuir.employeeservice.DTO.ApiResponse;
import by.bsuir.employeeservice.DTO.EmployeeDto;
import by.bsuir.employeeservice.DTO.RegisterRequest;
import by.bsuir.employeeservice.entity.Employee;
import by.bsuir.employeeservice.mapper.EmployeeMapper;
import by.bsuir.employeeservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/by-login")
    public ResponseEntity<EmployeeDto> getByLogin(@RequestParam String login) {
        Employee employee = employeeService.findByLogin(login);
        EmployeeDto dto = EmployeeDto
                .builder()
                .id(employee.getId())
                .login(employee.getLogin())
                .firstName(employee.getFirstName())
                .secondName(employee.getSecondName())
                .encodedPassword(employee.getPassword())
                .title(employee.getTitle())
                .organizationId(employee.getOrganizationId())
                .warehouseId(employee.getWarehouseId())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<Void>> createEmployee(@RequestBody RegisterRequest dto) {
        employeeService.create(dto);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .status(true)
                .message("Employee created successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/director")
    public ResponseEntity<ApiResponse<Void>> createDirector(@RequestBody RegisterRequest dto) {
        employeeService.create(dto);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .status(true)
                .message("Director created successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(apiResponse);
    }


    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByLogin(@RequestParam("login") String login) {
        return ResponseEntity.ok(employeeService.existsByLogin(login));
    }


    @PutMapping("/assign-organization/{orgId}")
    public ResponseEntity<Void> assignOrganization(@RequestParam String employeeLogin ,
                                                   @PathVariable Integer orgId) {
        employeeService.assignOrganization(employeeLogin, orgId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-organization/{orgId}")
    public ResponseEntity<Void> deleteByOrganization(@PathVariable Integer orgId) {
        employeeService.deleteByOrganization(orgId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/assign-warehouse/{warehouseId}")
    public ResponseEntity<Void> assignWarehouse(@RequestParam("employeeLogin") String employeeLogin,
                                                @PathVariable Integer warehouseId) {
        employeeService.assignWarehouse(employeeLogin, warehouseId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/unassign-warehouse/{warehouseId}")
    public ResponseEntity<Void> unassignWarehouseByWarehouseId(@PathVariable Integer warehouseId) {
        employeeService.unassignWarehouseByWarehouseId(warehouseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-warehouse/{warehouseId}")
    public ResponseEntity<Void> deleteByWarehouse(@PathVariable Integer warehouseId) {
        employeeService.deleteByWarehouse(warehouseId);
        return ResponseEntity.noContent().build();
    }
}

