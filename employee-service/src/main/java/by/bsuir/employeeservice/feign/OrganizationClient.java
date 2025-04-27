package by.bsuir.employeeservice.feign;

import by.bsuir.employeeservice.DTO.OrganizationDTO;
import by.bsuir.employeeservice.DTO.OrganizationSearchDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("organization-service")
public interface OrganizationClient {

    @PostMapping("/api/organization/by-inn")
    boolean getByInn(@RequestBody OrganizationSearchDTO organizationSearchDTO);

    @GetMapping("/api/organization/id")
    OrganizationDTO getOrganizationById(@RequestParam int id);
}
