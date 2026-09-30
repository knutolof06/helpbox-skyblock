package com.knutolof.helpbox.storage.storage;

import com.knutolof.helpbox.storage.util.TextUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SackChatListener {

    private static final Pattern SACK_ITEM_PATTERN = Pattern.compile(
            "\\[Sacks\\]\\s*([+-])\\s*([0-9,\\.]+)\\s+(?!items\\b)(.+?)\\.?", Pattern.CASE_INSENSITIVE);

    private static final Pattern SUPERCRAFT_PATTERN = Pattern.compile(
            "You Supercrafted (?:([0-9,\\.]+)\\s+)?(.+?)!?", Pattern.CASE_INSENSITIVE);

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = TextUtils.stripText(message).trim();
            if (text.contains("[Sacks]")) {
                Matcher matcher = SACK_ITEM_PATTERN.matcher(text);
                while (matcher.find()) {
                    String sign = matcher.group(1);
                    long amount = parseAmount(matcher.group(2));
                    if (sign.equals("-")) amount = -amount;
                    String itemName = matcher.group(3).trim();

                    if (!itemName.isEmpty() && amount != 0) {
                        SackCache.getInstance().updateItemDelta(itemName, amount);
                    }
                }
            } else if (text.contains("You Supercrafted")) {
                Matcher matcher = SUPERCRAFT_PATTERN.matcher(text);
                if (matcher.find()) {
                    String amountStr = matcher.group(1);
                    long amount = (amountStr != null && !amountStr.isBlank()) ? parseAmount(amountStr) : 1;
                    String itemName = matcher.group(2).trim();
                    if (!itemName.isEmpty()) {
                        SackCache.getInstance().updateItemDelta(itemName, amount);
                    }
                }
            }
        });
    }

    private static long parseAmount(String text) {
        try {
            String clean = text.replace(",", "").replace(".", "").trim();
            return Long.parseLong(clean);
        } catch (Exception e) {
            return 1;
        }
    }
}
