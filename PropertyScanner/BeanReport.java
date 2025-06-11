package com.example;

import java.util.ArrayList;
import java.util.List;

public class BeanReport {
    private String beanId;
    private String className;
    private List<PropertyReport> properties;

    public BeanReport(String beanId, String className) {
        this.beanId = beanId;
        this.className = className;
        this.properties = new ArrayList<>();
    }

    public void addProperty(String propertyName, String placeholderKey, String finalValue) {
        properties.add(new PropertyReport(propertyName, placeholderKey, finalValue));
    }

    public String getBeanId() {
        return beanId;
    }

    public String getClassName() {
        return className;
    }

    public List<PropertyReport> getProperties() {
        return properties;
    }
}
