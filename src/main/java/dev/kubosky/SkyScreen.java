package dev.kubosky;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class SkyScreen extends Screen {
    private static final String[] NAMES = {"Верх неба", "Горизонт", "Низ неба", "Солнце", "Луна"};
    private static int presetIdx = 0;
    private final int target;

    public SkyScreen(int target) {
        super(new LiteralText("Kubo Sky"));
        this.target = target;
    }

    private static Text label(String n, boolean v) {
        return new LiteralText(n + ": " + (v ? "вкл" : "выкл"));
    }

    private void toggle(int x, int y, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addButton(new ButtonWidget(x, y, 150, 20, label(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(name, get.getAsBoolean()));
        }));
    }

    @Override
    protected void init() {
        SkyConfig c = SkyConfig.get();
        int lx = width / 2 - 155, rx = width / 2 + 5, y = 40;

        toggle(lx, y, "Своё небо", () -> c.enabled, v -> c.enabled = v);
        toggle(lx, y + 24, "Звёзды", () -> c.stars, v -> c.stars = v);
        toggle(lx, y + 48, "Солнце", () -> c.sun, v -> c.sun = v);
        toggle(lx, y + 72, "Луна", () -> c.moon, v -> c.moon = v);
        toggle(lx, y + 96, "Облака", () -> c.clouds, v -> c.clouds = v);
        toggle(lx, y + 120, "Темнее ночью", () -> c.dimAtNight, v -> c.dimAtNight = v);
        addButton(new ButtonWidget(lx, y + 144, 150, 20,
                new LiteralText("Пресет: " + SkyConfig.PRESETS[presetIdx]), b -> {
            c.applyPreset(presetIdx);
            presetIdx = (presetIdx + 1) % SkyConfig.PRESETS.length;
            client.openScreen(new SkyScreen(target));
        }));

        addButton(new ButtonWidget(rx, y, 150, 20, new LiteralText("Цвет: " + NAMES[target]),
                b -> client.openScreen(new SkyScreen((target + 1) % NAMES.length))));
        int cur = c.getColor(target);
        addButton(new IntSlider(rx, y + 24, "R", 0, 255, (cur >> 16) & 255,
                v -> c.setColor(target, (c.getColor(target) & 0x00FFFF) | (v << 16))));
        addButton(new IntSlider(rx, y + 48, "G", 0, 255, (cur >> 8) & 255,
                v -> c.setColor(target, (c.getColor(target) & 0xFF00FF) | (v << 8))));
        addButton(new IntSlider(rx, y + 72, "B", 0, 255, cur & 255,
                v -> c.setColor(target, (c.getColor(target) & 0xFFFF00) | v)));
        addButton(new IntSlider(rx, y + 96, "Звёзд", 0, 4000, c.starCount, v -> c.starCount = v));
        addButton(new IntSlider(rx, y + 120, "Размер солнца", 5, 80, c.sunSize, v -> c.sunSize = v));
        addButton(new IntSlider(rx, y + 144, "Размер луны", 5, 80, c.moonSize, v -> c.moonSize = v));

        addButton(new ButtonWidget(lx, y + 176, 150, 20, new LiteralText("Эффекты >"), b -> client.openScreen(new EffectsScreen())));
        addButton(new ButtonWidget(rx, y + 176, 150, 20, new LiteralText("Готово"), b -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, width / 2 - 165, 30, width / 2 + 165, 240, 0x99000000);
        drawCenteredText(matrices, textRenderer, title, width / 2, 16, 0xFFFFFF);
        fill(matrices, width / 2 + 5, 224, width / 2 + 155, 234, 0xFF000000 | SkyConfig.get().getColor(target));
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        SkyConfig.save();
    }

    private static class IntSlider extends SliderWidget {
        private final String name;
        private final int min, max;
        private final IntConsumer set;

        IntSlider(int x, int y, String name, int min, int max, int val, IntConsumer set) {
            super(x, y, 150, 20, new LiteralText(name + ": " + val), (val - min) / (double) (max - min));
            this.name = name;
            this.min = min;
            this.max = max;
            this.set = set;
        }

        private int get() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(new LiteralText(name + ": " + get()));
        }

        @Override
        protected void applyValue() {
            set.accept(get());
        }
    }
}
