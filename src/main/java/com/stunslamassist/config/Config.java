package com.stunslamassist.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persisted mod config. Lives in .minecraft/config/stunslamassist.json.
 *
 * All fields are public so GSON can serialize them directly. Tweak in-game
 * via the config screen, or by editing the JSON when the game is closed.
 */
public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("stunslamassist/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
        FabricLoader.getInstance().getConfigDir().resolve("stunslamassist.json");

    /** Master on/off. Toggle in-game with the toggle keybind. */
    public boolean enabled = true;

    /** % chance the auto-slam fires when all conditions are met (0–100). */
    public int chancePercent = 90;

    /** Tick delay between the axe hit and the mace hit. Random in [min, max]. */
    public int minDelayTicks = 1;
    public int maxDelayTicks = 2;

    /** Only auto-slam when actually falling (not just jumping up). */
    public boolean requireFalling = true;

    /** Minimum fall distance before auto-slam triggers (smash damage scales with this). */
    public double minFallDistance = 1.0;

    /** Only auto-slam when the target is actively raising a shield. */
    public boolean requireShieldActive = true;

    /** Print action-bar feedback when the mod triggers / refuses to trigger. */
    public boolean showActionBarMessages = true;

    public static Config load() {
        if (!Files.exists(PATH)) {
            Config fresh = new Config();
            fresh.save();
            return fresh;
        }
        try (Reader r = Files.newBufferedReader(PATH)) {
            Config loaded = GSON.fromJson(r, Config.class);
            return loaded != null ? loaded.sanitize() : new Config();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to read config, using defaults", e);
            return new Config();
        }
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write config", e);
        }
    }

    /** Clamp all fields to sane ranges. Returns this for chaining. */
    public Config sanitize() {
        if (chancePercent < 0) chancePercent = 0;
        if (chancePercent > 100) chancePercent = 100;
        if (minDelayTicks < 1) minDelayTicks = 1;
        if (maxDelayTicks < minDelayTicks) maxDelayTicks = minDelayTicks;
        if (maxDelayTicks > 10) maxDelayTicks = 10;
        if (minFallDistance < 0) minFallDistance = 0;
        if (minFallDistance > 20) minFallDistance = 20;
        return this;
    }
}
