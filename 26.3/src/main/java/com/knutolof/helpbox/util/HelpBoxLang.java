package com.knutolof.helpbox.util;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class HelpBoxLang {
    private HelpBoxLang() {}

    public static MutableComponent tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static String get(String key, String fallback) {
        return str(key, fallback);
    }

    public static String str(String key, Object... args) {
        if (net.minecraft.locale.Language.getInstance().has(key)) {
            return I18n.get(key, args);
        }
        String s = Component.translatable(key, args).getString();
        if (!s.equals(key)) {
            return s;
        }
        if (args != null && args.length > 0 && args[0] instanceof String fallback) {
            if (args.length > 1) {
                Object[] formatArgs = new Object[args.length - 1];
                System.arraycopy(args, 1, formatArgs, 0, formatArgs.length);
                try {
                    return String.format(fallback, formatArgs);
                } catch (Exception ignored) {
                    return fallback;
                }
            }
            return fallback;
        }
        return s;
    }
}
