package com.knutolof.helpbox.navigation;

import net.minecraft.core.BlockPos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoordinateParser {

    /**
     * Parses a string into a BlockPos.
     * Extracts all numbers (including negatives and decimals) from the string.
     * If exactly three numbers are found, or at least three numbers are found in a row,
     * it assumes they are X, Y, and Z.
     * Decimals are rounded or truncated.
     */
    public static BlockPos parse(String input) throws IllegalArgumentException {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Coordinate string is empty.");
        }
        
        // Remove common command prefixes
        String cleanInput = input.trim();
        if (cleanInput.startsWith("/")) {
            cleanInput = cleanInput.replaceAll("^/tp\\s+(@[psea]\\s+)?", "");
        }
        
        // Remove 'x:', 'X:', etc. to just leave the numbers
        cleanInput = cleanInput.replaceAll("(?i)[xyz]:", " ");
        
        // Match numbers, allowing for negative signs and decimals
        Pattern numberPattern = Pattern.compile("-?\\d+(?:\\.\\d+)?");
        Matcher matcher = numberPattern.matcher(cleanInput);
        
        double[] coords = new double[3];
        int count = 0;
        
        while (matcher.find() && count < 3) {
            try {
                coords[count] = Double.parseDouble(matcher.group());
                count++;
            } catch (NumberFormatException ignored) {}
        }
        
        if (count < 3) {
            throw new IllegalArgumentException("Could not find X, Y, and Z coordinates in the string.");
        }
        
        return new BlockPos((int) Math.round(coords[0]), (int) Math.round(coords[1]), (int) Math.round(coords[2]));
    }
}
