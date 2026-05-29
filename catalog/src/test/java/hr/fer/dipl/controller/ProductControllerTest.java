package hr.fer.dipl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.fer.dipl.dto.ProductDTO;
import hr.fer.dipl.dto.ProductNameDTO;
import hr.fer.dipl.service.JwtService;
import hr.fer.dipl.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;
    // Required to satisfy WebSecurityConfiguration — not used in tests directly
    @MockBean JwtService jwtService;

    private ProductDTO sampleProduct() {
        return new ProductDTO(
                1,
                "Laptop Pro 15",
                "Electronics",
                new BigDecimal("999.99"),
                new BigDecimal("10.00"),
                new BigDecimal("4.5"),
                342,
                "High-performance 15-inch laptop with 16 GB RAM",
                "https://img.example.com/laptop-pro.jpg",
                "https://store.example.com/products/laptop-pro",
                "Computers > Laptops",
                "Laptop Pro 15 Electronics high performance",
                "laptop-pro.jpg",
                "/images/laptop-pro.jpg"
        );
    }

    // --- GET /products/{productId} ---

    @Test
    void should_return200_when_productExists() throws Exception {
        ProductDTO product = sampleProduct();
        given(productService.getProductById(1)).willReturn(Optional.of(product));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.category").value("Electronics"));
    }

    @Test
    void should_return404_when_productNotFound() throws Exception {
        given(productService.getProductById(99)).willReturn(Optional.empty());

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound());
    }

    // --- GET /products/search ---

    @Test
    void should_return200WithContent_when_searchQueryMatches() throws Exception {
        var page = new PageImpl<>(List.of(sampleProduct()), PageRequest.of(0, 10), 1);
        given(productService.searchProducts(eq("laptop"), any())).willReturn(page);

        mockMvc.perform(get("/products/search").param("q", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return200EmptyPage_when_noQueryProvided() throws Exception {
        given(productService.searchProducts(isNull(), any()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // --- GET /products/recommendations ---

    @Test
    void should_return200WithPage_when_recommendationsRequested() throws Exception {
        var page = new PageImpl<>(List.of(sampleProduct()));
        given(productService.getRecommendations(0, 10)).willReturn(page);

        mockMvc.perform(get("/products/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void should_return200EmptyPage_when_noRecommendationsAvailable() throws Exception {
        given(productService.getRecommendations(0, 10))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/products/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // --- GET /products/names ---

    @Test
    void should_returnNameList_when_idsProvided() throws Exception {
        var names = List.of(
                new ProductNameDTO(1, "Laptop Pro 15"),
                new ProductNameDTO(2, "Wireless Mouse")
        );
        given(productService.getProductNamesByIds(anyList())).willReturn(names);

        mockMvc.perform(get("/products/names").param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Wireless Mouse"));
    }

    // --- GET /products/popular ---

    @Test
    void should_return200WithPage_when_popularProductsRequested() throws Exception {
        var page = new PageImpl<>(List.of(sampleProduct()));
        given(productService.getMostPopularProducts(0, 10)).willReturn(page);

        mockMvc.perform(get("/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void should_return200WithPage_when_popularProductsRequestedWithCustomPaging() throws Exception {
        var page = new PageImpl<>(List.of(sampleProduct()), PageRequest.of(1, 5), 6);
        given(productService.getMostPopularProducts(1, 5)).willReturn(page);

        mockMvc.perform(get("/products/popular").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }
}
