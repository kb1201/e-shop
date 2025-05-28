package hr.fer.dipl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProductEvent {
    private Long userId;
    private Long productId;
    private int quantity;
    private long timestamp;
}
