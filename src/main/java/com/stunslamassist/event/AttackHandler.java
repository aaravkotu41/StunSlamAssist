package com.stunslamassist.event;

import com.stunslamassist.SlamExecutor;
import com.stunslamassist.StunSlamAssistClient;
import com.stunslamassist.config.Config;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Random;

/**
 * Listens to player attacks. When the attack satisfies all stun-slam
 * conditions, schedules the mace follow-up on the {@link SlamExecutor}.
 *
 * Conditions (all required):
 *   • Mod enabled
 *   • Executor not already busy
 *   • Player on the client (we drive client-side input)
 *   • Main-hand item is an axe
 *   • A mace exists somewhere in hotbar slots 0–8
 *   • Player is airborne, optionally with falling state + fall distance
 *   • Target is a LivingEntity (player or mob)
 *   • If configured, target is actively using a shield
 *   • Random roll (0..99) < configured chance %
 */
public class AttackHandler {

    private static final Random RNG = new Random();

    public static void register(SlamExecutor executor) {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Only act client-side; server-side firing would double-handle.
            if (!world.isClient()) return ActionResult.PASS;
            // Only handle main-hand attacks.
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            MinecraftClient client = MinecraftClient.getInstance();
            Config config = StunSlamAssistClient.getConfig();

            if (!config.enabled) return ActionResult.PASS;
            if (StunSlamAssistClient.getExecutor().isBusy()) return ActionResult.PASS;
            if (client.player == null || player != client.player) return ActionResult.PASS;

            // Must be holding an axe in main hand.
            if (!(player.getMainHandStack().getItem() instanceof AxeItem)) {
                return ActionResult.PASS;
            }

            // Must have a mace in hotbar.
            PlayerInventory inv = client.player.getInventory();
            int maceSlot = findMaceSlot(inv);
            if (maceSlot < 0) return ActionResult.PASS;

            // Must be airborne (with optional falling + distance gating).
            if (player.isOnGround()) return ActionResult.PASS;
            if (config.requireFalling && player.getVelocity().y >= 0) return ActionResult.PASS;
            if (player.fallDistance < config.minFallDistance) return ActionResult.PASS;

            // Target must be a LivingEntity; optionally must be shielding.
            if (!(entity instanceof LivingEntity living)) return ActionResult.PASS;
            if (config.requireShieldActive && !isShielding(living)) return ActionResult.PASS;

            // Random chance — this is the human-feel knob.
            if (RNG.nextInt(100) >= config.chancePercent) {
                if (config.showActionBarMessages && client.player != null) {
                    client.player.sendMessage(
                        Text.literal("§7[StunSlam] passed on slam"), true);
                }
                return ActionResult.PASS;
            }

            // Schedule the mace follow-up with a random tick delay.
            int min = config.minDelayTicks;
            int max = Math.max(min, config.maxDelayTicks);
            int delay = min + RNG.nextInt(max - min + 1);
            int returnSlot = inv.getSelectedSlot(); // come back to the axe afterwards

            StunSlamAssistClient.getExecutor().schedule(delay, entity, maceSlot, returnSlot);

            // Don't intercept — let the axe hit resolve normally.
            return ActionResult.PASS;
        });
    }

    private static int findMaceSlot(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() == Items.MACE) return i;
        }
        return -1;
    }

    /**
     * Is this entity actively raising a shield?
     * For players that means using the SHIELD item; mobs effectively never
     * shield in vanilla, so this is mostly a PlayerEntity check.
     */
    private static boolean isShielding(LivingEntity e) {
        if (e instanceof PlayerEntity p) {
            return p.isUsingItem()
                && p.getActiveItem().getItem() == Items.SHIELD;
        }
        // LivingEntity#isBlocking covers mobs that can block (none in vanilla 1.21.1
        // besides players, but futureproof).
        return e.isBlocking();
    }
}
