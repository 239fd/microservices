package by.bsuir.productservice.service;

import by.bsuir.productservice.DTO.*;
import com.itextpdf.text.DocumentException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@Service
public interface ProductService {

    Map<String,byte[]> dispatchProducts(DispatchDTO dto, Principal principal) throws Exception;

    byte[] performInventoryCheck(InventoryDTO dto, Principal principal) throws DocumentException, IOException;

    byte[] revaluateProducts(RevaluateDTO dto, Principal principal) throws DocumentException, IOException;

    byte[] writeOff(WriteOffDTO dto, Principal principal) throws DocumentException, IOException;

    List<ProductDTO> searchProducts(String query);

    List<ProductDTO> getAllStoredProducts(Principal principal);

    List<ProductDTO> getProductsByCellIds(List<Integer> cellIds);

    byte[] acceptProductWithBarcodes(List<ProductDTO> products, Principal principal) throws Exception;

    void deleteProduct(int id, Principal principal);

    ProductDTO updateProduct(int id, ProductDTO dto, Principal principal);
}
