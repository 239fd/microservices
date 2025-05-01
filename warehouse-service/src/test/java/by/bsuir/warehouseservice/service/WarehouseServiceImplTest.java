package by.bsuir.warehouseservice.service.impl;

import by.bsuir.warehouseservice.DTO.*;
import by.bsuir.warehouseservice.entity.Cell;
import by.bsuir.warehouseservice.entity.Rack;
import by.bsuir.warehouseservice.entity.Warehouse;
import by.bsuir.warehouseservice.feign.EmployeeClient;
import by.bsuir.warehouseservice.repository.CellRepository;
import by.bsuir.warehouseservice.repository.RackRepository;
import by.bsuir.warehouseservice.repository.WarehouseRepository;
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

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private RackRepository rackRepository;
    @Mock
    private CellRepository cellRepository;
    @Mock
    private EmployeeClient employeeClient;

    @InjectMocks
    private WarehouseServiceImpl service;

    private EmployeeDto director;
    private EmployeeDto other;

    @BeforeEach
    void setUp() {
        // директор из orgId = 100
        director = new EmployeeDto();
        director.setLogin("dir");
        director.setOrganizationId(100);
        director.setTitle("director");
        // обычный сотрудник
        other = new EmployeeDto();
        other.setLogin("user");
        other.setOrganizationId(100);
        other.setTitle("worker");
        other.setWarehouseId(42);
    }

    // ---------------- createWarehouse ----------------

    @Test
    void createWarehouse_success_noRacks() throws Exception {
        // prepare request
        CreateWarehouseRequest req = new CreateWarehouseRequest();
        req.setName("W1");
        req.setAddress("Addr");
        req.setOrganizationId(100);
        // mock employee
        when(employeeClient.getEmployee("dir"))
                .thenReturn(new ResponseDTO<>(director));

        doAnswer(invocation -> {
            Warehouse w = invocation.getArgument(0);
            ReflectionTestUtils.setField(w, "id", 1);
            return w;
        }).when(warehouseRepository).save(any(Warehouse.class));
        // mock последующий findById
        Warehouse saved = Warehouse.builder()
                .id(1).name("W1").address("Addr").organizationId(100).build();
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(saved));
        when(rackRepository.findAllByWarehouseId(1)).thenReturn(Collections.emptyList());

        // call
        WarehouseDTO dto = service.createWarehouse(req, "dir");

        // verify
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getName()).isEqualTo("W1");
        assertThat(dto.getRacks()).isEmpty();
        verify(warehouseRepository).save(any());
        verify(rackRepository).findAllByWarehouseId(1);
    }

    @Test
    void createWarehouse_accessDenied() {
        CreateWarehouseRequest req = new CreateWarehouseRequest();
        req.setOrganizationId(200);
        when(employeeClient.getEmployee("dir"))
                .thenReturn(new ResponseDTO<>(director));

        assertThatThrownBy(() -> service.createWarehouse(req, "dir"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only director");
    }

    // ---------------- getById ----------------

    @Test
    void getById_success_withRacksAndCells() throws Exception {
        Warehouse w = Warehouse.builder()
                .id(1).name("W1").address("A").organizationId(100).build();
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(w));
        when(employeeClient.getEmployee("dir")).thenReturn(new ResponseDTO<>(director));

        // один стеллаж
        Rack r = Rack.builder().id(11).capacity(50).warehouseId(1).build();
        when(rackRepository.findAllByWarehouseId(1)).thenReturn(List.of(r));
        // две ячейки для стеллажа
        Cell c1 = Cell.builder().id(101).length(1).width(2).height(3).rackId(11).build();
        Cell c2 = Cell.builder().id(102).length(4).width(5).height(6).rackId(11).build();
        when(cellRepository.findAllByRackId(11)).thenReturn(List.of(c1, c2));

        WarehouseDTO dto = service.getById(1, "dir");

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getRacks()).hasSize(1);
        RackDTO rd = dto.getRacks().get(0);
        assertThat(rd.getCells()).extracting(CellDTO::getId).containsExactlyInAnyOrder(101, 102);
    }

    @Test
    void getById_notFound() {
        when(warehouseRepository.findById(2)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(2, "dir"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Warehouse not found");
    }

    // ---------------- createRack ----------------

    @Test
    void createRack_success_withCells() {
        // подготовка склада
        Warehouse w = Warehouse.builder().id(1).organizationId(100).build();
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(w));
        when(employeeClient.getEmployee("dir")).thenReturn(new ResponseDTO<>(director));

        CreateRackRequest rr = new CreateRackRequest();
        rr.setCapacity(20);
        CreateCellRequest cr = new CreateCellRequest();
        cr.setLength(1); cr.setWidth(2); cr.setHeight(3);
        rr.setCells(List.of(cr));

        // симулируем присвоение id стеллажу
        doAnswer(inv -> {
            Rack r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 11);
            return r;
        }).when(rackRepository).save(any(Rack.class));

        doAnswer(inv -> {
            Cell c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 101);
            return c;
        }).when(cellRepository).save(any(Cell.class));

        RackDTO rd = service.createRack(1, rr, "dir");

        assertThat(rd.getId()).isEqualTo(11);
        assertThat(rd.getCapacity()).isEqualTo(20);
        assertThat(rd.getCells()).hasSize(1)
                .first().extracting(CellDTO::getLength).isEqualTo(1);
    }

    @Test
    void createRack_warehouseNotFound() {
        when(warehouseRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createRack(99, new CreateRackRequest(), "dir"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Warehouse not found");
    }

    // ---------------- findAllByUserLogin ----------------

    @Test
    void findAllByUserLogin_director() throws Exception {
        when(employeeClient.getEmployee("dir")).thenReturn(new ResponseDTO<>(director));
        Warehouse w1 = Warehouse.builder().id(1).organizationId(100).build();
        when(warehouseRepository.findAllByOrganizationId(100)).thenReturn(List.of(w1));

        // мокнут getById при вызове через stream
        doReturn(new WarehouseDTO()).when(service).getById(1, "dir");

        List<WarehouseDTO> list = service.findAllByUserLogin("dir");
        assertThat(list).hasSize(1);
        verify(service).getById(1, "dir");
    }

    @Test
    void findAllByUserLogin_nonDirector() {
        when(employeeClient.getEmployee("user")).thenReturn(new ResponseDTO<>(other));
        // сотрудник из склада id=42
        Warehouse w = Warehouse.builder()
                .id(42).name("Wn").address("A").organizationId(100).build();
        when(warehouseRepository.findAllById(Set.of(42))).thenReturn(List.of(w));
        when(rackRepository.findByWarehouseId(42)).thenReturn(List.of());
        // для непустого списка cellRepository не используется

        List<WarehouseDTO> list = service.findAllByUserLogin("user");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(42);
    }
}
