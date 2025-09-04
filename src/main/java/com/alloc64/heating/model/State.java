package com.alloc64.heating.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class State {
    private String message;
    private String serverDateTime;
    private List<ThermometerState> thermometerStates;
    private PumpConfig floorPumpConfig;
}
