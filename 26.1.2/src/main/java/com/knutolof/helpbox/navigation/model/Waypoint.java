package com.knutolof.helpbox.navigation.model;

import java.util.UUID;

public class Waypoint {
    private String id;
    private String name;
    private int x;
    private int y;
    private int z;
    private String colorHex;
    private String iconName;
    
    private boolean isMapLocation = false;
    private String description = "";
    private boolean isEnabled = true;
    
    public Waypoint(String name, int x, int y, int z, String colorHex, String iconName) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.colorHex = colorHex;
        this.iconName = iconName;
    }
    
    public Waypoint(String name, int x, int y, int z, String colorHex, String iconName, boolean isMapLocation) {
        this(name, x, y, z, colorHex, iconName);
        this.isMapLocation = isMapLocation;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    
    public int getZ() { return z; }
    public void setZ(int z) { this.z = z; }
    
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    
    public boolean isMapLocation() { return isMapLocation; }
    public void setMapLocation(boolean mapLocation) { isMapLocation = mapLocation; }
    
    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
}
