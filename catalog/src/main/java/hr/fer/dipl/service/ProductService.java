package hr.fer.dipl.service;

import hr.fer.dipl.dto.ProductDTO;
import hr.fer.dipl.dto.ProductNameDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    Optional<ProductDTO> getProductById(int productId);

    Page<ProductDTO> searchProducts(String query, Pageable pageable);

    List<ProductDTO> getProductsByIds(List<Integer> ids);

    Page<ProductDTO> getMostPopularProducts(int page, int size);

    Page<ProductDTO> getRecommendations(int page, int size);

    List<ProductNameDTO> getProductNamesByIds(List<Long> ids);
}
