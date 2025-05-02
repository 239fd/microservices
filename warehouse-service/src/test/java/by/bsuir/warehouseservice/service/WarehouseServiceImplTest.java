package by.bsuir.warehouseservice.service;

import by.bsuir.warehouseservice.DTO.*;
import by.bsuir.warehouseservice.entity.Cell;
import by.bsuir.warehouseservice.entity.Rack;
import by.bsuir.warehouseservice.entity.Warehouse;
import by.bsuir.warehouseservice.feign.EmployeeClient;
import by.bsuir.warehouseservice.repository.CellRepository;
import by.bsuir.warehouseservice.repository.RackRepository;
import by.bsuir.warehouseservice.repository.WarehouseRepository;
import by.bsuir.warehouseservice.service.impl.WarehouseServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.AccessDeniedException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock WarehouseRepository warehouseRepository;
    @Mock RackRepository rackRepository;
    @Mock CellRepository cellRepository;
    @Mock EmployeeClient employeeClient;

    @Spy @InjectMocks
    WarehouseServiceImpl service;

    EmployeeDto director;
    EmployeeDto other;

    @BeforeEach
    void init() {
        director = new EmployeeDto();
        director.setLogin("dir");
        director.setOrganizationId(100);
        director.setTitle("director");

        other = new EmployeeDto();
        other.setLogin("usr");
        other.setOrganizationId(200);
        other.setTitle("worker");
        other.setWarehouseId(42);
    }

    @Test
    void createWarehouse_success_noRacks() throws Exception {
        var req = new CreateWarehouseRequest();
        req.setName("W1");
        req.setAddress("A1");
        req.setOrganizationId(100);

        when(employeeClient.getEmployee("dir")).thenReturn(new ApiResponse<>(director,true,""));
        doAnswer(inv -> {
            Warehouse w = inv.getArgument(0);
            ReflectionTestUtils.setField(w, "id", 1);
            return w;
        }).when(warehouseRepository).save(any());

        Warehouse w = Warehouse.builder()
                .id(1).name("W1").address("A1").organizationId(100).build();
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(w));
        when(rackRepository.findAllByWarehouseId(1)).thenReturn(Collections.emptyList());

        var dto = service.createWarehouse(req, "dir");

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getRacks()).isEmpty();
        verify(warehouseRepository).save(any());
    }

    @Test
    void createWarehouse_accessDenied() {
        var req = new CreateWarehouseRequest();
        req.setOrganizationId(999);
        when(employeeClient.getEmployee("dir")).thenReturn(new ApiResponse<>(director,true,""));

        assertThatThrownBy(() -> service.createWarehouse(req, "dir"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateWarehouse_success() throws Exception {
        var w = Warehouse.builder().id(2).name("W").address("A").organizationId(100).build();
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(w));
        when(employeeClient.getEmployee("dir")).thenReturn(new ApiResponse<>(director,true,""));
        when(warehouseRepository.save(any())).thenReturn(w);
        when(rackRepository.findAllByWarehouseId(2)).thenReturn(Collections.emptyList());

        var req = new UpdateWarehouseRequest();
        req.setName("W2");
        req.setAddress("A2");

        var dto = service.updateWarehouse(2, req, "dir");
        assertThat(dto.getName()).isEqualTo("W2");
        assertThat(dto.getAddress()).isEqualTo("A2");
    }

    @Test
    void updateWarehouse_notFound() {
        when(warehouseRepository.findById(3)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateWarehouse(3, new UpdateWarehouseRequest(), "dir"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateWarehouse_accessDenied() {
        var w = Warehouse.builder().id(4).organizationId(100).build();
        when(warehouseRepository.findById(4)).thenReturn(Optional.of(w));
        when(employeeClient.getEmployee("usr")).thenReturn(new ApiResponse<>(other,true,""));

        assertThatThrownBy(() -> service.updateWarehouse(4, new UpdateWarehouseRequest(), "usr"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteByOrganization_delegates() {
        service.deleteByOrganization(77);
        verify(warehouseRepository).findByOrganizationId(77);
        verify(warehouseRepository).deleteByOrganizationId(77);
    }

    @Test
    void findById_true() {
        when(warehouseRepository.findById(5)).thenReturn(Optional.of(new Warehouse()));
        assertThat(service.findById(5)).isTrue();
    }

    @Test
    void findById_false() {
        when(warehouseRepository.findById(6)).thenReturn(Optional.empty());
        assertThat(service.findById(6)).isFalse();
    }

    @Test
    void getById_success() throws Exception {
        var w = Warehouse.builder().id(10).name("W").address("A").organizationId(100).build();
        when(warehouseRepository.findById(10)).thenReturn(Optional.of(w));
        when(employeeClient.getEmployee("dir")).thenReturn(new ApiResponse<>(director,true,""));

        var r = new Rack(); ReflectionTestUtils.setField(r,"id",11); r.setCapacity(5); r.setWarehouseId(10);
        when(rackRepository.findAllByWarehouseId(10)).thenReturn(List.of(r));
        var c = new Cell(); ReflectionTestUtils.setField(c,"id",101); c.setLength(1.0); c.setWidth(2.0); c.setHeight(3.0); c.setRackId(11);
        when(cellRepository.findAllByRackId(11)).thenReturn(List.of(c));

        WarehouseDTO dto = service.getById(10, "dir");
        assertThat(dto.getRacks()).hasSize(1)
                .first().extracting(RackDTO::getCells)
                .asList().hasSize(1);
    }

    @Test
    void getById_notFound() {
        when(warehouseRepository.findById(20)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(20, "dir"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getById_accessDenied() {
        var w = Warehouse.builder().id(30).organizationId(100).build();
        when(warehouseRepository.findById(30)).thenReturn(Optional.of(w));
        when(employeeClient.getEmployee("usr")).thenReturn(new ApiResponse<>(other,true,""));
        assertThatThrownBy(() -> service.getById(30, "usr"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findAllByUserLogin_director() throws Exception {
        when(employeeClient.getEmployee("dir")).thenReturn(new ApiResponse<>(director,true,""));
        var w = Warehouse.builder().id(7).organizationId(100).build();
        when(warehouseRepository.findAllByOrganizationId(100)).thenReturn(List.of(w));
        doReturn(new WarehouseDTO()).when(service).getById(7, "dir");

        var list = service.findAllByUserLogin("dir");
        assertThat(list).hasSize(1);
    }
}
