package by.bsuir.productservice.feign;

import by.bsuir.productservice.DTO.ApiResponse;
import by.bsuir.productservice.DTO.EmployeeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "employee-service")
public interface EmployeeClient {

    @GetMapping("/api/employees/by-login")
    ApiResponse<EmployeeDto> getByLogin(@RequestParam("login") String employeeLogin);
}