package com.stunslamassist;

import com.stunslamassist.config.Config;
import com.stunslamassist.config.ConfigScreen;
import com.stunslamassist.event.AttackHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stun Slam Assist — client-side Fabric mod.
 *
 * Auto-triggers the axe → mace stun-slam follow-up when you hit a shielded
 * target while airborne. Behaviour is governed by {@link Config}; you can
 * configure everything via the in-game settings screen.
 *
 *   B — toggle the mod on / off
 *   O — open the config screen
 *
 * Both keybinds are rebindable under Options → Controls → Stun Slam Assist.
 */
public class StunSlamAssistClient implements ClientModInitializer {

    public static final String MOD_ID = "stunslamassist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Config config;
    private static SlamExecutor executor;

    private static KeyBinding toggleKey;
    private static KeyBinding configKey;

    private static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.register(Identifier.of(MOD_ID, "main"));

    @Override
    public void onInitializeClient() {
        LOGGER.info("[{}] Initializing", MOD_ID);

        config = Config.load();
        executor = new SlamExecutor();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.stunslamassist.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
        ));

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.stunslamassist.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
        ));

        AttackHandler.register(executor);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        LOGGER.info("[{}] Ready (enabled={}, chance={}%)",
            MOD_ID, config.enabled, config.chancePercent);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        // Toggle on/off
        while (toggleKey.wasPressed()) {
            config.enabled = !config.enabled;
            config.save();
            if (config.showActionBarMessages) {
                client.player.sendMessage(
                    Text.literal("[StunSlam] " + (config.enabled ? "§aON" : "§cOFF")),
                    true
                );
            }
        }

        // Open config screen
        while (configKey.wasPressed()) {
            // Must defer setScreen if we're inside a tick — but END_CLIENT_TICK
            // is safe to set the screen from.
            client.setScreen(new ConfigScreen(client.currentScreen));
        }

        // Always advance the state machine, even when toggled off, so an
        // in-flight sequence finishes cleanly and restores your hotbar.
        executor.tick(client);
    }

    public static Config getConfig() {
        return config;
    }

    public static SlamExecutor getExecutor() {
        return executor;
    }
}
