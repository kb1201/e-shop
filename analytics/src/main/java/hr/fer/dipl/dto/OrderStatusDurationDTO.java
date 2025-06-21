package hr.fer.dipl.dto;

import lombok.Builder;
import lombok.Data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusDurationDTO {
    private String fromStatus;
    private String toStatus;
    private long statusOccurrences;
    private double avgHoursInStatus;
    private double medianHoursInStatus;
    private double maxHoursInStatus;
}

