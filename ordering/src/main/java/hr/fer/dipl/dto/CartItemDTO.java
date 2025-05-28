package hr.fer.dipl.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private int quantity;
}