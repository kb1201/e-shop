package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentStatusDurationDTO {
    private String status;
    private long statusOccurrences;
    private double avgHoursInStatus;
    private double medianHoursInStatus;
    private double p95HoursInStatus;
}
