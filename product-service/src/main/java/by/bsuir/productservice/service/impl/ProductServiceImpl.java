package by.bsuir.productservice.service.impl;

import by.bsuir.productservice.DTO.*;
import by.bsuir.productservice.entity.CellHasProduct;
import by.bsuir.productservice.entity.CellHasProductKey;
import by.bsuir.productservice.entity.Product;
import by.bsuir.productservice.feign.EmployeeClient;
import by.bsuir.productservice.feign.WarehouseClient;
import by.bsuir.productservice.repository.CellHasProductRepository;
import by.bsuir.productservice.repository.ProductRepository;
import by.bsuir.productservice.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final WarehouseClient warehouseClient;
    private final CellHasProductRepository cellHasProductRepository;
    private final EmployeeClient employeeClient;
    private final PdfService pdfService;

    @Override
    @Transactional
    public byte[] acceptProduct(List<ProductDTO> products, Principal principal) throws Exception {
        WarehouseDTO warehouse = warehouseClient.getByUser(principal.getName())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        List<CellInfo> allCells = warehouse.getRacks().stream()
                .sorted(Comparator.comparing(RackDTO::getId))
                .flatMap(rack -> rack.getCells().stream()
                        .map(cell -> new CellInfo(rack, cell)))
                .sorted(Comparator.comparing((CellInfo ci) -> ci.cell.getId()))
                .toList();
        if (allCells.isEmpty()) {
            throw new RuntimeException("No cells in warehouse");
        }

        Set<Integer> usedCellIds = cellHasProductRepository.findAll().stream()
                .map(rel -> rel.getId().getCellId())
                .collect(Collectors.toSet());

        List<Placement> placements = new ArrayList<>();
        for (ProductDTO dto : products) {
            boolean placed = false;
            for (CellInfo ci : allCells) {
                if (usedCellIds.contains(ci.cell.getId())) continue;

                double rackUsedWeight = calculateRackUsedWeight(ci.rack);
                double prodWeight = dto.getWeight() * dto.getAmount();
                if (ci.rack.getCapacity() < rackUsedWeight + prodWeight) continue;

                double availableVol = calculateCellAvailableVolume(ci.cell);
                double prodVol = dto.getLength() * dto.getWidth() * dto.getHeight();
                if (availableVol < prodVol) continue;

                if (ci.cell.getLength() < dto.getLength() || ci.cell.getWidth() < dto.getWidth() || ci.cell.getHeight() < dto.getHeight())
                    continue;

                placements.add(new Placement(ci.rack, ci.cell, dto));
                usedCellIds.add(ci.cell.getId());
                placed = true;
                break;
            }
            if (!placed) {
                throw new RuntimeException("No suitable cell found for product: " + dto.getName());
            }
        }

        List<Integer> savedIds = new ArrayList<>();
        Map<Integer, String> idToEan = new HashMap<>();
        for (Placement pl : placements) {
            Product product = createProduct(pl.dto);
            productRepository.save(product);
            String ean = String.format("%03d%03d%03d%03d",
                    warehouse.getId() % 1000, pl.rack.getId() % 1000, pl.cell.getId() % 1000, product.getId() % 1000);
            CellHasProductKey key = new CellHasProductKey(pl.cell.getId(), product.getId());
            cellHasProductRepository.save(new CellHasProduct(key, ean));
            savedIds.add(product.getId());
            System.out.println(ean);
            idToEan.put(product.getId(), ean);
        }
        return pdfService.generateReceiptOrderPDF(products, savedIds, idToEan, getFullName(principal.getName()));
    }

    @Override
    @Transactional
    public Map<String, byte[]> dispatchProducts(DispatchDTO dto, Principal principal) throws Exception {

        EmployeeDto employee = employeeClient.getByLogin(principal.getName());
        WarehouseDTO warehouse = warehouseClient.getByUser(principal.getName())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        List<Product> products = IntStream.range(0, dto.getProductIds().size())
                .mapToObj(i -> {
                    Integer productId = dto.getProductIds().get(i);
                    return productRepository.findById(productId)
                            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
                })
                .toList();

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getAmount() < dto.getAmounts().get(i)) {
                throw new RuntimeException("Insufficient quantity for product ID " + products.get(i).getId());
            }
        }

        List<ProductDTO> dispatchedDtos = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            int amountToDispatch = dto.getAmounts().get(i);
            p.setAmount(p.getAmount() - amountToDispatch);

            if (p.getAmount() == 0) {
                cellHasProductRepository.deleteAll(
                        cellHasProductRepository.findAllByIdProductId(p.getId())
                );
                productRepository.delete(p);
            } else {
                productRepository.save(p);
            }

            ProductDTO pdto = new ProductDTO();
            pdto.setId(p.getId());
            pdto.setName(p.getName());
            pdto.setAmount(amountToDispatch);
            pdto.setPrice(p.getPrice());
            pdto.setUnit(p.getUnit());
            dispatchedDtos.add(pdto);

        }

        byte[] orderPdf = pdfService.generateDispatchOrderPDF(
                dispatchedDtos,
                dto.getProductIds(),
                getFullName(employee)
        );

        byte[] ttnPdf = pdfService.generateTTN(
                dto,
                dispatchedDtos,
                employee,
                warehouse
        );

        byte[] tnPdf = pdfService.generateTN(
                dto,
                dispatchedDtos,
                employee,
                warehouse
        );

        Map<String, byte[]> result = new LinkedHashMap<>();
        result.put("dispatch_order.pdf", orderPdf);
        result.put("TTN.pdf", ttnPdf);
        result.put("TN.pdf", tnPdf);
        return result;
    }

    private String getFullName(EmployeeDto emp) {
        return emp.getFirstName()
                + (emp.getSecondName() != null ? " " + emp.getSecondName() : "");
    }

    @Override
    public byte[] generateBarcode(int productId) throws Exception {
        List<CellHasProduct> products = cellHasProductRepository.findAllByIdProductId(productId);

        if (products.isEmpty()) {
            throw new IllegalArgumentException("No barcode found for product with ID: " + productId);
        }

        String ean = products.get(0).getBarcodePdf();
        return pdfService.generateBarcodePDF(ean);
    }

    private String getFullName(String login) {
        EmployeeDto employeeDto = employeeClient.getByLogin(login);
        if (employeeDto.getFirstName() != null && employeeDto.getSecondName() != null ) {
            return employeeDto.getFirstName() + " " + employeeDto.getSecondName();
        }
        else{
            return employeeDto.getFirstName();
        }
    }

    private record CellInfo(RackDTO rack, CellDTO cell) {
    }

    private static class Placement {
        final RackDTO rack;
        final CellDTO cell;
        final ProductDTO dto;

        Placement(RackDTO r, CellDTO c, ProductDTO d) {
            rack = r;
            cell = c;
            dto = d;
        }
    }

    private double calculateRackUsedWeight(RackDTO rack) {
        return rack.getCells().stream()
                .flatMap(cell -> cellHasProductRepository.findAllByCellId(cell.getId()).stream())
                .mapToDouble(rel -> productRepository.findById(rel.getProductId()).orElseThrow().getWeight())
                .sum();
    }

    private double calculateCellAvailableVolume(CellDTO cell) {
        double cellVol = cell.getLength() * cell.getWidth() * cell.getHeight();
        double usedVol = cellHasProductRepository.findAllByCellId(cell.getId()).stream()
                .mapToDouble(rel -> {
                    Product p = productRepository.findById(rel.getProductId()).orElseThrow();
                    return p.getLength() * p.getWidth() * p.getHeight() * p.getAmount();
                }).sum();
        return cellVol - usedVol;
    }

    private Product createProduct(ProductDTO dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setAmount(dto.getAmount());
        p.setLength(dto.getLength());
        p.setWidth(dto.getWidth());
        p.setHeight(dto.getHeight());
        p.setWeight(dto.getWeight());
        p.setPrice(dto.getPrice());
        p.setUnit(dto.getUnit());
        p.setBestBeforeDate(dto.getBestBeforeDate());
        p.setStatus("accepted");
        return p;
    }
}