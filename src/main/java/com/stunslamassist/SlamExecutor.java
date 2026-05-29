package com.stunslamassist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/**
 * State machine that performs the mace follow-up after a qualifying axe hit.
 *
 * Lifecycle:
 *   IDLE → schedule() → WAITING(delay) → tick countdown → SWAPPED → restore on
 *   next tick → IDLE
 *
 * Designed so the actual mace hit lands on the configured tick boundary
 * (1 tick = 50 ms), which is exactly the stun slam window.
 *
 * Thread model: only touched on the client thread.
 */
public class SlamExecutor {

    /**
     * Internal stages, deliberately separated so the slot restore happens on
     * a different tick from the mace hit — keeps the server happy and avoids
     * "ghost swing" desyncs.
     */
    private enum Stage { IDLE, WAITING_FOR_MACE_HIT, SWAPPED_RESTORE_NEXT }

    private static final double MAX_REACH = 5.0;

    private Stage stage = Stage.IDLE;
    private int ticksRemaining = 0;
    private int maceSlot = -1;
    private int returnSlot = -1;
    private Entity target = null;

    /** True when a slam sequence is in flight; further attacks won't re-schedule. */
    public boolean isBusy() {
        return stage != Stage.IDLE;
    }

    /**
     * Schedule a mace follow-up. Called by {@code AttackHandler} after all
     * gating checks (airborne, axe in hand, shield up, chance roll, etc.) pass.
     *
     * @param delayTicks  ticks to wait before the mace hit; 1 = 50 ms
     * @param target      the entity to follow up on
     * @param maceSlot    hotbar slot index of the mace (0–8)
     * @param returnSlot  hotbar slot index to restore afterwards (usually the axe)
     */
    public void schedule(int delayTicks, Entity target, int maceSlot, int returnSlot) {
        if (stage != Stage.IDLE) return;
        if (target == null) return;
        if (maceSlot < 0 || maceSlot > 8) return;
        if (returnSlot < 0 || returnSlot > 8) return;

        this.ticksRemaining = Math.max(1, delayTicks);
        this.target = target;
        this.maceSlot = maceSlot;
        this.returnSlot = returnSlot;
        this.stage = Stage.WAITING_FOR_MACE_HIT;
    }

    /** Advance one client tick. */
    public void tick(MinecraftClient client) {
        if (stage == Stage.IDLE) return;

        if (client.player == null
            || client.interactionManager == null
            || client.getNetworkHandler() == null) {
            reset();
            return;
        }

        // Abort if the target became invalid mid-sequence.
        if (target == null || target.isRemoved() || !target.isAlive()) {
            restoreSlotAndReset(client);
            return;
        }
        if (client.player.distanceTo(target) > MAX_REACH) {
            restoreSlotAndReset(client);
            return;
        }

        switch (stage) {
            case WAITING_FOR_MACE_HIT -> {
                ticksRemaining--;
                if (ticksRemaining > 0) return;

                // Swap to mace, swing, hit. The attack packet is sent by
                // ClientPlayerInteractionManager#attackEntity.
                selectSlot(client, maceSlot);
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);

                stage = Stage.SWAPPED_RESTORE_NEXT;
            }
            case SWAPPED_RESTORE_NEXT -> {
                restoreSlotAndReset(client);

                if (StunSlamAssistClient.getConfig().showActionBarMessages) {
                    client.player.sendMessage(
                        Text.literal("§a[StunSlam] slammed"),
                        true
                    );
                }
            }
            case IDLE -> { /* unreachable — guarded above */ }
        }
    }

    private void restoreSlotAndReset(MinecraftClient client) {
        if (returnSlot >= 0 && returnSlot <= 8) {
            selectSlot(client, returnSlot);
        }
        reset();
    }

    /** Set the hotbar slot AND notify the server so it knows what we're holding. */
    private static void selectSlot(MinecraftClient client, int slot) {
        if (client.player == null || client.getNetworkHandler() == null) return;
        client.player.getInventory().setSelectedSlot(slot);
        client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void reset() {
        stage = Stage.IDLE;
        ticksRemaining = 0;
        maceSlot = -1;
        returnSlot = -1;
        target = null;
    }
}
