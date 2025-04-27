package by.bsuir.authservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("organization-service")
public interface OrganizationClient {

    @GetMapping("/api/organization/inn")
    Integer getOrganizationIdByINN(@RequestParam("inn") String inn);

}
