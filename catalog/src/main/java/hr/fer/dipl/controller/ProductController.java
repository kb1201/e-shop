package hr.fer.dipl.controller;


import hr.fer.dipl.db.model.Product;
import hr.fer.dipl.dto.ProductDTO;
import hr.fer.dipl.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Get a product by ID
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> readProduct(@PathVariable("productId") int productId) {
        Optional<ProductDTO> product = productService.getProductById(productId);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        // Create a Pageable object with the provided page and size
        Pageable pageable = PageRequest.of(page, size);

        // Call the service to get paginated results
        Page<Product> productPage = productService.searchProducts(query, pageable);

        return ResponseEntity.ok(productPage);
    }

    // Get products by a list of IDs
    @GetMapping("/by_ids")
    public ResponseEntity<List<ProductDTO>> getProductsByIds(@RequestParam("ids") List<Integer> ids) {
        List<ProductDTO> products = productService.getProductsByIds(ids);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/popular")
    public ResponseEntity<Page<Product>> getPopularProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getMostPopularProducts(page, size));
    }
}
