package hr.fer.dipl.client.catalog;

import hr.fer.dipl.client.catalog.model.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

@FeignClient(
        name = "product-catalog-service", // service name registered with discovery (e.g., Eureka) or configured via URL
        url = "${catalog-service.url:http://localhost:8081}", // fallback for local/dev use
        path = "/products" // base path from ProductController
)
public interface ProductCatalogClient {

    @GetMapping("/{productId}")
    ProductDTO getProductById(@PathVariable("productId") Long productId);
}
