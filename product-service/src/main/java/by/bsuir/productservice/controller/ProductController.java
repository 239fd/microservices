package by.bsuir.productservice.controller;

import by.bsuir.productservice.DTO.DispatchDTO;
import by.bsuir.productservice.DTO.ProductDTO;
import by.bsuir.productservice.service.ProductService;
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
