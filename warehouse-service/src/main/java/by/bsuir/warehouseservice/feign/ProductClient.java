package by.bsuir.warehouseservice.feign;

import by.bsuir.warehouseservice.DTO.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductClient {

    @PostMapping("/api/product/by-cell-ids")
    List<ProductDTO> getProductsByCellIds(@RequestBody List<Integer> cellIds);
}
