package com.stunslamassist.config;

import com.stunslamassist.StunSlamAssistClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/**
 * Vanilla-widget settings screen. Opens with the config keybind (default: O,
 * configurable in Controls). Saves on close.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;
    private final Config config;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Stun Slam Assist — Settings"));
        this.parent = parent;
        this.config = StunSlamAssistClient.getConfig();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int w = 220;
        int h = 20;
        int gap = 4;
        int row = 0;
        int top = 40;

        // Enabled toggle
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.enabled)
            .build(centerX - w / 2, top + (row++) * (h + gap), w, h,
                Text.literal("Mod Enabled"),
                (btn, val) -> config.enabled = val));

        // Chance slider 0-100
        addDrawableChild(new PercentSlider(
            centerX - w / 2, top + (row++) * (h + gap), w, h,
            "Trigger Chance", config.chancePercent,
            v -> config.chancePercent = v));

        // Min delay ticks 1-10
        addDrawableChild(new IntSlider(
            centerX - w / 2, top + (row++) * (h + gap), w, h,
            "Min Delay (ticks)", config.minDelayTicks, 1, 10,
            v -> {
                config.minDelayTicks = v;
                if (config.maxDelayTicks < v) config.maxDelayTicks = v;
            }));

        // Max delay ticks 1-10
        addDrawableChild(new IntSlider(
            centerX - w / 2, top + (row++) * (h + gap), w, h,
            "Max Delay (ticks)", config.maxDelayTicks, 1, 10,
            v -> {
                config.maxDelayTicks = v;
                if (config.minDelayTicks > v) config.minDelayTicks = v;
            }));

        // Require falling toggle
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.requireFalling)
            .build(centerX - w / 2, top + (row++) * (h + gap), w, h,
                Text.literal("Require Falling"),
                (btn, val) -> config.requireFalling = val));

        // Min fall distance slider (0-10 in 0.5 steps)
        addDrawableChild(new HalfStepSlider(
            centerX - w / 2, top + (row++) * (h + gap), w, h,
            "Min Fall Distance", config.minFallDistance,
            v -> config.minFallDistance = v));

        // Require shield active
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.requireShieldActive)
            .build(centerX - w / 2, top + (row++) * (h + gap), w, h,
                Text.literal("Require Active Shield"),
                (btn, val) -> config.requireShieldActive = val));

        // Show messages
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.showActionBarMessages)
            .build(centerX - w / 2, top + (row++) * (h + gap), w, h,
                Text.literal("Action-Bar Messages"),
                (btn, val) -> config.showActionBarMessages = val));

        // Save & close button
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"),
                btn -> {
                    config.save();
                    if (client != null) client.setScreen(parent);
                })
            .dimensions(centerX - 100, top + (row + 1) * (h + gap) + 6, 200, 20)
            .build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void close() {
        config.save();
        if (client != null) client.setScreen(parent);
    }

    // ---- slider widgets ----

    /** 0–100 percent slider. */
    private static class PercentSlider extends SliderWidget {
        private final String label;
        private final java.util.function.IntConsumer onChange;

        PercentSlider(int x, int y, int w, int h, String label, int initial,
                      java.util.function.IntConsumer onChange) {
            super(x, y, w, h, Text.literal(label + ": " + initial + "%"),
                  Math.max(0, Math.min(100, initial)) / 100.0);
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + intValue() + "%"));
        }

        @Override
        protected void applyValue() {
            onChange.accept(intValue());
        }

        private int intValue() {
            return (int) Math.round(value * 100);
        }
    }

    /** Integer slider with explicit min/max. */
    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final java.util.function.IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, String label, int initial,
                  int min, int max, java.util.function.IntConsumer onChange) {
            super(x, y, w, h, Text.literal(label + ": " + initial),
                  (double) (Math.max(min, Math.min(max, initial)) - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + intValue()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(intValue());
        }

        private int intValue() {
            return min + (int) Math.round(value * (max - min));
        }
    }

    /** 0.0–10.0 slider in 0.5 steps. */
    private static class HalfStepSlider extends SliderWidget {
        private final String label;
        private final java.util.function.DoubleConsumer onChange;

        HalfStepSlider(int x, int y, int w, int h, String label, double initial,
                       java.util.function.DoubleConsumer onChange) {
            super(x, y, w, h, Text.literal(label + ": " + initial),
                  Math.max(0, Math.min(10, initial)) / 10.0);
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + String.format("%.1f", doubleValue())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(doubleValue());
        }

        private double doubleValue() {
            // Snap to 0.5 increments
            double raw = value * 10.0;
            return Math.round(raw * 2.0) / 2.0;
        }
    }
}
