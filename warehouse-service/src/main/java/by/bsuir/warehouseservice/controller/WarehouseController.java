package by.bsuir.warehouseservice.controller;

import by.bsuir.warehouseservice.DTO.CreateWarehouseRequest;
import by.bsuir.warehouseservice.DTO.UpdateWarehouseRequest;
import by.bsuir.warehouseservice.DTO.WarehouseDTO;
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
    public ResponseEntity<WarehouseDTO> create(@RequestBody CreateWarehouseRequest request, Principal principal) throws AccessDeniedException {
        WarehouseDTO dto = warehouseService.create(request, principal.getName());
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
    public ResponseEntity<WarehouseDTO> update(@PathVariable Integer id,
                                               @RequestBody UpdateWarehouseRequest request,
                                               Principal principal) throws AccessDeniedException {
        WarehouseDTO dto = warehouseService.update(id, request, principal.getName());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Principal principal) throws AccessDeniedException {
        warehouseService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<List<WarehouseDTO>> getAllByUser(Principal principal) {
        List<WarehouseDTO> list = warehouseService.findAllByUserLogin(principal.getName());
        return ResponseEntity.ok(list);
    }
}
