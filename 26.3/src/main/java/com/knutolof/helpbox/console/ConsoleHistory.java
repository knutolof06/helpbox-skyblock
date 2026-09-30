package com.knutolof.helpbox.console;

import com.knutolof.helpbox.storage.util.TextUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConsoleHistory {

    private static final int MAX_ENTRIES = 1000;
    private static final List<ConsoleEntry> entries = Collections.synchronizedList(new ArrayList<>());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            addEntry(message);
        });
    }

    public static void addEntry(Component component) {
        if (component == null) return;
        String plain = TextUtils.stripText(component).trim();
        if (plain.isEmpty()) return;

        synchronized (entries) {
            // Deduplicate: If last entry was logged < 200ms ago with identical plain text, skip
            if (!entries.isEmpty()) {
                ConsoleEntry last = entries.get(entries.size() - 1);
                if (System.currentTimeMillis() - last.timestamp() < 200 && last.plainText().equals(plain)) {
                    return;
                }
            }

            String time = LocalTime.now().format(TIME_FORMATTER);
            ConsoleEntry entry = new ConsoleEntry(System.currentTimeMillis(), time, component, plain);

            entries.add(entry);
            if (entries.size() > MAX_ENTRIES) {
                entries.remove(0);
            }
        }
    }

    public static List<ConsoleEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public static void clear() {
        entries.clear();
    }

    public record ConsoleEntry(long timestamp, String timeStr, Component component, String plainText) {}
}
