package by.bsuir.employeeservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("warehouse-service")
public interface WarehouseClient {

    @GetMapping("/api/warehouse/by-id")
    boolean getById(@RequestParam int id);


}
