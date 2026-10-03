package com.knutolof.helpbox.storage.storage;
import com.knutolof.helpbox.storage.StorageInitializer;


import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Owns the "which SkyBlock profile are we on" state.
 * Optimistically defaults to last known profile or "default", allowing saves immediately.
 */
public final class StorageProfile {

    private static final StorageProfile INSTANCE = new StorageProfile();
    private volatile String currentProfileId = "default";
    private volatile boolean confirmed = true;
    private Runnable beforeChange;
    private Runnable onChange;

    private StorageProfile() {
        adoptLastKnownProfile();
    }

    public static StorageProfile getInstance() {
        return INSTANCE;
    }

    private static Path profilePointerFile() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(StorageInitializer.MOD_ID)
                .resolve("last_profile.txt");
    }

    private static Optional<String> readPersistedProfile() {
        Path file = profilePointerFile();
        if (!Files.exists(file)) return Optional.empty();
        try {
            String s = Files.readString(file).strip();
            return s.isBlank() ? Optional.empty() : Optional.of(sanitizeProfileName(s));
        } catch (IOException e) {
            StorageInitializer.LOGGER.error("Failed to read last profile pointer", e);
            return Optional.empty();
        }
    }

    private static void persistProfile(String profileId) {
        try {
            Path file = profilePointerFile();
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, profileId);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            StorageInitializer.LOGGER.error("Failed to persist last profile pointer", e);
        }
    }

    public static String sanitizeProfileName(String name) {
        if (name == null || name.isBlank()) return "default";
        String clean = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return clean.isBlank() ? "default" : clean;
    }

    public Optional<String> current() {
        if (currentProfileId == null || currentProfileId.isBlank()) {
            currentProfileId = readPersistedProfile().orElse("default");
        }
        return Optional.of(currentProfileId);
    }

    public boolean isConfirmed() {
        return true; // Always allow saving so chest cache & config is never lost
    }

    public void markConfirmed() {
        this.confirmed = true;
    }

    public void setBeforeChange(Runnable beforeChange) {
        this.beforeChange = beforeChange;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void adoptLastKnownProfile() {
        this.currentProfileId = readPersistedProfile().orElse("default");
        this.confirmed = true;
    }

    public java.util.List<String> getKnownProfiles() {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        current().ifPresent(set::add);
        set.add("default");
        Path profilesDir = FabricLoader.getInstance().getConfigDir()
                .resolve(StorageInitializer.MOD_ID)
                .resolve("profiles");
        if (Files.exists(profilesDir)) {
            try (var stream = Files.list(profilesDir)) {
                stream.filter(Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .filter(s -> !s.isBlank())
                        .forEach(set::add);
            } catch (IOException ignored) {}
        }
        return new java.util.ArrayList<>(set);
    }

    public void onProfileIdSeen(String profileId) {
        if (profileId == null || profileId.isBlank()) return;
        String sanitized = sanitizeProfileName(profileId);
        this.confirmed = true;
        if (sanitized.equalsIgnoreCase(currentProfileId)) return;

        if (beforeChange != null) {
            beforeChange.run();
        }

        this.currentProfileId = sanitized;
        persistProfile(sanitized);

        if (onChange != null) {
            onChange.run();
        }
    }
}