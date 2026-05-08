package com.example.subtracker;

public class Service {
    private String id;
    private String name;
    private String icon;
    private Double defaultAmount;
    
    public Service(String id, String name, String icon, Double defaultAmount) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.defaultAmount = defaultAmount;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public Double getDefaultAmount() { return defaultAmount; }
}