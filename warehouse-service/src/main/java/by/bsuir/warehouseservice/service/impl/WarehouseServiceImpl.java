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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collections;
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
    @Transactional
    public WarehouseDTO createWarehouse(CreateWarehouseRequest request, String dirLogin) throws AccessDeniedException {
        checkDirectorOrg(request.getOrganizationId(), dirLogin);

        Warehouse w = Warehouse.builder()
                .name(request.getName())
                .address(request.getAddress())
                .organizationId(request.getOrganizationId())
                .build();
        warehouseRepository.save(w);

        if (request.getRacks() != null) {
            for (CreateRackRequest r : request.getRacks()) {
                Rack rack = Rack.builder()
                        .capacity(r.getCapacity())
                        .warehouseId(w.getId())
                        .build();
                rackRepository.save(rack);

                if (r.getCells() != null) {
                    for (CreateCellRequest c : r.getCells()) {
                        Cell cell = Cell.builder()
                                .length(c.getLength())
                                .width(c.getWidth())
                                .height(c.getHeight())
                                .rackId(rack.getId())
                                .build();
                        cellRepository.save(cell);
                    }
                }
            }
        }

        return getById(w.getId(), dirLogin);
    }

    @Override
    @Transactional
    public WarehouseDTO updateWarehouse(Integer id, UpdateWarehouseRequest request, String dirLogin) throws AccessDeniedException {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        checkDirectorOrg(warehouse.getOrganizationId(), dirLogin);

        warehouse.setName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouseRepository.save(warehouse);

        return getById(id, dirLogin);
    }

    @Override
    @Transactional
    public void deleteWarehouse(Integer id, String dirLogin) throws AccessDeniedException {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        checkDirectorOrg(warehouse.getOrganizationId(), dirLogin);

        List<Rack> racks = rackRepository.findByWarehouseId(id);
        for (Rack r : racks) {
            cellRepository.deleteByRackId(r.getId());
        }
        rackRepository.deleteByWarehouseId(id);
        warehouseRepository.deleteById(id);
    }


    @Override
    @Transactional
    public RackDTO createRack(Integer warehouseId, CreateRackRequest request, String dirLogin) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        try {
            checkDirectorOrg(warehouse.getOrganizationId(), dirLogin);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }

        Rack rack = Rack.builder()
                .capacity(request.getCapacity())
                .warehouseId(warehouseId)
                .build();
        rackRepository.save(rack);

        if (request.getCells() != null) {
            for (CreateCellRequest cell : request.getCells()) {
                Cell c = Cell.builder()
                        .length(cell.getLength())
                        .width(cell.getWidth())
                        .height(cell.getHeight())
                        .rackId(rack.getId())
                        .build();
                cellRepository.save(c);
            }
        }

        return getRackDTO(rack);
    }

    @Override
    @Transactional
    public RackDTO updateRack(Integer warehouseId, Integer rackId, UpdateRackRequest request, String dirLogin) {
        Rack rack = rackRepository.findById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("Rack not found"));
        try {
            checkDirectorOrg(warehouseRepository.findById(warehouseId)
                    .orElseThrow().getOrganizationId(), dirLogin);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }

        rack.setCapacity(request.getCapacity());
        rackRepository.save(rack);
        return getRackDTO(rack);
    }

    @Override
    @Transactional
    public CellDTO createCell(Integer warehouseId, Integer rackId, CreateCellRequest request, String dirLogin) {
        try {
            checkDirectorOrg(warehouseRepository.findById(warehouseId)
                    .orElseThrow().getOrganizationId(), dirLogin);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }

        Cell cell = Cell.builder()
                .length(request.getLength())
                .width(request.getWidth())
                .height(request.getHeight())
                .rackId(rackId)
                .build();
        cellRepository.save(cell);
        return getCellDTO(cell);
    }

    @Override
    @Transactional
    public CellDTO updateCell(Integer warehouseId, Integer rackId, Integer cellId, UpdateCellRequest request, String dirLogin) {
        try {
            checkDirectorOrg(warehouseRepository.findById(warehouseId)
                    .orElseThrow().getOrganizationId(), dirLogin);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }

        Cell cell = cellRepository.findById(cellId)
                .orElseThrow(() -> new EntityNotFoundException("Cell not found"));

        cell.setLength(request.getLength());
        cell.setWidth(request.getWidth());
        cell.setHeight(request.getHeight());
        cellRepository.save(cell);
        return getCellDTO(cell);
    }

    @Override
    @Transactional
    public void deleteCell(Integer warehouseId, Integer rackId, Integer cellId, String dirLogin) {
        try {
            checkDirectorOrg(warehouseRepository.findById(warehouseId)
                    .orElseThrow().getOrganizationId(), dirLogin);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        cellRepository.deleteById(cellId);
    }


    @Override
    @Transactional
    public void deleteRack(Integer warehouseId, Integer rackId, String dirLogin) {
        try {
            checkDirectorOrg(warehouseRepository.findById(warehouseId)
                    .orElseThrow().getOrganizationId(), dirLogin);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }

        cellRepository.deleteByRackId(rackId);
        rackRepository.deleteById(rackId);
    }

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
        EmployeeDto emp = employeeClient.getEmployee(login).getData();
        if (!emp.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Only director of this organization");
        }
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

    private RackDTO getRackDTO(Rack rack) {
        List<CellDTO> cells = cellRepository.findAllByRackId(rack.getId()).stream().map(this::getCellDTO).toList();
        return RackDTO.builder()
                .id(rack.getId())
                .capacity(rack.getCapacity())
                .cells(cells)
                .build();
    }

    private CellDTO getCellDTO(Cell cell) {
        return CellDTO.builder()
                .id(cell.getId())
                .length(cell.getLength())
                .width(cell.getWidth())
                .height(cell.getHeight())
                .build();
    }


    @Override
    @Transactional
    public List<WarehouseDTO> findAllByUserLogin(String dirLogin) {

        EmployeeDto emp = employeeClient.getEmployee(dirLogin).getData();
        if (emp.getTitle().equalsIgnoreCase("director")) {
            Integer orgId = emp.getOrganizationId();

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
        List<Warehouse> single = warehouseRepository.findAllById(Collections.singleton(emp.getWarehouseId()));
        List<Rack> racks = rackRepository.findAllByWarehouseId(emp.getWarehouseId());
        List<Cell> cells = cellRepository.findAllByRackId(racks.get(0).getId());
        List<RackDTO> rackDTOs = new ArrayList<>();
        List<CellDTO> cellDTOs = new ArrayList<>();
        for (Cell c : cells) {
            cellDTOs.add(getCellDTO(c));
        }
        for (Rack r : racks) {
            rackDTOs.add(getRackDTO(r));
        }
        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(single.get(0).getId());
        dto.setName(single.get(0).getName());
        dto.setAddress(single.get(0).getAddress());
        dto.setOrganizationId(single.get(0).getOrganizationId());
        dto.setRacks(rackDTOs);
        return List.of(dto);

    }
}
