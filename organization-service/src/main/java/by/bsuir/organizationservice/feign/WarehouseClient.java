package by.bsuir.organizationservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "warehouse-service")
public interface WarehouseClient {
    @DeleteMapping("/api/warehouse/by-organization/{orgId}")
    void deleteByOrganization(@PathVariable("orgId") Integer orgId);
}