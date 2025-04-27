package by.bsuir.productservice.feign;

import by.bsuir.productservice.DTO.WarehouseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "warehouse-service")
public interface WarehouseClient {

    @GetMapping("/api/warehouse/by-user")
    List<WarehouseDTO> getByUser(@RequestParam String username);
}
