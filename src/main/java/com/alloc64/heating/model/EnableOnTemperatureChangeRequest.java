package com.alloc64.heating.model;

import lombok.Data;

@Data
public class EnableOnTemperatureChangeRequest {
    private Double triggerTemp;
}
