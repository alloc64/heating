package com.alloc64.heating.model;

import lombok.Data;

@Data
public class ThermometerState {
    private String id;
    private String name;
    private Double temperature;

    public ThermometerState(String name, String floorHotThermometerId) {
        this.id = floorHotThermometerId;
        this.name = name;
    }
}
