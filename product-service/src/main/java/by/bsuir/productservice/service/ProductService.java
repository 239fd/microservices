package by.bsuir.productservice.service;

import by.bsuir.productservice.DTO.DispatchDTO;
import by.bsuir.productservice.DTO.ProductDTO;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Service
public interface ProductService {
    byte[] acceptProduct(List<ProductDTO> products, Principal principal) throws Exception;

    byte[] generateBarcode(int productId) throws Exception;

    Map<String,byte[]> dispatchProducts(DispatchDTO dto, Principal principal) throws Exception;
}
