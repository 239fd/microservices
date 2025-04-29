package by.bsuir.warehouseservice.controller;

import by.bsuir.warehouseservice.DTO.*;
import by.bsuir.warehouseservice.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/by-id")
    public ResponseEntity<Boolean> getById(@RequestParam int id) {
        return ResponseEntity.ok(warehouseService.findById(id));
    }

    @GetMapping("/by-user")
    public ResponseEntity<List<WarehouseDTO>> getByUser(@RequestParam String username) {
        return ResponseEntity.ok(warehouseService.findAllByUserLogin(username));
    }

    @DeleteMapping("/by-organization/{orgId}")
    public ResponseEntity<Void> deleteByOrganization(@PathVariable("orgId") Integer orgId) {
        warehouseService.deleteByOrganization(orgId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<WarehouseDTO> createWarehouse(
            @RequestBody CreateWarehouseRequest request,
            Principal principal) throws AccessDeniedException {
        WarehouseDTO dto = warehouseService.createWarehouse(request, principal.getName());
        return ResponseEntity.ok(dto);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<WarehouseDTO> getById(@PathVariable Integer id, Principal principal) throws AccessDeniedException {
        WarehouseDTO dto = warehouseService.getById(id, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<WarehouseDTO> updateWarehouse(
            @PathVariable Integer id,
            @RequestBody UpdateWarehouseRequest request,
            Principal principal) throws AccessDeniedException {
        WarehouseDTO dto = warehouseService.updateWarehouse(id, request, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<Void> deleteWarehouse(
            @PathVariable Integer id,
            Principal principal) throws AccessDeniedException {
        warehouseService.deleteWarehouse(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<List<WarehouseDTO>> getAllByUser(Principal principal) {
        List<WarehouseDTO> list = warehouseService.findAllByUserLogin(principal.getName());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{warehouseId}/racks")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<RackDTO> createRack(
            @PathVariable Integer warehouseId,
            @RequestBody CreateRackRequest request,
            Principal principal) throws AccessDeniedException {
        RackDTO dto = warehouseService.createRack(warehouseId, request, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{warehouseId}/racks/{rackId}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<RackDTO> updateRack(
            @PathVariable Integer warehouseId,
            @PathVariable Integer rackId,
            @RequestBody UpdateRackRequest request,
            Principal principal) throws AccessDeniedException {
        RackDTO dto = warehouseService.updateRack(warehouseId, rackId, request, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{warehouseId}/racks/{rackId}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<Void> deleteRack(
            @PathVariable Integer warehouseId,
            @PathVariable Integer rackId,
            Principal principal) throws AccessDeniedException {
        warehouseService.deleteRack(warehouseId, rackId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{warehouseId}/racks/{rackId}/cells")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<CellDTO> createCell(
            @PathVariable Integer warehouseId,
            @PathVariable Integer rackId,
            @RequestBody CreateCellRequest request,
            Principal principal) throws AccessDeniedException {
        CellDTO dto = warehouseService.createCell(warehouseId, rackId, request, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{warehouseId}/racks/{rackId}/cells/{cellId}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<CellDTO> updateCell(
            @PathVariable Integer warehouseId,
            @PathVariable Integer rackId,
            @PathVariable Integer cellId,
            @RequestBody UpdateCellRequest request,
            Principal principal) throws AccessDeniedException {
        CellDTO dto = warehouseService.updateCell(warehouseId, rackId, cellId, request, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{warehouseId}/racks/{rackId}/cells/{cellId}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<Void> deleteCell(
            @PathVariable Integer warehouseId,
            @PathVariable Integer rackId,
            @PathVariable Integer cellId,
            Principal principal) throws AccessDeniedException {
        warehouseService.deleteCell(warehouseId, rackId, cellId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
