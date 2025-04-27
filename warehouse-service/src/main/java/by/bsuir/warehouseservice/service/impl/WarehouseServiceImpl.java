package by.bsuir.warehouseservice.service.impl;

import by.bsuir.warehouseservice.DTO.*;
import by.bsuir.warehouseservice.entity.Cell;
import by.bsuir.warehouseservice.entity.Rack;
import by.bsuir.warehouseservice.entity.Warehouse;
import by.bsuir.warehouseservice.feign.EmployeeClient;
import by.bsuir.warehouseservice.repository.CellRepository;
import by.bsuir.warehouseservice.repository.RackRepository;
import by.bsuir.warehouseservice.repository.WarehouseRepository;
import by.bsuir.warehouseservice.service.WarehouseService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final RackRepository rackRepository;
    private final CellRepository cellRepository;
    private final EmployeeClient employeeClient;

    @Override
    public boolean findById(int id) {
        return warehouseRepository.findById(id).isPresent();
    }

    @Override
    @Transactional
    public void deleteByOrganization(Integer organizationId) {
        List<Warehouse> list = warehouseRepository.findByOrganizationId(organizationId);
        for (Warehouse w : list) {
            List<Rack> racks = rackRepository.findByWarehouseId(w.getId());
            for (Rack r : racks) {
                cellRepository.deleteByRackId(r.getId());
            }
            rackRepository.deleteByWarehouseId(w.getId());
        }
        warehouseRepository.deleteByOrganizationId(organizationId);
    }

    private void checkDirectorOrg(Integer orgId, String login) throws AccessDeniedException {
        EmployeeDto emp = employeeClient.getEmployee(login);
        if (!emp.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Only director of this organization");
        }
    }

    @Override
    public WarehouseDTO create(CreateWarehouseRequest request, String dirLogin) throws AccessDeniedException {
        checkDirectorOrg(request.getOrganizationId(), dirLogin);
        Warehouse w = Warehouse.builder()
                .name(request.getName())
                .address(request.getAddress())
                .organizationId(request.getOrganizationId())
                .build();
        warehouseRepository.save(w);
        for (int i = 0; i < request.getRackCount(); i++) {
            Rack r = Rack.builder()
                    .capacity(request.getRackCapacity())
                    .warehouseId(w.getId())
                    .build();
            rackRepository.save(r);
            for (int j = 0; j < request.getCellsPerRack(); j++) {
                Cell c = Cell.builder()
                        .length(request.getCellLength())
                        .width(request.getCellWidth())
                        .height(request.getCellHeight())
                        .rackId(r.getId())
                        .build();
                cellRepository.save(c);
            }
        }
        return getById(w.getId(), dirLogin);
    }

    @Override
    public WarehouseDTO update(Integer id, UpdateWarehouseRequest request, String dirLogin) throws AccessDeniedException {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        checkDirectorOrg(w.getOrganizationId(), dirLogin);
        w.setName(request.getName());
        w.setAddress(request.getAddress());
        warehouseRepository.save(w);
        return getById(id, dirLogin);
    }

    @Override
    @Transactional
    public void delete(Integer id, String dirLogin) throws AccessDeniedException {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        checkDirectorOrg(w.getOrganizationId(), dirLogin);

        employeeClient.unassignWarehouseByWarehouseId(id);

        rackRepository.findAllByWarehouseId(id).forEach(r -> {
            cellRepository.deleteAllByRackId(r.getId());
        });
        rackRepository.deleteAllByWarehouseId(id);
        warehouseRepository.delete(w);
    }

    @Override
    public WarehouseDTO getById(Integer id, String dirLogin) throws AccessDeniedException {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        checkDirectorOrg(w.getOrganizationId(), dirLogin);

        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(w.getId());
        dto.setName(w.getName());
        dto.setAddress(w.getAddress());
        dto.setOrganizationId(w.getOrganizationId());

        List<RackDTO> racks = new ArrayList<>();
        rackRepository.findAllByWarehouseId(id).forEach(r -> {
            RackDTO rd = new RackDTO();
            rd.setId(r.getId());
            rd.setCapacity(r.getCapacity());
            List<CellDTO> cells = new ArrayList<>();
            cellRepository.findAllByRackId(r.getId()).forEach(c -> {
                CellDTO cd = new CellDTO();
                cd.setId(c.getId());
                cd.setLength(c.getLength());
                cd.setWidth(c.getWidth());
                cd.setHeight(c.getHeight());
                cells.add(cd);
            });
            rd.setCells(cells);
            racks.add(rd);
        });
        dto.setRacks(racks);
        return dto;
    }

    @Override
    @Transactional
    public List<WarehouseDTO> findAllByUserLogin(String dirLogin) {

        System.out.println("dirLogin: " + dirLogin);
        EmployeeDto emp = employeeClient.getEmployee(dirLogin);
        Integer orgId = emp.getOrganizationId();
        System.out.println("orgId: " + orgId);

        List<Warehouse> warehouses = warehouseRepository.findAllByOrganizationId(orgId);

        return warehouses.stream()
                .map(w -> {
                    try {
                        return getById(w.getId(), dirLogin);
                    } catch (AccessDeniedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
