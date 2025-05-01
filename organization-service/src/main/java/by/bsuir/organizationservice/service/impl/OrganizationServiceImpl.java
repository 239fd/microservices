package by.bsuir.organizationservice.service.impl;

import by.bsuir.organizationservice.DTO.*;
import by.bsuir.organizationservice.entity.Organization;
import by.bsuir.organizationservice.exeption.AppException;
import by.bsuir.organizationservice.feign.EmployeeClient;
import by.bsuir.organizationservice.feign.WarehouseClient;
import by.bsuir.organizationservice.repository.OrganizationRepository;
import by.bsuir.organizationservice.service.OrganizationService;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final WarehouseClient warehouseClient;
    private final EmployeeClient employeeClient;


    @Override
    public boolean findByINN(String inn) {
        Organization organization = organizationRepository.findByInn(inn);
        return organization != null;
    }

    @Override
    public OrganizationResponse create(CreateOrganizationRequest request, String dirLogin) {

        EmployeeDto director = employeeClient.getByLogin(dirLogin).getData();

        if (!"director".equalsIgnoreCase(director.getTitle())) {
            throw new SecurityException("Only a director can create an organization");
        }
        if (director.getOrganizationId() != null) {
            throw new IllegalStateException("Director already has an organization");
        }
        Organization org = new Organization();
        org.setInn(request.getInn());
        org.setName(request.getName());
        org.setAddress(request.getAddress());
        org = organizationRepository.save(org);
        employeeClient.assignOrganization(dirLogin, org.getId());
        return new OrganizationResponse(org.getId(), org.getInn(), org.getName(), org.getAddress());
    }

    @Override
    public OrganizationResponse update(Integer id, UpdateOrganizationRequest request, String dirLogin) {

        Organization org = organizationRepository.findByInn(id.toString());
        if (org == null) {
             throw new  IllegalArgumentException("Organization not found");
        }

        EmployeeDto director = employeeClient.getByLogin(dirLogin).getData();

        int orgIdOfDirector = director.getOrganizationId();
        if (!(orgIdOfDirector == org.getId()))  {
            throw new IllegalStateException("Only the director of this organization can update it");
        }

        org.setName(request.getName());
        org.setAddress(request.getAddress());
        org = organizationRepository.save(org);
        return new OrganizationResponse(org.getId(), org.getInn(), org.getName(), org.getAddress());
    }

    @Override
    public void delete(Integer id, String dirLogin) {
        Organization org = organizationRepository.findByInn(id.toString());
        if (org == null) {
            throw new  IllegalArgumentException("Organization not found");
        }

        EmployeeDto director = employeeClient.getByLogin(dirLogin).getData();

        int orgIdOfDirector = director.getOrganizationId();
        if (!(orgIdOfDirector == org.getId()))  {
            throw new IllegalStateException("Only the director of this organization can update it");
        }

        warehouseClient.deleteByOrganization(org.getId());
        employeeClient.deleteByOrganization(org.getId());
        organizationRepository.delete(org);
    }

    @Override
    public OrganizationDTO findInfoByUserLogin(String username) {
        EmployeeDto employee;
        try {
            employee = employeeClient.getByLogin(username).getData();
        } catch (FeignException e) {
            throw new EntityNotFoundException("Employee not found by login: " + username);
        }

        Integer directorId = employee.getOrganizationId();

        Organization org = organizationRepository
                .findById(directorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization not found for director id: " + directorId
                ));

        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setInn(org.getInn());
        dto.setAddress(org.getAddress());

        return dto;
    }

    @Override
    public Integer findOrganizationIdByINN(String inn) {
        return organizationRepository.findByInn(inn).getId();
    }

    @Override
    public OrganizationDTO findById(int id) {
        Organization organizationDTO = organizationRepository.findById(id)
                .orElseThrow();
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(organizationDTO.getId());
        dto.setInn(organizationDTO.getInn());
        dto.setName(organizationDTO.getName());
        dto.setAddress(organizationDTO.getAddress());
        return dto;
    }
}
