package by.bsuir.organizationservice.service;

import by.bsuir.organizationservice.DTO.*;
import by.bsuir.organizationservice.entity.Organization;
import by.bsuir.organizationservice.feign.EmployeeClient;
import by.bsuir.organizationservice.feign.WarehouseClient;
import by.bsuir.organizationservice.repository.OrganizationRepository;
import by.bsuir.organizationservice.service.impl.OrganizationServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private OrganizationRepository orgRepo;
    @Mock
    private WarehouseClient warehouseClient;
    @Mock
    private EmployeeClient employeeClient;

    @InjectMocks
    private OrganizationServiceImpl service;

    private EmployeeDto director;
    private EmployeeDto other;
    private CreateOrganizationRequest createReq;
    private UpdateOrganizationRequest updateReq;

    @BeforeEach
    void setUp() {

        director = new EmployeeDto();
        director.setLogin("dir");
        director.setTitle("director");
        director.setOrganizationId(null);

        other = new EmployeeDto();
        other.setLogin("dir2");
        other.setTitle("director");
        other.setOrganizationId(99);

        createReq = new CreateOrganizationRequest();
        createReq.setInn("123");
        createReq.setName("OrgName");
        createReq.setAddress("Addr");

        updateReq = new UpdateOrganizationRequest();
        updateReq.setName("NewName");
        updateReq.setAddress("NewAddr");
    }

    @Test
    void findByINN_trueWhenExists() {
        when(orgRepo.findByInn("123")).thenReturn(new Organization());
        assertThat(service.findByINN("123")).isTrue();
    }

    @Test
    void findByINN_falseWhenNotExists() {
        when(orgRepo.findByInn("123")).thenReturn(null);
        assertThat(service.findByINN("123")).isFalse();
    }

    @Test
    void create_success() {
        when(employeeClient.getByLogin("dir")).thenReturn(new ApiResponse<>(director, true, ""));
        Organization saved = new Organization();
        saved.setId(42);
        saved.setInn("123");
        saved.setName("OrgName");
        saved.setAddress("Addr");
        when(orgRepo.save(any())).thenReturn(saved);

        OrganizationResponse resp = service.create(createReq, "dir");

        assertThat(resp.getId()).isEqualTo(42);
        verify(orgRepo).save(any(Organization.class));
        verify(employeeClient).assignOrganization("dir", 42);
    }

    @Test
    void create_nonDirector_throws() {
        EmployeeDto notDir = new EmployeeDto();
        notDir.setLogin("u");
        notDir.setTitle("worker");
        when(employeeClient.getByLogin("u")).thenReturn(new ApiResponse<>(notDir, true, ""));

        assertThatThrownBy(() -> service.create(createReq, "u"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Only a director");
        verifyNoInteractions(orgRepo, warehouseClient);
    }

    @Test
    void create_directorHasOrg_throws() {
        when(employeeClient.getByLogin("dir2")).thenReturn(new ApiResponse<>(other, true, ""));

        assertThatThrownBy(() -> service.create(createReq, "dir2"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already has an organization");
        verifyNoInteractions(orgRepo, warehouseClient);
    }

    @Test
    void update_success() {
        Organization existing = new Organization();
        existing.setId(5);
        existing.setInn("5");
        existing.setName("Old");
        existing.setAddress("OldA");
        when(orgRepo.findByInn("5")).thenReturn(existing);
        director.setOrganizationId(5);
        when(employeeClient.getByLogin("dir")).thenReturn(new ApiResponse<>(director, true, ""));
        when(orgRepo.save(existing)).thenReturn(existing);

        OrganizationResponse resp = service.update(5, updateReq, "dir");

        assertThat(resp.getName()).isEqualTo("NewName");
        assertThat(resp.getAddress()).isEqualTo("NewAddr");
    }

    @Test
    void update_notFound_throws() {
        when(orgRepo.findByInn("5")).thenReturn(null);
        assertThatThrownBy(() -> service.update(5, updateReq, "dir"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Organization not found");
    }

    @Test
    void update_wrongDirector_throws() {
        Organization existing = new Organization();
        existing.setId(6);
        existing.setInn("6");
        when(orgRepo.findByInn("6")).thenReturn(existing);
        director.setOrganizationId(7);
        when(employeeClient.getByLogin("dir")).thenReturn(new ApiResponse<>(director, true, ""));

        assertThatThrownBy(() -> service.update(6, updateReq, "dir"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only the director");
    }

    @Test
    void delete_success() {
        Organization existing = new Organization();
        existing.setId(8);
        existing.setInn("8");
        when(orgRepo.findByInn("8")).thenReturn(existing);
        director.setOrganizationId(8);
        when(employeeClient.getByLogin("dir")).thenReturn(new ApiResponse<>(director, true, ""));

        service.delete(8, "dir");

        verify(warehouseClient).deleteByOrganization(8);
        verify(employeeClient).deleteByOrganization(8);
        verify(orgRepo).delete(existing);
    }

    @Test
    void delete_notFound_throws() {
        when(orgRepo.findByInn("8")).thenReturn(null);
        assertThatThrownBy(() -> service.delete(8, "dir"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Organization not found");
    }

    @Test
    void delete_wrongDirector_throws() {
        Organization existing = new Organization();
        existing.setId(9);
        existing.setInn("9");
        when(orgRepo.findByInn("9")).thenReturn(existing);
        director.setOrganizationId(10);
        when(employeeClient.getByLogin("dir")).thenReturn(new ApiResponse<>(director, true, ""));

        assertThatThrownBy(() -> service.delete(9, "dir"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only the director");
    }

    @Test
    void findInfoByUserLogin_success() {
        EmployeeDto emp = new EmployeeDto();
        emp.setLogin("u");
        emp.setOrganizationId(11);
        when(employeeClient.getByLogin("u")).thenReturn(new ApiResponse<>(emp, true, ""));
        Organization org = new Organization();
        org.setId(11);
        org.setInn("inn");
        org.setName("N");
        org.setAddress("A");
        when(orgRepo.findById(11)).thenReturn(Optional.of(org));

        OrganizationDTO dto = service.findInfoByUserLogin("u");

        assertThat(dto.getId()).isEqualTo(11);
        assertThat(dto.getName()).isEqualTo("N");
    }

    @Test
    void findInfoByUserLogin_employeeNotFound_throws() {
        when(employeeClient.getByLogin("u")).thenThrow(mock(FeignException.class));
        assertThatThrownBy(() -> service.findInfoByUserLogin("u"))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Employee not found");
    }

    @Test
    void findInfoByUserLogin_orgNotFound_throws() {
        EmployeeDto emp = new EmployeeDto();
        emp.setLogin("u");
        emp.setOrganizationId(12);
        when(employeeClient.getByLogin("u")).thenReturn(new ApiResponse<>(emp, true, ""));
        when(orgRepo.findById(12)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findInfoByUserLogin("u"))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Organization not found");
    }

    @Test
    void findOrganizationIdByINN_returnsId() {
        Organization org = new Organization();
        org.setId(13);
        when(orgRepo.findByInn("inn")).thenReturn(org);
        assertThat(service.findOrganizationIdByINN("inn")).isEqualTo(13);
    }

    @Test
    void findById_success() {
        Organization org = new Organization();
        org.setId(14);
        org.setInn("i");
        org.setName("n");
        org.setAddress("a");
        when(orgRepo.findById(14)).thenReturn(Optional.of(org));

        OrganizationDTO dto = service.findById(14);
        assertThat(dto.getInn()).isEqualTo("i");
    }

    @Test
    void findById_notFound_throws() {
        when(orgRepo.findById(15)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(15))
            .isInstanceOf(Exception.class);
    }
}
