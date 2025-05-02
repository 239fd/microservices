package by.bsuir.warehouseservice.service;

import by.bsuir.warehouseservice.DTO.*;
import by.bsuir.warehouseservice.entity.*;
import by.bsuir.warehouseservice.feign.EmployeeClient;
import by.bsuir.warehouseservice.feign.ProductClient;
import by.bsuir.warehouseservice.repository.*;
import by.bsuir.warehouseservice.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    WarehouseRepository warehouseRepository;
    @Mock
    RackRepository rackRepository;
    @Mock
    CellRepository cellRepository;
    @Mock
    EmployeeClient employeeClient;
    @Mock
    ProductClient productClient;

    @InjectMocks
    StatisticsServiceImpl service;

    EmployeeDto director;

    @BeforeEach
    void init() {
        director = new EmployeeDto();
        director.setLogin("dir");
        director.setOrganizationId(1);
        director.setTitle("director");
    }

    @Test
    void getWarehouseAnalytics_happyPath() {
        when(employeeClient.getEmployee("dir"))
                .thenReturn(new ApiResponse<>(director, true, ""));

        Warehouse wh = new Warehouse();
        wh.setId(1);
        wh.setName("Warehouse A");
        wh.setOrganizationId(1);
        when(warehouseRepository.findAllByOrganizationId(1))
                .thenReturn(List.of(wh));

        Rack r1 = new Rack();
        ReflectionTestUtils.setField(r1, "id", 11);
        r1.setCapacity(100);
        r1.setWarehouseId(1);
        Rack r2 = new Rack();
        ReflectionTestUtils.setField(r2, "id", 12);
        r2.setCapacity(200);
        r2.setWarehouseId(1);
        when(rackRepository.findByWarehouseId(1))
                .thenReturn(List.of(r1, r2));

        Cell c1 = new Cell();
        ReflectionTestUtils.setField(c1, "id", 101);
        c1.setRackId(11);
        Cell c2 = new Cell();
        ReflectionTestUtils.setField(c2, "id", 102);
        c2.setRackId(11);
        Cell c3 = new Cell();
        ReflectionTestUtils.setField(c3, "id", 103);
        c3.setRackId(12);
        when(cellRepository.findAllByRackId(11)).thenReturn(List.of(c1, c2));
        when(cellRepository.findAllByRackId(12)).thenReturn(List.of(c3));

        ProductDTO p1 = new ProductDTO();
        p1.setId(1001);
        p1.setPrice(10.0);
        p1.setAmount(2);
        ProductDTO p2 = new ProductDTO();
        p2.setId(1002);
        p2.setPrice(5.0);
        p2.setAmount(1);

        when(productClient.getProductsByCellIds(List.of(101, 102, 103)))
                .thenReturn(List.of(p1, p2));

        List<WarehouseAnalyticsDTO> analytics = service.getWarehouseAnalytics("dir");

        assertThat(analytics).hasSize(1);
        WarehouseAnalyticsDTO dto = analytics.get(0);
        assertThat(dto.getWarehouseId()).isEqualTo(1);
        assertThat(dto.getWarehouseName()).isEqualTo("Warehouse A");
        assertThat(dto.getRackCount()).isEqualTo(2);
        assertThat(dto.getCellCount()).isEqualTo(3);
        assertThat(dto.getFilledCellCount()).isEqualTo(2);

        assertThat(dto.getAverageRackCapacity()).isEqualTo(150.0);
        assertThat(dto.getProductCount()).isEqualTo(2);

        assertThat(dto.getTotalProductValue()).isEqualTo(25.0);
    }

    @Test
    void getExpiringProducts_filtersByThreshold() {
        when(employeeClient.getEmployee("dir"))
                .thenReturn(new ApiResponse<>(director, true, ""));

        Warehouse w = new Warehouse();
        w.setId(2);
        w.setOrganizationId(1);
        when(warehouseRepository.findAllByOrganizationId(1)).thenReturn(List.of(w));
        Rack r = new Rack();
        ReflectionTestUtils.setField(r, "id", 21);
        r.setWarehouseId(2);
        when(rackRepository.findByWarehouseId(2)).thenReturn(List.of(r));
        Cell cell = new Cell();
        ReflectionTestUtils.setField(cell, "id", 201);
        cell.setRackId(21);
        when(cellRepository.findAllByRackId(21)).thenReturn(List.of(cell));

        LocalDate now = LocalDate.now();
        ProductDTO good = new ProductDTO();
        good.setId(1);
        good.setName("Good");
        good.setBestBeforeDate(now.plusDays(3));
        good.setWarehouseId(2);
        good.setWarehouseName("W2");
        ProductDTO expired = new ProductDTO();
        expired.setId(2);
        expired.setName("Expired");
        expired.setBestBeforeDate(now.minusDays(1));
        ProductDTO far = new ProductDTO();
        far.setId(3);
        far.setName("Far");
        expired.setBestBeforeDate(now.plusDays(10));
        when(productClient.getProductsByCellIds(List.of(201)))
                .thenReturn(List.of(good, expired, far));

        List<ProductShortExpiryDTO> list = service.getExpiringProducts("dir", 5);

        assertThat(list).hasSize(1);
        ProductShortExpiryDTO dto = list.get(0);
        assertThat(dto.getProductId()).isEqualTo(1);
        assertThat(dto.getName()).isEqualTo("Good");
        assertThat(dto.getDaysLeft()).isEqualTo(3);
        assertThat(dto.getWarehouseId()).isEqualTo(2);
        assertThat(dto.getWarehouseName()).isEqualTo("W2");
    }
}
