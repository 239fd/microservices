package by.bsuir.organizationservice.feign;

import by.bsuir.organizationservice.DTO.ApiResponse;
import by.bsuir.organizationservice.DTO.EmployeeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "employee-service")
public interface EmployeeClient {

    @GetMapping("/api/employees/by-login")
    ApiResponse<EmployeeDto> getByLogin(@RequestParam("login") String employeeLogin);

    @PutMapping("/api/employees/assign-organization/{orgId}")
    void assignOrganization(@RequestParam("employeeLogin") String employeeLogin,
                             @PathVariable("orgId") Integer organizationId);

    @DeleteMapping("/api/employees/by-organization/{orgId}")
    void deleteByOrganization(@PathVariable("orgId") Integer organizationId);
}