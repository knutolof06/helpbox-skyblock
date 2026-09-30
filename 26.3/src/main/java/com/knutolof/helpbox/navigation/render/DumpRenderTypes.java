package com.knutolof.helpbox.navigation.render;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import java.io.FileWriter;
import java.io.PrintWriter;
public class DumpRenderTypes {
    public static void main(String[] args) throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("C:\\Users\\burha\\Desktop\\RenderTypesDump.txt"))) {
            out.println("--- RenderType ---");
            for (java.lang.reflect.Method m : RenderType.class.getDeclaredMethods()) {
                out.println(m.getName());
            }
            out.println("--- RenderTypes ---");
            for (java.lang.reflect.Method m : RenderTypes.class.getDeclaredMethods()) {
                out.println(m.getName());
            }
        }
    }
}
