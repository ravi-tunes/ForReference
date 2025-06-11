package com.example;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds placeholder resolution details for a single bean property.
 */
public class PropertyReport {
    private final String propertyName;
    private final String placeholderKey;
    private final Map<String,String> valuesPerSource; // Keys should use full relative path (e.g., ldn/teamA/app.properties) to avoid column name collisions
    private final String finalValue;
    private final String satisfiedIn;

    /**
     * @param propertyName  the name of the bean property
     * @param placeholderKey the key inside ${...}
     * @param valuesPerSource map of each source-layer name to its value (or null if absent)
     */
    public PropertyReport(String propertyName,
                          String placeholderKey,
                          Map<String,String> valuesPerSource) {
        this.propertyName = propertyName;
        this.placeholderKey = placeholderKey;
        // preserve insertion order of sources
        this.valuesPerSource = Collections.unmodifiableMap(new LinkedHashMap<>(valuesPerSource));

        // Determine satisfiedIn (first non-null) and finalValue (last non-null)
        String first = "MISSING";
        String last = null;
        for (Map.Entry<String,String> e : this.valuesPerSource.entrySet()) {
            String layer = e.getKey();
            String val = e.getValue();
            if (val != null) {
                if ("MISSING".equals(first)) {
                    first = layer;
                }
                last = val;
            }
        }
        this.satisfiedIn = first;
        this.finalValue = last;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPlaceholderKey() {
        return placeholderKey;
    }

    /**
     * @return an unmodifiable map of each source-layer name to the value (or null)
     */
    public Map<String,String> getValuesPerSource() {
        return valuesPerSource;
    }

    public String getFinalValue() {
        return finalValue;
    }

    public String getSatisfiedIn() {
        return satisfiedIn;
    }
}