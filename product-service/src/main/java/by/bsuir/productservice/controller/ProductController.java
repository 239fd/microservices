package by.bsuir.productservice.controller;

import by.bsuir.productservice.DTO.*;
import by.bsuir.productservice.service.ProductService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
@CrossOrigin(maxAge = 3600L)
public class ProductController {

    private final ProductService productService;

    @PostMapping("/accept")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public ResponseEntity<byte[]> acceptProduct(@RequestBody List<ProductDTO> products, Principal principal) throws Exception {
        byte[] pdf = productService.acceptProduct(products, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt_order.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/barcode")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public ResponseEntity<byte[]> getBarcode(@RequestParam int productId) throws Exception {
        byte[] pdf = productService.generateBarcode(productId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt_order.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @PostMapping("/dispatch")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public ResponseEntity<byte[]> dispatchProducts(
            @RequestBody DispatchDTO dto,
            Principal principal) throws Exception {

        Map<String, byte[]> docs = productService.dispatchProducts(dto, principal);

        byte[] zipBytes = zipDocuments(docs);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dispatch_docs.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipBytes.length)
                .body(zipBytes);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> getAllProducts(Principal principal) throws Exception {
        return ResponseEntity.ok(productService.getAllStoredProducts(principal));
    }

    @PostMapping("/inventory")
    @PreAuthorize("hasAuthority('ROLE_ACCOUNTANT')")
    public ResponseEntity<byte[]> performInventory(
            @RequestBody InventoryDTO dto,
            Principal principal) throws Exception {

        byte[] pdf = productService.performInventoryCheck(dto, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt_order.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
    @PostMapping("/revaluation")
    @PreAuthorize("hasAuthority('ROLE_ACCOUNTANT')")
    public ResponseEntity<byte[]> revaluateProduct(
            @RequestBody RevaluateDTO dto,
            Principal principal) throws Exception {

        byte[] pdf = productService.revaluateProducts(dto, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt_order.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
    @PostMapping("/writeoff")
    @PreAuthorize("hasAuthority('ROLE_ACCOUNTANT')")
    public ResponseEntity<byte[]> writeoffProduct(
            @RequestBody WriteOffDTO dto,
            Principal principal) throws Exception {

        byte[] pdf = productService.writeOff(dto, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt_order.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(
            @RequestParam("query") String query) {
        List<ProductDTO> result = productService.searchProducts(query);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/by-cell-ids")
    public List<ProductDTO> getProductsByCellIds(@RequestBody List<Integer> cellIds) {
        return productService.getProductsByCellIds(cellIds);
    }
    private byte[] zipDocuments(Map<String, byte[]> docs) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Map.Entry<String, byte[]> entry : docs.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        }
    }

}
