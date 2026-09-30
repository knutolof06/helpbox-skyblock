package com.knutolof.helpbox.navigation.render;
import com.mojang.blaze3d.vertex.VertexConsumer;
public class TestRender {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : VertexConsumer.class.getDeclaredMethods()) {
            System.out.println("Method: " + m.getName() + " -> " + m.getReturnType().getName());
        }
    }
}
