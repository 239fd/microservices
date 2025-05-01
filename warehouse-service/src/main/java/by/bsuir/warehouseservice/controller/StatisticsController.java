package by.bsuir.warehouseservice.controller;

import by.bsuir.warehouseservice.DTO.ProductShortExpiryDTO;
import by.bsuir.warehouseservice.DTO.WarehouseAnalyticsDTO;
import by.bsuir.warehouseservice.service.StatisticsService;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/warehouses")
    @RolesAllowed("ROLE_DIRECTOR")
    public ResponseEntity<List<WarehouseAnalyticsDTO>> getWarehouseAnalytics(Principal principal) {
        List<WarehouseAnalyticsDTO> analytics = statisticsService.getWarehouseAnalytics(principal.getName());
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/products/expiring")
    @RolesAllowed("ROLE_DIRECTOR")
    public ResponseEntity<List<ProductShortExpiryDTO>> getExpiringProducts(
            Principal principal,
            @RequestParam(defaultValue = "7") int daysAhead) {
        List<ProductShortExpiryDTO> products = statisticsService.getExpiringProducts(principal.getName(), daysAhead);
        return ResponseEntity.ok(products);
    }
}
