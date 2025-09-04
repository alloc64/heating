package com.alloc64.heating.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnableByTimeRequest {
    private String timeRange;
    private Double minTemp;
}
