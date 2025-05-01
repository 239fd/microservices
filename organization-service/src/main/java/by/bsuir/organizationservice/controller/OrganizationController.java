package by.bsuir.organizationservice.controller;

import by.bsuir.organizationservice.DTO.*;
import by.bsuir.organizationservice.entity.Organization;
import by.bsuir.organizationservice.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
@CrossOrigin(maxAge = 3600L)
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<OrganizationDTO> getOrganization(Principal principal) {
        String username = principal.getName();
        OrganizationDTO organizationDTO = organizationService.findInfoByUserLogin(username);
        return ResponseEntity.ok(organizationDTO);
    }

    @PostMapping("/by-inn")
    public ResponseEntity<Boolean> getOrganizationByINN(@RequestBody OrganizationSearchDTO organizationSearchDTO) {
        return ResponseEntity.ok(organizationService.findByINN(organizationSearchDTO.getInn()));
    }

    @GetMapping("/inn")
    public ResponseEntity<Integer> getOrganizationIdByINN(@RequestParam String inn) {
        return ResponseEntity.ok(organizationService.findOrganizationIdByINN(inn));
    }

    @GetMapping("/id")
    public ResponseEntity<OrganizationDTO> getOrganizationById(@RequestParam int id) {
        OrganizationDTO organizationDTO = organizationService.findById(id);
        return ResponseEntity.ok(organizationDTO);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('director')")
    public ResponseEntity<OrganizationResponse> create(@RequestBody CreateOrganizationRequest request,
                                                       Principal principal) {
        String dirLogin = principal.getName();
        OrganizationResponse response = organizationService.create(request, dirLogin);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<OrganizationResponse> update(@PathVariable Integer id,
                                                       @RequestBody UpdateOrganizationRequest request,
                                                       Principal principal) {
        String dirLogin = principal.getName();
        OrganizationResponse response = organizationService.update(id, request, dirLogin);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DIRECTOR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id,
                                       Principal principal) {
        String dirLogin = principal.getName();
        organizationService.delete(id, dirLogin);
        return ResponseEntity.noContent().build();
    }

}
