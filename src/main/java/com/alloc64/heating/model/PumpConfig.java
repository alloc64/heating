package com.alloc64.heating.model;

import com.alloc64.heating.prefs.JsonSharedPreferences;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalTime;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class PumpConfig {
    private String id;
    private String name;
    private boolean manualOverride;
    private Double tempTrigger;

    private String timeTrigger; // "08-18"
    private LocalTime timeTriggerStartTime;
    private LocalTime timeTriggerEndTime;
    private Double tempTriggerMinTemp;

    private final DigitalOutput digitalOutput;

    private String enableSource;

    public PumpConfig(Context pi4j,
                      JsonSharedPreferences sharedPreferences,
                      String id,
                      String name,
                      int gpioId) {
        this.id = id;
        this.name = name;

        this.digitalOutput = pi4j.create(
                DigitalOutput.newConfigBuilder(pi4j)
                        .id(id)
                        .name(name)
                        .address(gpioId)
                        .shutdown(DigitalState.HIGH)
                        .initial(DigitalState.HIGH)
                        .build()
        );

        this.setManualOverride(sharedPreferences.getBoolean(buildManualOverrideKey(), false));

        this.tempTrigger = sharedPreferences.getDouble(buildTempTriggerKey(), null);
        this.setTimeTrigger(sharedPreferences.getString(buildTimeTriggerKey(), null));
        this.tempTriggerMinTemp = sharedPreferences.getDouble(buildTimeTriggerMinTempKey(), null);
    }

    public boolean isTimeTriggerEnabled() {
        return timeTriggerStartTime != null && timeTriggerEndTime != null;
    }

    public PumpConfig setManualOverride(boolean manualOverride) {
        this.manualOverride = manualOverride;
        setEnabled(manualOverride, "Manual override");
        return this;
    }

    public boolean isEnabled() {
        return digitalOutput.state() == DigitalState.LOW;
    }

    public PumpConfig setEnabled(boolean enabled, String source) {
        if(enabled != isEnabled()) {
            if (enabled)
                digitalOutput.low();
            else
                digitalOutput.high();
        }

        this.enableSource = enabled ? source : null;

        return this;
    }

    public PumpConfig setTimeTrigger(String timeTrigger) {
        this.timeTrigger = timeTrigger;
        if(!StringUtils.isEmpty(timeTrigger)) {
            String[] parts = timeTrigger.split("-");

            if (parts.length != 2)
                throw new IllegalArgumentException("Time range must be in format HH:mm-HH:mm");

            try {
                this.timeTriggerStartTime = LocalTime.parse(parts[0]);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid start time format, must be HH:mm: " + e.getMessage(), e);
            }

            try {
                this.timeTriggerEndTime = LocalTime.parse(parts[1]);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid end time format, must be HH:mm: " + e.getMessage(), e);
            }

            if(timeTriggerEndTime.isBefore(timeTriggerStartTime))
                throw new IllegalArgumentException("End time must be after start time");
        }

        return this;
    }

    public PumpConfig save(JsonSharedPreferences sharedPreferences) {
        sharedPreferences.putBoolean(buildManualOverrideKey(), manualOverride);
        sharedPreferences.putDouble(buildTempTriggerKey(), tempTrigger);
        sharedPreferences.putString(buildTimeTriggerKey(), timeTrigger);
        sharedPreferences.putDouble(buildTimeTriggerMinTempKey(), tempTriggerMinTemp);

        return this;
    }

    private String buildManualOverrideKey() {
        return "pump." + id + ".manual-override";
    }

    private String buildTempTriggerKey() {
        return "pump." + id + ".temp-trigger";
    }

    private String buildTimeTriggerKey() {
        return "pump." + id + ".time-trigger";
    }

    private String buildTimeTriggerMinTempKey() {
        return "pump." + id + ".time-trigger.min-temp";
    }
}
