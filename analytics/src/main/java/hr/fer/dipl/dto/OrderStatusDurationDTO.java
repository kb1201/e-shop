package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusDurationDTO {
    private String status;
    private long statusOccurrences;
    private double avgHoursInStatus;
    private double medianHoursInStatus;
    private long maxHoursInStatus;
}
