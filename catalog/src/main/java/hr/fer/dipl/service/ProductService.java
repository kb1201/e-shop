package hr.fer.dipl.service;

import hr.fer.dipl.client.recommendation.RecommendationSystemClient;
import hr.fer.dipl.client.recommendation.model.RecommendationRequest;
import hr.fer.dipl.db.model.Product;
import hr.fer.dipl.db.repository.ProductRepository;
import hr.fer.dipl.dto.ProductDTO;
import hr.fer.dipl.dto.ProductNameDTO;
import hr.fer.dipl.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RecommendationSystemClient client;
    private final SecurityUtils securityUtils;

    @Autowired
    public ProductService(ProductRepository productRepository, RecommendationSystemClient client, SecurityUtils securityUtils) {
        this.productRepository = productRepository;
        this.client = client;
        this.securityUtils = securityUtils;
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

    public Page<ProductDTO> getRecommendations(int page, int size) {
        var userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        try {
            var response = client.getRecommendations(new RecommendationRequest(userId, page, size));

            var products = productRepository
                    .findByIdIn(response.getRecommendations()).stream()
                    .map(ProductMapper::toDTO).toList();

            return new PageImpl<>(products, pageable, response.getPagination().getTotalItems());
        } catch (Exception exc) {
            exc.printStackTrace();
            return getMostPopularProducts(page, size);
        }

    }

    public List<ProductNameDTO> getProductNamesByIds(List<Long> ids) {
        List<Product> products = productRepository.findByIdIn(ids);
        return products.stream()
                .map(p -> new ProductNameDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }
}
