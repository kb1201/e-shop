
package hr.fer.dipl.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
}