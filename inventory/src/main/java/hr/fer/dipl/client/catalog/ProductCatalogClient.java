package hr.fer.dipl.client.catalog;

import hr.fer.dipl.client.catalog.model.ProductNameDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "product-catalog-service",
        url = "${catalog-service.url:http://localhost:8081}",
        path = "/products"
)
public interface ProductCatalogClient {

    @GetMapping("/names")
    List<ProductNameDTO> getProductNames(@RequestParam("ids") List<Long> ids);
}