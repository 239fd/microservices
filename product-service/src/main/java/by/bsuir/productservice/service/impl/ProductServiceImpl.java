package by.bsuir.productservice.service.impl;

import by.bsuir.productservice.DTO.*;
import by.bsuir.productservice.entity.CellHasProduct;
import by.bsuir.productservice.entity.CellHasProductKey;
import by.bsuir.productservice.entity.Product;
import by.bsuir.productservice.exeption.AppException;
import by.bsuir.productservice.feign.EmployeeClient;
import by.bsuir.productservice.feign.WarehouseClient;
import by.bsuir.productservice.repository.CellHasProductRepository;
import by.bsuir.productservice.repository.ProductRepository;
import by.bsuir.productservice.service.ProductService;
import com.itextpdf.text.DocumentException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    public byte[] acceptProductWithBarcodes(List<ProductDTO> products, Principal principal) throws Exception {
        WarehouseDTO warehouse = warehouseClient.getByUser(principal.getName())
                .stream().findFirst()
                .orElseThrow(() -> new AppException("Warehouse not found", HttpStatus.NOT_FOUND));

        EmployeeDto worker = employeeClient.getByLogin(principal.getName()).getData();
        if (!"worker".equalsIgnoreCase(worker.getTitle())) {
            throw new SecurityException("Only a worker can make it");
        }

        List<CellInfo> allCells = warehouse.getRacks().stream()
                .sorted(Comparator.comparing(RackDTO::getId))
                .flatMap(rack -> rack.getCells().stream()
                        .map(cell -> new CellInfo(rack, cell)))
                .sorted(Comparator.comparing((CellInfo ci) -> ci.cell.getId()))
                .toList();
        for (CellInfo cell : allCells) {
            System.out.println(cell.toString());
        }
        if (allCells.isEmpty()) {
            throw new AppException("No cells in warehouse", HttpStatus.CONFLICT);
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
                throw new AppException("No suitable cell found for product: " + dto.getName(), HttpStatus.CONFLICT);
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
        byte[] orderPdf = pdfService.generateReceiptOrderPDF(products, savedIds, idToEan, getFullName(principal.getName()));

        record BarcodeFile(int productId, byte[] pdf) {
        }
        List<BarcodeFile> barcodes = new ArrayList<>();

        for (Map.Entry<Integer, String> e : idToEan.entrySet()) {
            byte[] codePdf = pdfService.generateBarcodePDF(e.getValue());
            barcodes.add(new BarcodeFile(e.getKey(), codePdf));
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            zos.putNextEntry(new ZipEntry("receipt_order.pdf"));
            zos.write(orderPdf);
            zos.closeEntry();

            for (BarcodeFile bf : barcodes) {
                String name = "barcode_" + bf.productId() + ".pdf";
                zos.putNextEntry(new ZipEntry(name));
                zos.write(bf.pdf());
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        }

    }

    @Override
    @Transactional
    public Map<String, byte[]> dispatchProducts(DispatchDTO dto, Principal principal) throws Exception {

        EmployeeDto employee = employeeClient.getByLogin(principal.getName()).getData();
        if (!"worker".equalsIgnoreCase(employee.getTitle())) {
            throw new SecurityException("Only a worker can make it");
        }

        WarehouseDTO warehouse = warehouseClient.getByUser(principal.getName())
                .stream().findFirst()
                .orElseThrow(() -> new AppException("Warehouse not found", HttpStatus.NOT_FOUND));

        List<Integer> cellIds = getWarehouseCellIds(principal.getName());

        List<Product> products = dto.getProductIds().stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new AppException("Product not found: " + id, HttpStatus.NOT_FOUND)))
                .peek(p -> {
                    boolean belongs = cellHasProductRepository.findAllByIdProductId(p.getId()).stream()
                            .anyMatch(assoc -> cellIds.contains(assoc.getId().getCellId()));
                    if (!belongs) {
                        throw new AppException(
                                "Product ID " + p.getId() + " is not in your warehouse", HttpStatus.FORBIDDEN);
                    }
                })
                .toList();

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getAmount() < dto.getAmounts().get(i)) {
                throw new AppException("Insufficient quantity for product ID " + products.get(i).getId(), HttpStatus.CONFLICT);
            }
            removeIfZero(products.get(i).getId());
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

    @Override
    @Transactional
    public byte[] performInventoryCheck(InventoryDTO dto, Principal principal) throws DocumentException, IOException {

        String username = principal.getName();
        EmployeeDto accountant = employeeClient.getByLogin(username).getData();
        if (!"accountant".equalsIgnoreCase(accountant.getTitle())) {
            throw new SecurityException("Only a accountant can make it");
        }
        if (dto.getIds().size() != dto.getAmounts().size()) {
            throw new AppException("Mismatched inventory data", HttpStatus.BAD_REQUEST);
        }

        List<Integer> cellIds = getWarehouseCellIds(username);
        List<ProductDTO> stored = getAllStoredProducts(principal).stream()
                .peek(pDto -> {
                    boolean belongs = cellHasProductRepository.findAllByIdProductId(pDto.getId()).stream()
                            .anyMatch(assoc -> cellIds.contains(assoc.getId().getCellId()));
                    if (!belongs) {
                        throw new AppException(
                                "Product ID " + pDto.getId() + " is not in your warehouse", HttpStatus.FORBIDDEN);
                    }
                })
                .toList();

        Map<Integer, Integer> actualMap = IntStream.range(0, dto.getIds().size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> dto.getIds().get(i),
                        i -> dto.getAmounts().get(i)
                ));
        List<ProductDTO> reportProducts = new ArrayList<>();
        List<Integer> expected = new ArrayList<>();
        List<Integer> actual = new ArrayList<>();
        for (ProductDTO pDto : stored) {
            int id = pDto.getId();
            int oldQty = pDto.getAmount();
            int newQty = actualMap.getOrDefault(id, 0);
            reportProducts.add(pDto);
            expected.add(oldQty);
            actual.add(newQty);
            Product prod = productRepository.findById(id)
                    .orElseThrow(() -> new AppException("Product not found: " + id, HttpStatus.NOT_FOUND));
            prod.setAmount(newQty);
            productRepository.save(prod);
            removeIfZero(pDto.getId());
        }
        return pdfService.generateInventoryReport(
                getFullName(principal.getName()),
                warehouseClient.getByUser(principal.getName()).stream().findFirst()
                        .orElseThrow(() -> new AppException("Warehouse not found", HttpStatus.NOT_FOUND)),
                reportProducts,
                expected,
                actual
        );
    }

    @Override
    @Transactional
    public byte[] revaluateProducts(RevaluateDTO dto, Principal principal) throws DocumentException, IOException {

        String username = principal.getName();
        EmployeeDto accountant = employeeClient.getByLogin(username).getData();
        if (!"accountant".equalsIgnoreCase(accountant.getTitle())) {
            throw new SecurityException("Only a accountant can make it");
        }
        if (dto.getProductIds().size() != dto.getNewPrice().size()) {
            throw new AppException("Mismatched revaluation data", HttpStatus.BAD_REQUEST);
        }

        List<Integer> cellIds = getWarehouseCellIds(username);
        List<ProductDTO> products = new ArrayList<>();
        List<Double> oldPrices = new ArrayList<>();
        List<Double> newPrices = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (int i = 0; i < dto.getProductIds().size(); i++) {
            Integer id = dto.getProductIds().get(i);
            Double price = dto.getNewPrice().get(i);
            boolean belongs = cellHasProductRepository.findAllByIdProductId(id).stream()
                    .anyMatch(assoc -> cellIds.contains(assoc.getId().getCellId()));
            if (!belongs) {
                throw new AppException(
                        "Product ID " + id + " is not in your warehouse", HttpStatus.FORBIDDEN);
            }

            Product prod = productRepository.findById(id)
                    .orElseThrow(() -> new AppException("Product not found: " + id, HttpStatus.NOT_FOUND));
            products.add(toDTO(prod));
            oldPrices.add(prod.getPrice());
            newPrices.add(price);
            quantities.add(prod.getAmount());

            prod.setPrice(price);
            productRepository.save(prod);
            removeIfZero(id);
        }
        return pdfService.generateRevaluationReport(products, oldPrices, newPrices, quantities);
    }

    @Override
    @Transactional
    public byte[] writeOff(WriteOffDTO dto, Principal principal) throws DocumentException, IOException {

        String username = principal.getName();
        validateWriteOffDto(dto);
        EmployeeDto employee = employeeClient.getByLogin(username).getData();
        if (!"accountant".equalsIgnoreCase(employee.getTitle())) {
            throw new SecurityException("Only a accountant can make it");
        }

        List<Integer> cellIds = getWarehouseCellIds(username);
        String chairman = getFullName(employee);

        List<ProductDTO> reportProducts = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        for (int i = 0; i < dto.getProductId().size(); i++) {
            Integer id = dto.getProductId().get(i);
            Integer qty = dto.getQuantity().get(i);
            boolean belongs = cellHasProductRepository.findAllByIdProductId(id).stream()
                    .anyMatch(assoc -> cellIds.contains(assoc.getId().getCellId()));
            if (!belongs) {
                throw new AppException(
                        "Product ID " + id + " is not in your warehouse", HttpStatus.FORBIDDEN);
            }
            Product prod = productRepository.findById(id)
                    .orElseThrow(() -> new AppException("Product not found: " + id, HttpStatus.NOT_FOUND));
            if (prod.getAmount() < qty) {
                throw new AppException("Not enough quantity for product: " + id, HttpStatus.BAD_REQUEST);
            }
            reportProducts.add(toDTO(prod));
            reportProducts.get(i).setAmount(qty);
            quantities.add(qty);
            prod.setAmount(prod.getAmount() - qty);
            productRepository.save(prod);
            removeIfZero(id);
        }

        return pdfService.generateWriteOffAct(
                java.time.LocalDate.now(),
                chairman,
                reportProducts,
                dto.getReason()
        );
    }

    @Override
    @Transactional
    public List<ProductDTO> searchProducts(String query) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        employeeClient.getByLogin(username);

        List<Integer> cellIds = getWarehouseCellIds(username);
        if (cellIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CellHasProduct> associations = cellHasProductRepository.findAllByIdCellIdIn(cellIds);

        String lowerQuery = query.toLowerCase();
        Set<Integer> matchedIds = associations.stream()
                .filter(assoc -> {
                    boolean barcodeMatches = assoc.getBarcodePdf() != null && assoc.getBarcodePdf().toLowerCase().contains(lowerQuery);
                    boolean nameMatches = productRepository.findById(assoc.getProductId())
                            .map(p -> p.getName().toLowerCase().contains(lowerQuery))
                            .orElse(false);
                    return barcodeMatches || nameMatches;
                })
                .map(CellHasProduct::getProductId)
                .collect(Collectors.toSet());

        return matchedIds.stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new AppException("Product not found: " + id, HttpStatus.NOT_FOUND)))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getAllStoredProducts(Principal principal) {
        employeeClient.getByLogin(principal.getName());
        WarehouseDTO warehouse = warehouseClient.getByUser(principal.getName())
                .stream().findFirst()
                .orElseThrow(() -> new AppException("Warehouse not found", HttpStatus.NOT_FOUND));

        List<CellInfo> allCells = warehouse.getRacks().stream()
                .sorted(Comparator.comparing(RackDTO::getId))
                .flatMap(rack -> rack.getCells().stream()
                        .map(cell -> new CellInfo(rack, cell)))
                .sorted(Comparator.comparing((CellInfo ci) -> ci.cell.getId()))
                .toList();
        if (allCells.isEmpty()) {
            throw new AppException("No cells in warehouse", HttpStatus.NOT_FOUND);
        }

        List<Integer> productIds = new ArrayList<>();
        for (CellInfo allCell : allCells) {
            List<CellHasProduct> info = cellHasProductRepository.findAllByCellId(allCell.cell().getId());
            for (CellHasProduct p : info) {
                productIds.add(p.getProductId());
            }
        }

        List<Product> products = new ArrayList<>();
        for (Integer productId : productIds) {
            products.add(productRepository.findById(productId)
                    .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND)));
        }
        List<ProductDTO> result = new ArrayList<>();
        for (Product p : products) {
            result.add(toDTO(p));
        }

        return result;
    }

    @Override
    public List<ProductDTO> getProductsByCellIds(List<Integer> cellIds) {
        List<CellHasProduct> links = cellHasProductRepository.findAllByIdCellIdIn(cellIds);

        Map<Integer, Integer> productToCellMap = links.stream()
                .collect(Collectors.toMap(
                        link -> link.getId().getProductId(),
                        link -> link.getId().getCellId(),
                        (existing, replacement) -> existing
                ));

        List<Product> products = productRepository.findAllById(productToCellMap.keySet());

        return products.stream().map(product -> {
            ProductDTO dto = new ProductDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setAmount(product.getAmount());
            dto.setPrice(product.getPrice());
            dto.setBestBeforeDate(product.getBestBeforeDate());
            return dto;
        }).collect(Collectors.toList());
    }

    private void removeIfZero(Integer productId) {
        productRepository.findById(productId).ifPresent(prod -> {
            if (prod.getAmount() == 0) {
                List<CellHasProduct> chpList = cellHasProductRepository.findAllByIdProductId(productId);
                if (!chpList.isEmpty()) {
                    cellHasProductRepository.deleteAll(chpList);
                }
                productRepository.delete(prod);
            }
        });
    }

    private void validateWriteOffDto(WriteOffDTO dto) {
        if (dto.getProductId().size() != dto.getQuantity().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mismatched write-off data");
        }
    }

    private String getFullName(EmployeeDto emp) {
        return emp.getFirstName()
                + (emp.getSecondName() != null ? " " + emp.getSecondName() : "");
    }

    private String getFullName(String login) {
        EmployeeDto employeeDto = employeeClient.getByLogin(login).getData();
        if (employeeDto.getFirstName() != null && employeeDto.getSecondName() != null) {
            return employeeDto.getFirstName() + " " + employeeDto.getSecondName();
        } else {
            return employeeDto.getFirstName();
        }
    }

    private record CellInfo(RackDTO rack, CellDTO cell) {
    }

    private List<Integer> getWarehouseCellIds(String username) {
        WarehouseDTO warehouse = warehouseClient.getByUser(username)
                .stream().findFirst()
                .orElseThrow(() -> new AppException("Warehouse not found", HttpStatus.NOT_FOUND));
        return warehouse.getRacks().stream()
                .flatMap(r -> r.getCells().stream())
                .map(CellDTO::getId)
                .collect(Collectors.toList());
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

    private ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setAmount(p.getAmount());
        dto.setLength(p.getLength());
        dto.setWidth(p.getWidth());
        dto.setHeight(p.getHeight());
        dto.setWeight(p.getWeight());
        dto.setPrice(p.getPrice());
        dto.setUnit(p.getUnit());
        dto.setBestBeforeDate(p.getBestBeforeDate());
        return dto;
    }
}