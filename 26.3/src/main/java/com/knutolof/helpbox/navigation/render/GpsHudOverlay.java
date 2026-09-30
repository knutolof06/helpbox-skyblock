package com.knutolof.helpbox.navigation.render;

import com.knutolof.helpbox.navigation.WaypointManager;
import com.knutolof.helpbox.navigation.model.Waypoint;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.List;

public class GpsHudOverlay implements HudElement {
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        // 1. Draw GPS arrow for active waypoint
        Waypoint active = WaypointManager.getActiveWaypoint();
        if (active != null) {
            Vec3 playerPos = client.player.position();
            double dx = (active.getX() + 0.5) - playerPos.x;
            double dz = (active.getZ() + 0.5) - playerPos.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            float yaw = client.player.getYRot();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float angleDiff = targetYaw - yaw;
            
            int color = 0xFFFFFFFF;
            try {
                color = Color.decode(active.getColorHex()).getRGB() | 0xFF000000;
            } catch (Exception ignored) {}
            
            int screenWidth = client.getWindow().getGuiScaledWidth();
            int y = 40;
            int x = screenWidth / 2;
            
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            
            graphics.pose().pushMatrix();
            graphics.pose().rotate((float) Math.toRadians(angleDiff));
            graphics.centeredText(client.font, "▲", 0, -4, color);
            graphics.pose().popMatrix();
            
            graphics.centeredText(client.font, (int)dist + "m", 0, 10, color);
            
            graphics.pose().popMatrix();
        }
        
        // 2. Draw waypoint labels projected from world-space
        List<WaypointWorldRenderer.WaypointLabel> labels = WaypointWorldRenderer.getPendingLabels();
        if (labels == null || labels.isEmpty()) return;
        
        net.minecraft.client.gui.Font font = client.font;
        
        for (WaypointWorldRenderer.WaypointLabel label : labels) {
            int sx = label.screenX;
            int sy = label.screenY;
            float scale = label.scale;
            
            // Don't draw labels that are off-screen
            int screenW = client.getWindow().getGuiScaledWidth();
            int screenH = client.getWindow().getGuiScaledHeight();
            if (sx < -100 || sx > screenW + 100 || sy < -50 || sy > screenH + 50) continue;
            
            int textWidth = font.width(label.text);
            int halfWidth = textWidth / 2;
            
            if (scale != 1.0f) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(sx, sy);
                graphics.pose().scale(scale, scale);
                
                // Background box for readability
                graphics.fill(
                    -halfWidth - 2, -2,
                    halfWidth + 2, font.lineHeight + 1,
                    0x80000000
                );
                
                // Text
                graphics.text(font, label.text, -halfWidth, 0, label.color, true);
                
                graphics.pose().popMatrix();
            } else {
                // Background box
                graphics.fill(
                    sx - halfWidth - 2, sy - 2,
                    sx + halfWidth + 2, sy + font.lineHeight + 1,
                    0x80000000
                );
                
                // Text
                graphics.text(font, label.text, sx - halfWidth, sy, label.color, true);
            }
        }
    }
}
