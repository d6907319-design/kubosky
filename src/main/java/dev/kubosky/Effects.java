package dev.kubosky;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class Effects {
    private static final DefaultParticleType[] MAIN = {ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.END_ROD, ParticleTypes.FLAME, ParticleTypes.PORTAL};
    private static final DefaultParticleType[] ACC = {ParticleTypes.SOUL, ParticleTypes.FIREWORK, ParticleTypes.SMOKE, ParticleTypes.ENCHANT};
    private static final int[] COL = {0x7FD8FF, 0xFFE27A, 0xFF7A3A, 0xC07AFF};
    private static final String[] DIRS = {"Север", "Восток", "Юг", "Запад"};

    private static final class Burst {
        double x, y, z;
        int age, style;
    }

    private static final class Floater {
        String text;
        int age, dx;
    }

    private static final List<Burst> bursts = new ArrayList<>();
    private static final List<Floater> floaters = new ArrayList<>();
    private static final Random rnd = new Random();
    private static KeyBinding zoomKey;
    private static PlayerEntity lastTarget;
    private static float lastTargetHealth = -1.0F, zoomT, zoomPrev;
    private static int tick, lastHitTick, lastKillTick = -1000, combo;
    private static int bannerAge = -1, flashAge = -1, hitMarkAge = -1, bannerStyle;
    private static String bannerText = "";

    private Effects() {}

    public static void init() {
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kubosky.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.kubosky"));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof PlayerEntity && entity != player) {
                onHit((PlayerEntity) entity);
            }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(Effects::tick);
        HudRenderCallback.EVENT.register(Effects::hud);
    }

    public static float zoomFactor(float td) {
        float z = zoomPrev + (zoomT - zoomPrev) * td;
        return 1.0F + (SkyConfig.get().zoom - 1) * z;
    }

    private static void p(ParticleEffect e, double x, double y, double z, double vx, double vy, double vz) {
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w != null) w.addParticle(e, x, y, z, vx, vy, vz);
    }

    private static void onHit(PlayerEntity t) {
        SkyConfig c = SkyConfig.get();
        if (t != lastTarget) lastTargetHealth = t.getHealth();
        lastTarget = t;
        lastHitTick = tick;
        hitMarkAge = 0;
        if (c.hitParticles) {
            for (int i = 0; i < 6; i++) {
                p(ParticleTypes.CRIT, t.getX(), t.getBodyY(0.6), t.getZ(), rnd.nextGaussian() * 0.3, 0.2 + rnd.nextDouble() * 0.2, rnd.nextGaussian() * 0.3);
            }
            for (int i = 0; i < 4; i++) {
                p(ParticleTypes.ENCHANTED_HIT, t.getX(), t.getBodyY(0.6), t.getZ(), rnd.nextGaussian() * 0.3, 0.2, rnd.nextGaussian() * 0.3);
            }
        }
    }

    public static void test() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Vec3d pos = mc.player.getPos().add(mc.player.getRotationVec(1.0F).multiply(3.0, 0.0, 3.0));
        kill(pos.x, mc.player.getY(), pos.z, "Тест");
        Floater f = new Floater();
        f.text = "-6.0";
        f.dx = 12;
        floaters.add(f);
    }

    private static void lightning(double x, double y, double z) {
        try {
            ClientWorld w = MinecraftClient.getInstance().world;
            if (w == null) return;
            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
            if (bolt == null) return;
            bolt.refreshPositionAfterTeleport(x, y, z);
            bolt.setCosmetic(true);
            w.addEntity(-4242 - (tick % 1000), bolt);
        } catch (Throwable ignored) {
        }
    }

    private static void kill(double x, double y, double z, String name) {
        SkyConfig c = SkyConfig.get();
        if (!c.killEffect) return;
        Burst b = new Burst();
        b.x = x; b.y = y; b.z = z; b.style = c.killStyle;
        bursts.add(b);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c.killSound && mc.world != null) {
            mc.world.playSound(x, y, z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.2F, false);
            mc.world.playSound(x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 0.8F, 1.0F, false);
        }
        if (c.killLightning) lightning(x, y, z);
        if (c.killBanner) {
            combo = (tick - lastKillTick <= 200) ? combo + 1 : 1;
            lastKillTick = tick;
            bannerText = name;
            bannerStyle = c.killStyle;
            bannerAge = 0;
            flashAge = 0;
        }
    }

    private static void ring(Burst b, DefaultParticleType type, int n, double speed, double yoff) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            p(type, b.x + Math.cos(a) * 0.3, b.y + yoff, b.z + Math.sin(a) * 0.3, Math.cos(a) * speed, 0.0, Math.sin(a) * speed);
        }
    }

    private static void step(Burst b) {
        DefaultParticleType main = MAIN[b.style], acc = ACC[b.style];
        if (b.age == 0) {
            for (int i = 0; i < 40; i++) {
                double x = rnd.nextGaussian(), y = rnd.nextGaussian(), z = rnd.nextGaussian();
                double l = Math.sqrt(x * x + y * y + z * z) + 1.0E-4;
                p(acc, b.x, b.y + 1.0, b.z, x / l * 0.35, y / l * 0.35, z / l * 0.35);
            }
            ring(b, main, 36, 0.45, 0.1);
        }
        if (b.age >= 1 && b.age <= 40) {
            for (int arm = 0; arm < 3; arm++) {
                double a = b.age * 0.45 + arm * 2.0944;
                double r = 0.9 - b.age * 0.01;
                p(main, b.x + Math.cos(a) * r, b.y + b.age * 0.05, b.z + Math.sin(a) * r, 0.0, 0.02, 0.0);
            }
            if (b.age % 10 == 0) ring(b, main, 24, 0.2, 0.05 + b.age * 0.05);
        }
        if (b.age >= 30 && b.age <= 45) {
            p(acc, b.x + rnd.nextGaussian() * 0.15, b.y + 0.2, b.z + rnd.nextGaussian() * 0.15, 0.0, 0.25, 0.0);
        }
    }

    private static void tick(MinecraftClient mc) {
        tick++;
        if (mc.world == null || mc.player == null) {
            bursts.clear();
            floaters.clear();
            lastTarget = null;
            return;
        }
        SkyConfig c = SkyConfig.get();
        zoomPrev = zoomT;
        zoomT += (((zoomKey.isPressed() && mc.currentScreen == null) ? 1.0F : 0.0F) - zoomT) * 0.4F;
        if (zoomT < 0.001F) zoomT = 0.0F;

        if (lastTarget != null) {
            float hp = lastTarget.getHealth();
            if (c.floatDmg && lastTargetHealth >= 0 && hp < lastTargetHealth - 0.05F) {
                Floater f = new Floater();
                f.text = "-" + (Math.round((lastTargetHealth - hp) * 10.0F) / 10.0F);
                f.dx = rnd.nextInt(31) - 5;
                floaters.add(f);
            }
            lastTargetHealth = hp;
            if (tick - lastHitTick > 100) {
                lastTarget = null;
            } else if (hp <= 0.0F) {
                kill(lastTarget.getX(), lastTarget.getY(), lastTarget.getZ(), lastTarget.getName().getString());
                lastTarget = null;
            }
        }
        Iterator<Floater> fi = floaters.iterator();
        while (fi.hasNext()) {
            if (++fi.next().age > 28) fi.remove();
        }
        Iterator<Burst> it = bursts.iterator();
        while (it.hasNext()) {
            Burst b = it.next();
            step(b);
            if (++b.age > 50) it.remove();
        }

        boolean first = mc.options.getPerspective().isFirstPerson();
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        DefaultParticleType main = MAIN[c.killStyle];
        if (c.trail) {
            Vec3d v = mc.player.getVelocity();
            if (v.x * v.x + v.z * v.z > 0.005) {
                for (int i = 0; i < 2; i++) {
                    p(main, px + rnd.nextGaussian() * 0.12, py + 0.05, pz + rnd.nextGaussian() * 0.12, 0.0, 0.02, 0.0);
                }
            }
        }
        if (c.auraMode == 1) {
            double y0 = py + 0.1 + ((tick % 40) / 40.0) * 1.8;
            for (int k = 0; k < 2; k++) {
                double a = tick * 0.25 + k * Math.PI;
                p(main, px + Math.cos(a) * 0.7, y0, pz + Math.sin(a) * 0.7, 0.0, 0.0, 0.0);
            }
        } else if (c.auraMode == 2 && !first) {
            double y0 = py + mc.player.getHeight() + 0.35;
            for (int k = 0; k < 6; k++) {
                double a = tick * 0.15 + k * Math.PI / 3;
                p(ParticleTypes.END_ROD, px + Math.cos(a) * 0.35, y0, pz + Math.sin(a) * 0.35, 0.0, 0.0, 0.0);
            }
        } else if (c.auraMode == 3 && !first && tick % 3 == 0) {
            double yaw = Math.toRadians(mc.player.bodyYaw);
            double bx = Math.sin(yaw), bz = -Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            double flap = Math.sin(tick * 0.2) * 0.2;
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 7; k++) {
                    double out = 0.2 + k * 0.17;
                    double up = 1.2 + 0.13 * k - 0.025 * k * k + flap * k * 0.15;
                    p(main, px + bx * (0.25 + k * 0.02) + rx * side * out, py + up, pz + bz * (0.25 + k * 0.02) + rz * side * out, 0.0, 0.0, 0.0);
                }
            }
        }

        if (bannerAge >= 0 && ++bannerAge > 70) bannerAge = -1;
        if (flashAge >= 0 && ++flashAge > 6) flashAge = -1;
        if (hitMarkAge >= 0 && ++hitMarkAge > 8) hitMarkAge = -1;
    }

    private static int accent(int i) {
        SkyConfig c = SkyConfig.get();
        if (c.rainbow) return 0xFF000000 | MathHelper.hsvToRgb(((tick * 2 + i * 18) % 360) / 360.0F, 0.55F, 1.0F);
        return 0xFF000000 | c.accent;
    }

    private static void box(MatrixStack m, int x1, int y1, int x2, int y2) {
        DrawableHelper.fill(m, x1, y1, x2, y2, 0xA0101018);
    }

    private static void key(MatrixStack m, TextRenderer tr, int x, int y, int w, String s, boolean down) {
        DrawableHelper.fill(m, x, y, x + w, y + 18, down ? ((0xB0 << 24) | (accent(0) & 0xFFFFFF)) : 0xA0101018);
        tr.drawWithShadow(m, s, x + (w - tr.getWidth(s)) / 2.0F, y + 5, down ? 0xFF101018 : 0xFFFFFFFF);
    }

    private static void hud(MatrixStack m, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        SkyConfig c = SkyConfig.get();
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int cx = sw / 2, cy = sh / 2;

        if (c.lowHp && mc.player.isAlive() && mc.player.getHealth() <= 6.0F) {
            float pulse = (MathHelper.sin((tick + td) * 0.2F) + 1.0F) / 2.0F;
            int a = (int) (50 + 70 * pulse);
            for (int i = 0; i < 6; i++) {
                int col = (((int) (a * (1.0F - i / 6.0F))) << 24) | 0xD01010;
                DrawableHelper.fill(m, 0, i * 6, sw, i * 6 + 6, col);
                DrawableHelper.fill(m, 0, sh - i * 6 - 6, sw, sh - i * 6, col);
                DrawableHelper.fill(m, i * 6, i * 6 + 6, i * 6 + 6, sh - i * 6 - 6, col);
                DrawableHelper.fill(m, sw - i * 6 - 6, i * 6 + 6, sw - i * 6, sh - i * 6 - 6, col);
            }
        }

        if (c.hudWatermark) {
            int fps = 0;
            try {
                String fs = mc.fpsDebugString;
                fps = Integer.parseInt(fs.substring(0, fs.indexOf(' ')));
            } catch (Exception ignored) {
            }
            int ping = 0;
            ClientPlayNetworkHandler h = mc.getNetworkHandler();
            if (h != null) {
                PlayerListEntry e = h.getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            String s1 = "KUBO  |  " + fps + " FPS  |  " + ping + " мс";
            box(m, 6, 6, 6 + tr.getWidth(s1) + 10, 19);
            tr.drawWithShadow(m, s1, 11, 9, accent(0));
            float yaw = MathHelper.wrapDegrees(mc.player.yaw);
            String s2 = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ() + "  " + DIRS[((int) Math.floor((yaw + 180.0F) / 90.0F + 0.5F)) & 3];
            box(m, 6, 22, 6 + tr.getWidth(s2) + 10, 35);
            tr.drawWithShadow(m, s2, 11, 25, 0xFFFFFFFF);
        }

        if (c.hudEffects) {
            int yy = 40;
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String name = new TranslatableText(e.getTranslationKey()).getString();
                if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
                int s = e.getDuration() / 20;
                String line = name + "  " + (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
                box(m, 6, yy, 6 + tr.getWidth(line) + 10, yy + 13);
                tr.drawWithShadow(m, line, 11, yy + 3, e.getEffectType().isBeneficial() ? 0xFF7BE37A : 0xFFFF7A7A);
                yy += 15;
            }
        }

        if (c.hudKeys) {
            int x0 = 8, y0 = sh - 74;
            key(m, tr, x0 + 20, y0, 18, "W", mc.options.keyForward.isPressed());
            key(m, tr, x0, y0 + 20, 18, "A", mc.options.keyLeft.isPressed());
            key(m, tr, x0 + 20, y0 + 20, 18, "S", mc.options.keyBack.isPressed());
            key(m, tr, x0 + 40, y0 + 20, 18, "D", mc.options.keyRight.isPressed());
            key(m, tr, x0, y0 + 40, 28, "ЛКМ", mc.options.keyAttack.isPressed());
            key(m, tr, x0 + 30, y0 + 40, 28, "ПКМ", mc.options.keyUse.isPressed());
        }

        if (c.hudList) {
            List<String> L = new ArrayList<>();
            if (c.enabled) L.add("Небо");
            if (c.enabled && c.stars) L.add("Звёзды");
            if (c.enabled && c.aurora) L.add("Сияние");
            if (c.enabled && c.meteors) L.add("Метеоры");
            if (c.enabled && c.nebula) L.add("Туманность");
            if (c.killEffect) L.add("Убийство");
            if (c.hitMarker) L.add("Хитмаркер");
            if (c.floatDmg) L.add("Урон");
            if (c.customCross) L.add("Прицел");
            if (c.trail) L.add("Шлейф");
            if (c.auraMode > 0) L.add("Аура");
            if (zoomT > 0.05F) L.add("Зум");
            L.sort((a, b) -> tr.getWidth(b) - tr.getWidth(a));
            int yy = mc.player.getStatusEffects().isEmpty() ? 6 : 56;
            for (int i = 0; i < L.size(); i++) {
                String s = L.get(i);
                int w = tr.getWidth(s) + 8;
                box(m, sw - w - 6, yy, sw - 6, yy + 12);
                tr.drawWithShadow(m, s, sw - w - 2, yy + 2, accent(i));
                yy += 12;
            }
        }

        if (c.customCross && mc.options.getPerspective().isFirstPerson()) {
            int gap = 3 + (mc.player.isSprinting() ? 3 : 0) + (hitMarkAge >= 0 ? 2 : 0);
            int col = hitMarkAge >= 0 ? 0xFFFF5A5A : accent(0);
            if (c.crossStyle == 0) {
                DrawableHelper.fill(m, cx - 1, cy - 1, cx + 1, cy + 1, col);
            } else if (c.crossStyle == 1) {
                DrawableHelper.fill(m, cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
                DrawableHelper.fill(m, cx - gap - 5, cy - 1, cx - gap, cy + 1, col);
                DrawableHelper.fill(m, cx + gap, cy - 1, cx + gap + 5, cy + 1, col);
                DrawableHelper.fill(m, cx - 1, cy - gap - 5, cx + 1, cy - gap, col);
                DrawableHelper.fill(m, cx - 1, cy + gap, cx + 1, cy + gap + 5, col);
            } else {
                for (int k = 0; k < 16; k++) {
                    double a = Math.PI * 2 * k / 16;
                    int rx = cx + (int) Math.round(Math.cos(a) * (gap + 3)), ry = cy + (int) Math.round(Math.sin(a) * (gap + 3));
                    DrawableHelper.fill(m, rx - 1, ry - 1, rx + 1, ry + 1, col);
                }
                DrawableHelper.fill(m, cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
            }
        }

        if (c.hitMarker && hitMarkAge >= 0 && hitMarkAge < 8) {
            int a = (int) (255 * (1.0F - hitMarkAge / 8.0F));
            if (a > 10) {
                int col = (a << 24) | 0xFFFFFF;
                for (int i = 0; i < 4; i++) {
                    int o = 4 + i * 2;
                    DrawableHelper.fill(m, cx + o, cy + o, cx + o + 2, cy + o + 2, col);
                    DrawableHelper.fill(m, cx - o - 2, cy + o, cx - o, cy + o + 2, col);
                    DrawableHelper.fill(m, cx + o, cy - o - 2, cx + o + 2, cy - o, col);
                    DrawableHelper.fill(m, cx - o - 2, cy - o - 2, cx - o, cy - o, col);
                }
            }
        }

        for (Floater f : floaters) {
            int a = (int) (255 * MathHelper.clamp(1.0F - (f.age + td) / 28.0F, 0.0F, 1.0F));
            if (a > 10) {
                tr.drawWithShadow(m, f.text, cx + 14 + f.dx, cy - 6 - (f.age + td) * 1.2F, (a << 24) | 0xFF5A3C);
            }
        }

        if (flashAge >= 0 && flashAge < 6) {
            int a = (int) (70 * (1.0F - flashAge / 6.0F));
            if (a > 3) DrawableHelper.fill(m, 0, 0, sw, sh, (a << 24) | 0xFFFFFF);
        }

        if (bannerAge >= 0 && bannerAge < 70) {
            int a = bannerAge < 50 ? 255 : (int) (255 * (70 - bannerAge) / 20.0F);
            if (a > 10) {
                float s = 2.2F + 1.2F * Math.max(0.0F, 1.0F - bannerAge / 6.0F);
                int col = (a << 24) | COL[bannerStyle];
                int white = (a << 24) | 0xFFFFFF;
                m.push();
                m.translate(sw / 2.0F, sh * 0.26F, 0.0F);
                m.scale(s, s, 1.0F);
                String title = "УБИЙСТВО";
                tr.drawWithShadow(m, title, -tr.getWidth(title) / 2.0F, 0.0F, col);
                m.pop();
                String sub = combo > 1 ? bannerText + "  ·  серия x" + combo : bannerText;
                tr.drawWithShadow(m, sub, sw / 2.0F - tr.getWidth(sub) / 2.0F, sh * 0.26F + 26.0F, white);
            }
        }

        if (c.hudArmor) {
            int ax = sw - 24, ay = sh / 2 - 40;
            for (int i = 3; i >= 0; i--) {
                ItemStack st = mc.player.inventory.getArmorStack(i);
                box(m, ax - 2, ay - 2, ax + 18, ay + 18);
                if (!st.isEmpty()) {
                    mc.getItemRenderer().renderGuiItemIcon(st, ax, ay);
                    mc.getItemRenderer().renderGuiItemOverlay(tr, st, ax, ay);
                }
                ay += 22;
            }
        }
    }
}
