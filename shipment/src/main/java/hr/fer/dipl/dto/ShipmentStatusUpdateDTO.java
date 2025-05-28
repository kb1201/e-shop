package hr.fer.dipl.dto;

import hr.fer.dipl.db.model.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdateDTO {
    private ShipmentStatus status;
}