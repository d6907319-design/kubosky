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

public class MoreScreen extends Screen {
    private static final String[] TITLES = {"Небо+", "HUD", "Прицел и мир"};
    private static final String[] CROSS = {"Точка", "Плюс", "Круг"};
    private static final String[] ACC_NAMES = {"Голубой", "Розовый", "Зелёный", "Оранжевый", "Фиолетовый"};
    private static final int[] ACC = {0x8fb0ff, 0xff6fb5, 0x7be37a, 0xffa24a, 0xb48cff};
    private final int page;

    public MoreScreen(int page) {
        super(new LiteralText("Kubo Sky: " + TITLES[page]));
        this.page = page;
    }

    private int cx(int i) { return i % 2 == 0 ? width / 2 - 155 : width / 2 + 5; }

    private int cy(int i) { return 40 + (i / 2) * 24; }

    private static Text label(String n, boolean v) {
        return new LiteralText(n + ": " + (v ? "вкл" : "выкл"));
    }

    private void toggle(int i, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addButton(new ButtonWidget(cx(i), cy(i), 150, 20, label(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(name, get.getAsBoolean()));
        }));
    }

    private void slider(int i, String name, int min, int max, int val, IntConsumer set) {
        addButton(new Sl(cx(i), cy(i), name, min, max, val, set));
    }

    @Override
    protected void init() {
        SkyConfig c = SkyConfig.get();
        if (page == 0) {
            toggle(0, "Северное сияние", () -> c.aurora, v -> c.aurora = v);
            toggle(1, "Метеоры", () -> c.meteors, v -> c.meteors = v);
            toggle(2, "Туманность", () -> c.nebula, v -> c.nebula = v);
            slider(3, "Частота метеоров", 1, 10, c.meteorRate, v -> c.meteorRate = v);
            slider(4, "Зум (клавиша C) x", 2, 10, c.zoom, v -> c.zoom = v);
        } else if (page == 1) {
            toggle(0, "Вотермарк", () -> c.hudWatermark, v -> c.hudWatermark = v);
            toggle(1, "Клавиши", () -> c.hudKeys, v -> c.hudKeys = v);
            toggle(2, "Эффекты зелий", () -> c.hudEffects, v -> c.hudEffects = v);
            toggle(3, "Броня", () -> c.hudArmor, v -> c.hudArmor = v);
            toggle(4, "Список модулей", () -> c.hudList, v -> c.hudList = v);
            toggle(5, "Цифры урона", () -> c.floatDmg, v -> c.floatDmg = v);
            toggle(6, "Радуга", () -> c.rainbow, v -> c.rainbow = v);
            addButton(new ButtonWidget(cx(7), cy(7), 150, 20, new LiteralText("Цвет HUD: " + ACC_NAMES[accIdx(c)]), b -> {
                c.accent = ACC[(accIdx(c) + 1) % ACC.length];
                b.setMessage(new LiteralText("Цвет HUD: " + ACC_NAMES[accIdx(c)]));
            }));
        } else {
            toggle(0, "Свой прицел", () -> c.customCross, v -> c.customCross = v);
            addButton(new ButtonWidget(cx(1), cy(1), 150, 20, new LiteralText("Прицел: " + CROSS[c.crossStyle]), b -> {
                c.crossStyle = (c.crossStyle + 1) % CROSS.length;
                b.setMessage(new LiteralText("Прицел: " + CROSS[c.crossStyle]));
            }));
            toggle(2, "Шлейф", () -> c.trail, v -> c.trail = v);
            toggle(3, "Молния (убийство)", () -> c.killLightning, v -> c.killLightning = v);
        }
        int ny = 144;
        int lx = width / 2 - 155;
        addButton(new ButtonWidget(lx, ny, 100, 20, new LiteralText("< Назад"),
                b -> client.openScreen(page > 0 ? new MoreScreen(page - 1) : new EffectsScreen())));
        addButton(new ButtonWidget(lx + 105, ny, 100, 20, new LiteralText("Готово"), b -> onClose()));
        addButton(new ButtonWidget(lx + 210, ny, 100, 20, new LiteralText(page < 2 ? "Далее >" : "К небу >"),
                b -> client.openScreen(page < 2 ? new MoreScreen(page + 1) : new SkyScreen(0))));
    }

    private static int accIdx(SkyConfig c) {
        for (int i = 0; i < ACC.length; i++) if (ACC[i] == c.accent) return i;
        return 0;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, width / 2 - 165, 30, width / 2 + 165, 172, 0x99000000);
        drawCenteredText(matrices, textRenderer, title, width / 2, 16, 0xFFFFFF);
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

    private static class Sl extends SliderWidget {
        private final String name;
        private final int min, max;
        private final IntConsumer set;

        Sl(int x, int y, String name, int min, int max, int val, IntConsumer set) {
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
