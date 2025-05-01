package by.bsuir.employeeservice.service;

import by.bsuir.employeeservice.DTO.EmployeeDto;
import by.bsuir.employeeservice.DTO.OrganizationSearchDTO;
import by.bsuir.employeeservice.DTO.RegisterRequest;
import by.bsuir.employeeservice.entity.Employee;
import by.bsuir.employeeservice.exception.AppException;
import by.bsuir.employeeservice.feign.OrganizationClient;
import by.bsuir.employeeservice.feign.WarehouseClient;
import by.bsuir.employeeservice.repository.EmployeeRepository;
import by.bsuir.employeeservice.service.Impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private OrganizationClient organizationClient;
    @Mock
    private WarehouseClient warehouseClient;

    @InjectMocks
    private EmployeeServiceImpl service;

    private Employee existing;

    @BeforeEach
    void init() {
        existing = new Employee();
        existing.setLogin("john");
        existing.setFirstName("John");
        existing.setSecondName("Q");
        existing.setSurname("Public");
        existing.setPhone("123");
        existing.setTitle("ROLE_WORKER");
        existing.setOrganizationId(10);
        existing.setWarehouseId(20);
    }

    @Test
    void findByLogin_success() {
        when(employeeRepository.findByLogin("john")).thenReturn(Optional.of(existing));

        Employee result = service.findByLogin("john");

        assertThat(result).isSameAs(existing);
    }

    @Test
    void existsByLogin_true() {
        when(employeeRepository.existsByLogin("john")).thenReturn(true);
        assertThat(service.existsByLogin("john")).isTrue();
    }

    @Test
    void existsByLogin_false() {
        when(employeeRepository.existsByLogin("john")).thenReturn(false);
        assertThat(service.existsByLogin("john")).isFalse();
    }

    private RegisterRequest buildRequest(String title) {
        RegisterRequest r = new RegisterRequest();
        r.setLogin("new");
        r.setFirstName("A");
        r.setSecondName("B");
        r.setSurname("C");
        r.setPhone("999");
        r.setPassword("pass");
        r.setTitle(title);
        r.setOrganizationId(10);
        r.setWarehouseId(20);
        return r;
    }

    @Test
    void create_asDirector_skipsExternalChecks_andSaves() {
        var req = buildRequest("director");

        service.create(req);

        verifyNoInteractions(organizationClient, warehouseClient);
        verify(employeeRepository).save(any(Employee.class));
    }


    @Test
    void getEmployee_notFound_throws() {
        when(employeeRepository.findByLogin("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEmployee("x"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Employee not found");
    }

    @Test
    void assignOrganization_success() {
        when(employeeRepository.findByLogin("john")).thenReturn(Optional.of(existing));

        service.assignOrganization("john", 55);

        assertThat(existing.getOrganizationId()).isEqualTo(55);
        verify(employeeRepository).save(existing);
    }

    @Test
    void assignOrganization_notFound_throws() {
        when(employeeRepository.findByLogin("jane")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOrganization("jane", 5))
            .isInstanceOf(IllegalArgumentException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void deleteByOrganization_delegates() {
        service.deleteByOrganization(42);
        verify(employeeRepository).deleteByOrganizationId(42);
    }

    @Test
    void assignWarehouse_success() {
        when(employeeRepository.findByLogin("john")).thenReturn(Optional.of(existing));

        service.assignWarehouse("john", 77);

        assertThat(existing.getWarehouseId()).isEqualTo(77);
        verify(employeeRepository).save(existing);
    }

    @Test
    void unassignWarehouseByWarehouseId_resetsAll() {
        Employee e1 = new Employee(); e1.setLogin("a"); e1.setWarehouseId(5);
        Employee e2 = new Employee(); e2.setLogin("b"); e2.setWarehouseId(5);
        when(employeeRepository.findAllByWarehouseId(5)).thenReturn(List.of(e1,e2));

        service.unassignWarehouseByWarehouseId(5);

        assertThat(e1.getWarehouseId()).isNull();
        assertThat(e2.getWarehouseId()).isNull();
        verify(employeeRepository, times(2)).save(any(Employee.class));
    }

    @Test
    void deleteByWarehouse_delegates() {
        service.deleteByWarehouse(9);
        verify(employeeRepository).deleteByWarehouseId(9);
    }
}
