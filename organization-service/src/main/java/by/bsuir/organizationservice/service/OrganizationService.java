package by.bsuir.organizationservice.service;

import by.bsuir.organizationservice.DTO.CreateOrganizationRequest;
import by.bsuir.organizationservice.DTO.OrganizationDTO;
import by.bsuir.organizationservice.DTO.OrganizationResponse;
import by.bsuir.organizationservice.DTO.UpdateOrganizationRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface OrganizationService {
    boolean findByINN(String inn);
    OrganizationResponse create(CreateOrganizationRequest request, String directorId);
    OrganizationResponse update(Integer id, UpdateOrganizationRequest request, String directorId);
    void delete(Integer id, String directorId);
    OrganizationDTO findInfoByUserLogin(String username);
    Integer findOrganizationIdByINN(String inn);
    OrganizationDTO findById(int id);
}
