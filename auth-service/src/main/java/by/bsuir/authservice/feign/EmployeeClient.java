package by.bsuir.authservice.feign;

import by.bsuir.authservice.DTO.EmployeeDto;
import by.bsuir.authservice.DTO.RegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("employee-service")
public interface EmployeeClient {

    @GetMapping("/api/employees/by-login")
    EmployeeDto getByLogin(@RequestParam String login);

    @GetMapping("/api/employees/exists")
    boolean existsByLogin(@RequestParam("login") String login);

    @PostMapping("/api/employees")
    void createEmployee(@RequestBody RegisterRequest dto);

    @PostMapping("/api/employees/director")
    void createDirector(@RequestBody RegisterRequest dto);
}
