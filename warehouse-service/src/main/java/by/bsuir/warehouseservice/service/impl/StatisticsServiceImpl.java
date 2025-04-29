package by.bsuir.warehouseservice.service.impl;

import by.bsuir.warehouseservice.DTO.ProductDTO;
import by.bsuir.warehouseservice.DTO.ProductShortExpiryDTO;
import by.bsuir.warehouseservice.DTO.WarehouseAnalyticsDTO;
import by.bsuir.warehouseservice.entity.*;
import by.bsuir.warehouseservice.repository.*;
import by.bsuir.warehouseservice.feign.EmployeeClient;
import by.bsuir.warehouseservice.feign.ProductClient;
import by.bsuir.warehouseservice.service.StatisticsService;
import by.bsuir.warehouseservice.DTO.EmployeeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final WarehouseRepository warehouseRepository;
    private final RackRepository rackRepository;
    private final CellRepository cellRepository;
    private final EmployeeClient employeeClient;
    private final ProductClient productClient;

    @Override
    public List<WarehouseAnalyticsDTO> getWarehouseAnalytics(String directorLogin) {
        EmployeeDto emp = employeeClient.getEmployee(directorLogin).getData();

        Integer orgId = emp.getOrganizationId();

        List<Warehouse> warehouses = warehouseRepository.findAllByOrganizationId(orgId);

        return warehouses.stream().map(warehouse -> {
            List<Rack> racks = rackRepository.findByWarehouseId(warehouse.getId());
            List<Cell> allCells = racks.stream()
                    .flatMap(rack -> cellRepository.findAllByRackId(rack.getId()).stream())
                    .toList();

            for (Cell cell : allCells) {
                System.out.println(cell);
            }
            Set<Integer> cellIds = allCells.stream().map(Cell::getId).collect(Collectors.toSet());

            List<ProductDTO> products = productClient.getProductsByCellIds(new ArrayList<>(cellIds));

            int filledCells = products.size();

            double avgCapacity = racks.isEmpty() ? 0.0 :
                    racks.stream().mapToInt(Rack::getCapacity).average().orElse(0.0);

            WarehouseAnalyticsDTO dto = new WarehouseAnalyticsDTO();
            dto.setWarehouseId(warehouse.getId());
            dto.setWarehouseName(warehouse.getName());

            dto.setRackCount(racks.size());
            dto.setCellCount(allCells.size());
            dto.setFilledCellCount(filledCells);
            dto.setAverageRackCapacity(avgCapacity);
            dto.setProductCount(products.size());
            dto.setTotalProductValue(
                    products.stream().mapToDouble(p -> p.getPrice() * p.getAmount()).sum()
            );

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProductShortExpiryDTO> getExpiringProducts(String directorLogin, int daysThreshold) {
        EmployeeDto emp = employeeClient.getEmployee(directorLogin).getData();
        Integer orgId = emp.getOrganizationId();

        List<Warehouse> warehouses = warehouseRepository.findAllByOrganizationId(orgId);
        List<Rack> racks = warehouses.stream()
                .flatMap(w -> rackRepository.findByWarehouseId(w.getId()).stream())
                .collect(Collectors.toList());

        List<Cell> cells = racks.stream()
                .flatMap(r -> cellRepository.findAllByRackId(r.getId()).stream())
                .collect(Collectors.toList());

        Set<Integer> cellIds = cells.stream().map(Cell::getId).collect(Collectors.toSet());

        List<ProductDTO> allProducts = productClient.getProductsByCellIds(new ArrayList<>(cellIds));

        LocalDate now = LocalDate.now();
        return allProducts.stream()
                .filter(p -> p.getBestBeforeDate() != null)
                .filter(p -> {
                    long days = ChronoUnit.DAYS.between(now, p.getBestBeforeDate());
                    return days <= daysThreshold && days >= 0;
                })
                .map(p -> {
                    ProductShortExpiryDTO dto = new ProductShortExpiryDTO();
                    dto.setProductId(p.getId());
                    dto.setName(p.getName());
                    dto.setBestBeforeDate(p.getBestBeforeDate());
                    dto.setDaysLeft((int) ChronoUnit.DAYS.between(now, p.getBestBeforeDate()));
                    dto.setWarehouseId(p.getWarehouseId());
                    dto.setWarehouseName(p.getWarehouseName());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
