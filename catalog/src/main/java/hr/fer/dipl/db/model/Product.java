package hr.fer.dipl.db.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "products")
public class Product {

    @Id
    private int id;
    private String name;
    private String category;
    private String specificCategory;
    private BigDecimal discountedPrice;
    private BigDecimal actualPrice;
    private BigDecimal discountPercentage;
    private BigDecimal rating;
    private Integer ratingCount;
    private String aboutProduct;
    private String imgLink;
    private String productLink;
    private String combinedText;
    private String imageName;
    private String imagePath;
}
