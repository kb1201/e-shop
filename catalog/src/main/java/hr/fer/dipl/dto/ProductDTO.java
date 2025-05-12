package hr.fer.dipl.dto;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductDTO {

    private int id;
    private String name;
    private String category;
    private BigDecimal actualPrice;
    private BigDecimal discountPercentage;
    private BigDecimal rating;
    private Integer ratingCount;
    private String aboutProduct;
    private String imgLink;
    private String productLink;
    private String specificCategory;
    private String combinedText;
    private String imageName;
    private String imagePath;

}
