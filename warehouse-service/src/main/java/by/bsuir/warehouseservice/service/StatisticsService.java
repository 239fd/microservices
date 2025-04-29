package by.bsuir.warehouseservice.service;


import by.bsuir.warehouseservice.DTO.ProductShortExpiryDTO;
import by.bsuir.warehouseservice.DTO.WarehouseAnalyticsDTO;

import java.util.List;

public interface StatisticsService {
    List<WarehouseAnalyticsDTO> getWarehouseAnalytics(String directorLogin);
    List<ProductShortExpiryDTO> getExpiringProducts(String directorLogin, int daysThreshold);
}
