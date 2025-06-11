package com.example;

public class PropertyReport {
    private String propertyName;
    private String placeholderKey;
    private String finalValue;
    private String satisfiedIn;

    public PropertyReport(String propertyName, String placeholderKey, String finalValue) {
        this.propertyName = propertyName;
        this.placeholderKey = placeholderKey;
        this.finalValue = finalValue;
        this.satisfiedIn = determineSatisfiedIn(placeholderKey, finalValue);
    }

    private String determineSatisfiedIn(String placeholderKey, String finalValue) {
        // Implement logic to determine the layer where the placeholder was satisfied
        return "layer"; // Placeholder, replace with actual logic
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPlaceholderKey() {
        return placeholderKey;
    }

    public String getFinalValue() {
        return finalValue;
    }

    public String getSatisfiedIn() {
        return satisfiedIn;
    }
}
