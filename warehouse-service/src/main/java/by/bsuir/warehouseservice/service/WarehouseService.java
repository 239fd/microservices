package by.bsuir.warehouseservice.service;

import by.bsuir.warehouseservice.DTO.*;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
public interface WarehouseService {
    boolean findById(int id);
    void deleteByOrganization(Integer organizationId);
    WarehouseDTO createWarehouse(CreateWarehouseRequest request, String dirLogin) throws AccessDeniedException;
    WarehouseDTO updateWarehouse(Integer id, UpdateWarehouseRequest request, String dirLogin) throws AccessDeniedException;
    void deleteWarehouse(Integer id, String dirLogin) throws AccessDeniedException;
    WarehouseDTO getById(Integer id, String dirLogin) throws AccessDeniedException;
    List<WarehouseDTO> findAllByUserLogin(String dirLogin);
    RackDTO createRack(Integer warehouseId, CreateRackRequest request, String dirLogin);
    RackDTO updateRack(Integer warehouseId, Integer rackId, UpdateRackRequest request, String dirLogin);
    void deleteRack(Integer warehouseId, Integer rackId, String dirLogin);
    CellDTO createCell(Integer warehouseId, Integer rackId, CreateCellRequest request, String dirLogin);
    CellDTO updateCell(Integer warehouseId, Integer rackId, Integer cellId, UpdateCellRequest request, String dirLogin);
    void deleteCell(Integer warehouseId, Integer rackId, Integer cellId, String dirLogin);
}
