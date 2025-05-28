package hr.fer.dipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartUpdateResponse {
    private boolean success;
    private String message;
}