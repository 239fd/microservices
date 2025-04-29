package by.bsuir.warehouseservice.feign;

import by.bsuir.warehouseservice.DTO.ApiResponse;
import by.bsuir.warehouseservice.DTO.EmployeeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "employee-service")
public interface EmployeeClient {

    @GetMapping("/api/employees/by-login")
    ApiResponse<EmployeeDto> getEmployee(@RequestParam("login") String login);

    @PutMapping("/api/employees/assign-organization/{orgId}")
    void assignOrganization(@RequestParam("employeeLogin") String login,
                            @PathVariable("orgId") Integer orgId);

    @PutMapping("/api/employees/unassign-warehouse/{warehouseId}")
    void unassignWarehouseByWarehouseId(@PathVariable("warehouseId") Integer warehouseId);

}