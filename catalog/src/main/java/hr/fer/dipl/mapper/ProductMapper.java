package hr.fer.dipl.mapper;

import hr.fer.dipl.db.model.Product;
import hr.fer.dipl.dto.ProductDTO;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class ProductMapper {
    private static final String BASE_URL = "https://github.com/kb1201/e-shop-image-repo/blob/main/";
    private static final String QUERY_PARAM = "?raw=true";

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
                //TODO move to data
                BASE_URL + buildEncodedImageURL(product.getImagePath()) + QUERY_PARAM,
                product.getProductLink(),
                product.getSpecificCategory(),
                product.getCombinedText(),
                product.getImageName(),
                product.getImagePath()
        );
    }

    private static String buildEncodedImageURL(String imagePath) {
        try {
            // Ensure the imagePath is properly URL encoded

            return URLEncoder.encode(imagePath, StandardCharsets.UTF_8)
                    .replace("+", "%20")   // Encode space
                    .replace(",", "%2C")   // Encode comma
                    .replace("(", "%28")   // Encode (
                    .replace(")", "%29")   // Encode )
                    .replace("+", "%2B")   // Encode plus sign
                    .replace("&", "%26");  // Encode ampersand
        } catch (Exception e) {
            // Fallback to original path if encoding fails
            System.err.println("Error encoding image path: " + imagePath);
            return imagePath;
        }
    }

}
