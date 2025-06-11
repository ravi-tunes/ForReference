package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single Spring bean and all of its property-placeholder usages.
 */
public class BeanReport {
    private final String beanId;
    private final String className;
    private final List<PropertyReport> properties = new ArrayList<>();

    public BeanReport(String beanId, String className) {
        this.beanId = beanId;
        this.className = className;
    }

    /**
     * Add a property-placeholder usage to this bean’s report.
     */
    public void addProperty(PropertyReport prop) {
        properties.add(prop);
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