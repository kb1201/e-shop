package hr.fer.dipl.client.catalog.model;

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
}
