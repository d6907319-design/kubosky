package dev.kubosky;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class EffectsScreen extends Screen {
    public static final String[] STYLES = {"Души", "Фейерверк", "Огонь", "Космос"};
    private static final String[] AURAS = {"Выкл", "Спираль", "Нимб", "Крылья"};

    public EffectsScreen() {
        super(new LiteralText("Kubo Sky: эффекты"));
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

        toggle(lx, y, "Эффект убийства", () -> c.killEffect, v -> c.killEffect = v);
        addButton(new ButtonWidget(lx, y + 24, 150, 20, new LiteralText("Стиль: " + STYLES[c.killStyle]), b -> {
            c.killStyle = (c.killStyle + 1) % STYLES.length;
            b.setMessage(new LiteralText("Стиль: " + STYLES[c.killStyle]));
        }));
        toggle(lx, y + 48, "Звук убийства", () -> c.killSound, v -> c.killSound = v);
        toggle(lx, y + 72, "Надпись, вспышка", () -> c.killBanner, v -> c.killBanner = v);

        toggle(rx, y, "Частицы удара", () -> c.hitParticles, v -> c.hitParticles = v);
        toggle(rx, y + 24, "Хитмаркер", () -> c.hitMarker, v -> c.hitMarker = v);
        addButton(new ButtonWidget(rx, y + 48, 150, 20, new LiteralText("Аура: " + AURAS[c.auraMode]), b -> {
            c.auraMode = (c.auraMode + 1) % AURAS.length;
            b.setMessage(new LiteralText("Аура: " + AURAS[c.auraMode]));
        }));
        toggle(rx, y + 72, "Красный край", () -> c.lowHp, v -> c.lowHp = v);

        addButton(new ButtonWidget(lx, y + 112, 150, 20, new LiteralText("Проверить эффект"), b -> Effects.test()));
        addButton(new ButtonWidget(rx, y + 112, 150, 20, new LiteralText("Назад"), b -> client.openScreen(new SkyScreen(0))));
        addButton(new ButtonWidget(lx, y + 136, 310, 20, new LiteralText("Ещё визуалы >"), b -> client.openScreen(new MoreScreen(0))));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, width / 2 - 165, 30, width / 2 + 165, 206, 0x99000000);
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
}
