package hr.fer.dipl.dto;

import hr.fer.dipl.db.model.OrderStatus;
import lombok.Data;

@Data
public class OrderRequest {
    private String shippingAddress;
    private String billingAddress;
    private String paymentMethod;
    private PaymentDetails paymentDetails;
    private OrderStatus status; // Used for status updates
}