package hr.fer.dipl.dto;

import lombok.Data;

@Data
public class PaymentDetails {
    private String cardNumber;
    private String cardholderName;
    private String expiryDate;
    private String cvv;
    private String paymentMethod;
}