package hr.fer.dipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class ProductInteraction {
    private String productId;
    private String userId;
    private double interactionValue;
    private Instant timestamp;
}
