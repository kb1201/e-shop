package hr.fer.dipl.service;

import hr.fer.dipl.db.model.Product;
import hr.fer.dipl.db.repository.ProductRepository;
import hr.fer.dipl.dto.ProductDTO;
import hr.fer.dipl.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Optional<ProductDTO> getProductById(int productId) {
        return productRepository.findById(productId)
                .map(ProductMapper::toDTO);
    }

    public Page<ProductDTO> searchProducts(String query, Pageable pageable) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query must not be empty");
        }
        var products = productRepository.findByNameContainingIgnoreCase(query, pageable);

        return products.map(ProductMapper::toDTO);
    }

    public List<ProductDTO> getProductsByIds(List<Integer> ids) {
        return productRepository.findAllById(ids).stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    public Page<ProductDTO> getMostPopularProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // Choose one of the methods depending on your popularity definition
        var products = productRepository.findMostPopularByRating(pageable);
        // Map the list of products to a list of ProductDTO

        return products.map(ProductMapper::toDTO);
    }
}
