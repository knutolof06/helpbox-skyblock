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
            return s.isBlank() ? Optional.empty() : Optional.of(s);
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

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void adoptLastKnownProfile() {
        this.currentProfileId = readPersistedProfile().orElse("default");
        this.confirmed = true;
    }

    public void onProfileIdSeen(String profileId) {
        if (profileId == null || profileId.isBlank()) return;
        this.confirmed = true;
        if (profileId.equals(currentProfileId)) return;

        this.currentProfileId = profileId;
        persistProfile(profileId);

        if (onChange != null) {
            onChange.run();
        }
    }
}