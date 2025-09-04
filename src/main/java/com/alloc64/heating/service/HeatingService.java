package com.alloc64.heating.service;

import com.alloc64.heating.model.PumpConfig;
import com.alloc64.heating.model.State;
import com.alloc64.heating.model.ThermometerState;
import com.alloc64.heating.prefs.JsonSharedPreferences;
import com.pi4j.Pi4J;
import com.pi4j.boardinfo.definition.BoardModel;
import com.pi4j.boardinfo.util.BoardInfoHelper;
import com.pi4j.context.Context;
import com.pi4j.plugin.gpiod.provider.gpio.digital.GpioDDigitalOutputProvider;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class HeatingService implements AutoCloseable {
    public static final String FLOOR_PUMP_ID = "floor-pump";

    private final List<ThermometerState> thermometerStates;
    private final PumpConfig floorPumpConfig;
    private final Map<String, PumpConfig> pumpConfigs = new LinkedHashMap<>();

    private final JsonSharedPreferences sharedPreferences;
    private final Context pi4j;
    private final ThermometerState floorHotThermometer;

    private String serverTime;

    public HeatingService(
            @Value("${heating.thermometerid.floor.cold}") String floorColdThermometerId,
            @Value("${heating.thermometerid.floor.hot}") String floorHotThermometerId,
            @Value("${heating.thermometerid.tank.hot}") String tankHotThermometerId,
            @Value("${heating.thermometerid.tank.cold}") String tankColdThermometerId,
            @Value("${heating.pumprelay.gpio}") int pumpRelayGpioId,
            JsonSharedPreferences sharedPreferences
    ) {
        // for development out of Raspberry Pi
        //this.pi4j = Pi4J.newAutoContext();

        this.pi4j = Pi4J.newContextBuilder()
                .add(GpioDDigitalOutputProvider.newInstance())
                .build();
        this.floorHotThermometer = new ThermometerState("Teplá podlaha", floorHotThermometerId);

        this.thermometerStates = List.of(
                new ThermometerState("Vratka podlaha", floorColdThermometerId),
                floorHotThermometer,
                new ThermometerState("Teplá nádrž", tankHotThermometerId),
                new ThermometerState("Vratka nádrž", tankColdThermometerId)
        );

        this.sharedPreferences = sharedPreferences;
        this.floorPumpConfig = buildPumpConfig("Čerpadlo podlah", FLOOR_PUMP_ID, pumpRelayGpioId);

        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this::update, 0, 1, TimeUnit.SECONDS);
    }

    public State getState() {
        return new State()
                .setServerDateTime(serverTime)
                .setThermometerStates(thermometerStates)
                .setFloorPumpConfig(floorPumpConfig);
    }

    public void manualOverride(String pumpId, boolean enabled) {
        getPumpOrThrow(pumpId)
                .setManualOverride(enabled)
                .save(sharedPreferences);
    }

    public void setTempTrigger(String pumpId, Double triggerTemp) {
        getPumpOrThrow(pumpId)
                .setTempTrigger(triggerTemp)
                .save(sharedPreferences);
    }

    public void setTimeTrigger(String pumpId, String timeRange, Double minTemp) {
        getPumpOrThrow(pumpId)
                .setTimeTrigger(timeRange)
                .setTempTriggerMinTemp(minTemp)
                .save(sharedPreferences);
    }

    private void update() {
        this.serverTime = LocalDateTime.now().toString();

        for (ThermometerState ts : thermometerStates)
            ts.setTemperature(readTemperature(ts.getId()));

        // nothing more to do
        if(floorPumpConfig.isManualOverride())
            return;

        Double temperature = floorHotThermometer.getTemperature();

        // no temp, no business
        if(temperature == null)
            return;

        if(floorPumpConfig.isTimeTriggerEnabled()) {
            Double timeTriggerMinTemp = floorPumpConfig.getTempTriggerMinTemp();
            boolean validTemp = timeTriggerMinTemp == null || temperature >= timeTriggerMinTemp;

            LocalTime timeTriggerStartTime = floorPumpConfig.getTimeTriggerStartTime();
            LocalTime timeTriggerEndTime = floorPumpConfig.getTimeTriggerEndTime();

            LocalTime now = LocalDateTime.now().toLocalTime();
            boolean validTimeAndTemp = now.isAfter(timeTriggerStartTime) && now.isBefore(timeTriggerEndTime) && validTemp;

            floorPumpConfig.setEnabled(validTimeAndTemp, "Time trigger");

            if(validTimeAndTemp)
                return;
        }

        Double tempTrigger = floorPumpConfig.getTempTrigger();

        if(tempTrigger != null || floorPumpConfig.isEnabled()) {
            boolean validTemp = tempTrigger != null && temperature >= tempTrigger;
            floorPumpConfig.setEnabled(validTemp, "Temp trigger");

            if(validTemp)
                return; // for future extensions
        }
    }

    public Double readTemperature(String sensorId) {
        try {
            List<String> lines = Files.readAllLines(Paths.get("/sys/bus/w1/devices/" + sensorId + "/" + "w1_slave"));

            for (String line : lines) {
                int idx = line.indexOf("t=");

                if (idx != -1) {
                    String tempData = line.substring(idx + 2);
                    return Double.parseDouble(tempData) / 1000.0;
                }
            }
        } catch (IOException e) {
            // this may happen when sensor is not connected
        }

        return null;
    }

    private PumpConfig buildPumpConfig(String name, String id, int gpioId) {
        PumpConfig pumpConfig = new PumpConfig(pi4j, sharedPreferences, id, name, gpioId);

        pumpConfigs.put(id, pumpConfig);
        return pumpConfig;
    }

    private PumpConfig getPumpOrThrow(String pumpId) {
        PumpConfig pump = pumpConfigs.get(pumpId);

        if(pump == null)
            throw new NoSuchElementException("No pump with id " + pumpId);

        return pump;
    }

    @Override
    public void close() throws Exception {
        pi4j.shutdown();
    }
}
