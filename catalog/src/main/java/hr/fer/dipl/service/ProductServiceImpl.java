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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final RecommendationSystemClient client;
    private final SecurityUtils securityUtils;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository,
                              RecommendationSystemClient client,
                              SecurityUtils securityUtils) {
        this.productRepository = productRepository;
        this.client = client;
        this.securityUtils = securityUtils;
    }

    @Override
    public Optional<ProductDTO> getProductById(int productId) {
        return productRepository.findById(productId)
                .map(ProductMapper::toDTO);
    }

    @Override
    public Page<ProductDTO> searchProducts(String query, Pageable pageable) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query must not be empty");
        }
        return productRepository.findByNameContainingIgnoreCase(query, pageable)
                .map(ProductMapper::toDTO);
    }

    @Override
    public List<ProductDTO> getProductsByIds(List<Integer> ids) {
        return productRepository.findAllById(ids).stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    @Override
    public Page<ProductDTO> getMostPopularProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findMostPopularByRating(pageable)
                .map(ProductMapper::toDTO);
    }

    @Override
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

    @Override
    public List<ProductNameDTO> getProductNamesByIds(List<Long> ids) {
        List<Product> products = productRepository.findByIdIn(ids);
        return products.stream()
                .map(p -> new ProductNameDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }
}
