package by.bsuir.warehouseservice.service;

import by.bsuir.warehouseservice.DTO.CreateWarehouseRequest;
import by.bsuir.warehouseservice.DTO.UpdateWarehouseRequest;
import by.bsuir.warehouseservice.DTO.WarehouseDTO;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
public interface WarehouseService {
    boolean findById(int id);
    void deleteByOrganization(Integer organizationId);
    WarehouseDTO create(CreateWarehouseRequest request, String dirLogin) throws AccessDeniedException;
    WarehouseDTO update(Integer id, UpdateWarehouseRequest request, String dirLogin) throws AccessDeniedException;
    void delete(Integer id, String dirLogin) throws AccessDeniedException;
    WarehouseDTO getById(Integer id, String dirLogin) throws AccessDeniedException;
    List<WarehouseDTO> findAllByUserLogin(String dirLogin);
}
