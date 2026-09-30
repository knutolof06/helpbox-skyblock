package com.knutolof.helpbox.navigation.model;

import java.util.ArrayList;
import java.util.List;

public class NavigationConfig {
    public boolean autoClearWaypoint = true;
    public float espLineOpacity = 0.85f;
    // transient prevents GSON from saving this to the config file
    public transient String activeWaypointId = null;
    
    public List<Waypoint> waypoints = new ArrayList<>();
}
