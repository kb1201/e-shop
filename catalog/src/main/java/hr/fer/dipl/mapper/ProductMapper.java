package hr.fer.dipl.mapper;

import hr.fer.dipl.db.model.Product;
import hr.fer.dipl.dto.ProductDTO;


public class ProductMapper {
    private static final String BASE_URL = "https://raw.githubusercontent.com/kb1201/e-shop-image-repo/";

    public static ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getActualPrice(),
                product.getDiscountPercentage(),
                product.getRating(),
                product.getRatingCount(),
                product.getAboutProduct(),
                BASE_URL + product.getImagePath(),
                product.getProductLink(),
                product.getSpecificCategory(),
                product.getCombinedText(),
                product.getImageName(),
                product.getImagePath()
        );
    }
}
