package com.knutolof.helpbox.navigation.render;

import com.knutolof.helpbox.navigation.WaypointManager;
import com.knutolof.helpbox.navigation.model.Waypoint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class WaypointWorldRenderer {
    private static boolean debugLogged = false;
    private static volatile List<WaypointLabel> pendingLabels = Collections.emptyList();

    public static List<WaypointLabel> getPendingLabels() {
        return pendingLabels;
    }

    public static void render(LevelRenderContext context) {
        Vec3 playerPos;
        double distSq;
        Waypoint active;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (!debugLogged) {
            System.out.println("[HelpBox] WaypointWorldRenderer event fired successfully.");
            debugLogged = true;
        }
        if ((active = WaypointManager.getActiveWaypoint()) != null && (distSq = (playerPos = client.player.position()).distanceToSqr((double)active.getX() + 0.5, (double)active.getY() + 0.5, (double)active.getZ() + 0.5)) < 9.0 && WaypointManager.isAutoClearEnabled()) {
            WaypointManager.clearActiveWaypoint();
            client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            active = null;
        }
        PoseStack poseStack = context.poseStack();
        Camera camera = client.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 p0 = new Vec3(Mth.lerp((double)partialTick, (double)client.player.xo, (double)client.player.getX()), Mth.lerp((double)partialTick, (double)client.player.yo, (double)client.player.getY()) + 0.5, Mth.lerp((double)partialTick, (double)client.player.zo, (double)client.player.getZ()));

        Waypoint finalActive = active;
        Vec3 finalP0 = p0;
        context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vertexConsumer) -> {
            if (finalActive != null) {
                boolean drawAStar;
                float opacity = WaypointManager.getEspLineOpacity();
                Color color = Color.decode(finalActive.getColorHex());
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                int a = (int)(opacity * 255.0f);
                Vec3 targetVec = new Vec3((double)finalActive.getX() + 0.5, (double)finalActive.getY() + 0.5, (double)finalActive.getZ() + 0.5);
                List<BlockPos> path = WaypointManager.getPathfinder().getCurrentPath();
                drawAStar = path != null && path.size() >= 2;
                
                Vec3 eyeStart;
                {
                    Vec3 lookAngle = client.player.getLookAngle();
                    Vec3 eyePos = new Vec3(
                        client.player.getX(),
                        client.player.getY() + client.player.getEyeHeight(),
                        client.player.getZ()
                    );
                    eyeStart = eyePos.add(lookAngle.scale(2.0));
                }

                if (drawAStar) {
                    int startIndex = 0;
                    double closestDist = Double.MAX_VALUE;
                    for (int i2 = 0; i2 < path.size(); i2++) {
                        BlockPos pos = path.get(i2);
                        double d = finalP0.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                        if (d < closestDist) {
                            closestDist = d;
                            startIndex = i2;
                        }
                    }

                    if (startIndex < path.size() - 1) {
                        BlockPos pA = path.get(startIndex);
                        BlockPos pB = path.get(startIndex + 1);
                        Vec3 dir = new Vec3(pB.getX() - pA.getX(), pB.getY() - pA.getY(), pB.getZ() - pA.getZ()).normalize();
                        Vec3 toPlayer = new Vec3(finalP0.x - (pA.getX() + 0.5), finalP0.y - (pA.getY() + 1.0), finalP0.z - (pA.getZ() + 0.5));
                        if (dir.dot(toPlayer) > 0) {
                            startIndex++;
                        }
                    }

                    List<Vec3> points = new ArrayList<>();
                    points.add(eyeStart);
                    for (int i2 = startIndex; i2 < path.size(); i2++) {
                        BlockPos pos = path.get(i2);
                        double surfaceY = pos.getY() + 1.0;
                        points.add(new Vec3(pos.getX() + 0.5, surfaceY + 0.3, pos.getZ() + 0.5));
                    }

                    if (points.size() >= 2) {
                        double bezierPoint = 1.0;
                        for (int i2 = 0; i2 < points.size() - 1; i2++) {
                            Vec3 segA = points.get(i2);
                            Vec3 segB = points.get(i2 + 1);
                            Vec3 reduce = segB.subtract(segA).normalize().scale(bezierPoint);
                            Vec3 segStart = (i2 != 0) ? segA.add(reduce) : segA;
                            Vec3 segEnd   = (i2 != points.size() - 2) ? segB.subtract(reduce) : segB;
                            WaypointWorldRenderer.drawLine(vertexConsumer, pose, segStart, segEnd, r, g, b, a);
                        }

                        for (int i2 = 1; i2 < points.size() - 1; i2++) {
                            Vec3 prev = points.get(i2 - 1);
                            Vec3 curr = points.get(i2);
                            Vec3 next = points.get(i2 + 1);
                            Vec3 bp1 = curr.subtract(curr.subtract(prev).normalize().scale(bezierPoint));
                            Vec3 bp3 = curr.subtract(curr.subtract(next).normalize().scale(bezierPoint));
                            Vec3 bp2 = curr;
                            int bSeg = 30;
                            for (int s = 1; s <= bSeg; s++) {
                                float t1 = (float)(s - 1) / bSeg;
                                float t2 = (float) s       / bSeg;
                                Vec3 pt1 = calcBezier(t1, bp1, bp2, bp3);
                                Vec3 pt2 = calcBezier(t2, bp1, bp2, bp3);
                                WaypointWorldRenderer.drawLine(vertexConsumer, pose, pt1, pt2, r, g, b, a);
                            }
                        }
                    }
                } else {
                    Vec3 dist3 = targetVec.subtract(finalP0);
                    double dist = dist3.length();
                    double arcHeight = Math.min(dist * 0.1, 20.0);
                    Vec3 midControl = finalP0.add(targetVec).scale(0.5).add(0.0, arcHeight, 0.0);
                    int segments = 100;
                    Vec3 previousPoint = finalP0;
                    for (int i2 = 1; i2 <= segments; ++i2) {
                        float t = (float) i2 / segments;
                        Vec3 currentPoint = calcBezier(t, finalP0, midControl, targetVec);
                        WaypointWorldRenderer.drawLine(vertexConsumer, pose, previousPoint, currentPoint, r, g, b, a);
                        previousPoint = currentPoint;
                    }
                }

                double minX = (double)finalActive.getX() - 0.01;
                double minY = (double)finalActive.getY() - 0.01;
                double minZ = (double)finalActive.getZ() - 0.01;
                double maxX = (double)finalActive.getX() + 1.01;
                double maxY = (double)finalActive.getY() + 1.01;
                double maxZ = (double)finalActive.getZ() + 1.01;
                int boxA = 100;
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(maxX, maxY, maxZ), new Vec3(maxX, minY, maxZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(minX, minY, maxZ), new Vec3(minX, maxY, maxZ), r, g, b, boxA);
                
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(maxX, maxY, minZ), new Vec3(maxX, minY, minZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(maxX, minY, minZ), new Vec3(minX, minY, minZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), r, g, b, boxA);
                
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ), r, g, b, boxA);
                WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), r, g, b, boxA);
            }

            for (Waypoint wp : WaypointManager.getWaypoints()) {
                double distToWaypoint;
                if (!wp.isEnabled() || (distToWaypoint = finalP0.distanceTo(new Vec3((double)wp.getX() + 0.5, (double)wp.getY(), (double)wp.getZ() + 0.5))) > 150.0) continue;
                double thickness = 0.15;
                double cx = (double)wp.getX() + 0.5;
                double cz = (double)wp.getZ() + 0.5;
                double bx = cx - thickness;
                double bz = cz - thickness;
                double bx2 = cx + thickness;
                double bz2 = cz + thickness;
                double beamBottom = wp.getY();
                double beamTop = wp.getY() + 80;
                try {
                    int col = Color.decode(wp.getColorHex()).getRGB();
                    int rB = col >> 16 & 0xFF;
                    int gB = col >> 8 & 0xFF;
                    int bB = col & 0xFF;
                    int aB = 80;
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx, beamTop, bz2), new Vec3(bx2, beamTop, bz2), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx2, beamTop, bz2), new Vec3(bx2, beamBottom, bz2), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx2, beamBottom, bz2), new Vec3(bx, beamBottom, bz2), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx, beamBottom, bz2), new Vec3(bx, beamTop, bz2), rB, gB, bB, aB);

                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx2, beamTop, bz), new Vec3(bx, beamTop, bz), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx, beamTop, bz), new Vec3(bx, beamBottom, bz), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx, beamBottom, bz), new Vec3(bx2, beamBottom, bz), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx2, beamBottom, bz), new Vec3(bx2, beamTop, bz), rB, gB, bB, aB);

                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx, beamTop, bz), new Vec3(bx, beamTop, bz2), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx, beamBottom, bz), new Vec3(bx, beamBottom, bz2), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx2, beamTop, bz), new Vec3(bx2, beamTop, bz2), rB, gB, bB, aB);
                    WaypointWorldRenderer.drawLine(vertexConsumer, pose, new Vec3(bx2, beamBottom, bz), new Vec3(bx2, beamBottom, bz2), rB, gB, bB, aB);
                }
                catch (Exception ignored) {}
            }
        });

        Matrix4f viewMatrix = new Matrix4f();
        camera.getViewRotationMatrix(viewMatrix);
        viewMatrix.translate((float)(-cameraPos.x), (float)(-cameraPos.y), (float)(-cameraPos.z));
        float fov = (float)Math.toRadians(((Integer)client.options.fov().get()).intValue());
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float aspect = (float)client.getWindow().getWidth() / (float)client.getWindow().getHeight();
        float near = 0.05f;
        float far = (float)client.options.getEffectiveRenderDistance() * 16.0f;
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective(fov, aspect, near, far);
        Matrix4f mvp = new Matrix4f((Matrix4fc)projMatrix).mul((Matrix4fc)viewMatrix);
        ArrayList<WaypointLabel> labels = new ArrayList<WaypointLabel>();
        for (Waypoint wp : WaypointManager.getWaypoints()) {
            if (!wp.isEnabled()) continue;
            double wpX = (double)wp.getX() + 0.5;
            double wpY = (double)wp.getY() + 2.5;
            double wpZ = (double)wp.getZ() + 0.5;
            double distToWaypoint = p0.distanceTo(new Vec3(wpX, wpY, wpZ));
            String distStr = String.format(" [%.0fm]", distToWaypoint);
            String icon = wp.getIconName() != null && !wp.getIconName().isEmpty() ? wp.getIconName() + " " : "";
            String text = icon + wp.getName() + distStr;
            int textColor = -1;
            try {
                textColor = Color.decode(wp.getColorHex()).getRGB() | 0xFF000000;
            }
            catch (Exception ignored) {}
            Vector4f pos4 = new Vector4f((float)wpX, (float)wpY, (float)wpZ, 1.0f);
            mvp.transform(pos4);
            if (pos4.w <= 0.0f) continue;
            float ndcX = pos4.x / pos4.w;
            float ndcY = pos4.y / pos4.w;
            float sx = (ndcX * 0.5f + 0.5f) * (float)screenW;
            float sy = (1.0f - (ndcY * 0.5f + 0.5f)) * (float)screenH;
            float scale = 1.0f;
            if (distToWaypoint > 30.0) {
                scale = (float)Math.max((double)0.6f, 30.0 / distToWaypoint);
            }
            labels.add(new WaypointLabel(text, (int)sx, (int)sy, textColor, scale));
        }
        pendingLabels = labels;
        poseStack.popPose();
    }

    private static void drawLine(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p1, Vec3 p2, int r, int g, int b, int a) {
        Vec3 dir = p2.subtract(p1);
        if (dir.lengthSqr() < 1.0E-5) {
            return;
        }
        dir = dir.normalize();
        float nx = (float)dir.x;
        float ny = (float)dir.y;
        float nz = (float)dir.z;
        consumer.addVertex(pose, (float)p1.x, (float)p1.y, (float)p1.z).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(8.0f);
        consumer.addVertex(pose, (float)p2.x, (float)p2.y, (float)p2.z).setColor(r, g, b, a).setNormal(pose, nx, ny, nz).setLineWidth(8.0f);
    }

    private static Vec3 calcBezier(float t, Vec3 p1, Vec3 p2, Vec3 p3) {
        float u = 1.0f - t;
        double x = u * u * p1.x + 2.0 * u * t * p2.x + t * t * p3.x;
        double y = u * u * p1.y + 2.0 * u * t * p2.y + t * t * p3.y;
        double z = u * u * p1.z + 2.0 * u * t * p2.z + t * t * p3.z;
        return new Vec3(x, y, z);
    }

    public static class WaypointLabel {
        public final String text;
        public final int screenX;
        public final int screenY;
        public final int color;
        public final float scale;

        public WaypointLabel(String text, int screenX, int screenY, int color, float scale) {
            this.text = text;
            this.screenX = screenX;
            this.screenY = screenY;
            this.color = color;
            this.scale = scale;
        }
    }
}
