package dev.kubosky;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class SkyRenderer {
    private static final float R = 100.0F;
    private static float[] sx, sy, sz, ss;
    private static final float[][] NEB = new float[6][3];
    private static final float[] NEB_R = {26, 32, 22, 28, 24, 30};
    private static final int[] NEB_COL = {0x7a3cff, 0x2fb6ff, 0xff4fa3, 0x3cff9a, 0x7a3cff, 0xff9a3c};
    private static boolean nebInit;

    private static final class Meteor {
        float px, py, pz, vx, vy, vz;
        int age, life;
    }

    private static final List<Meteor> meteors = new ArrayList<>();
    private static final Random rnd = new Random();
    private static long lastTime = -1;

    private static void genStars() {
        if (sx != null) return;
        int n = 4000;
        sx = new float[n]; sy = new float[n]; sz = new float[n]; ss = new float[n];
        Random r = new Random(10842L);
        for (int i = 0; i < n; i++) {
            float[] d = dir(r);
            sx[i] = d[0]; sy[i] = d[1]; sz[i] = d[2];
            ss[i] = 0.2F + r.nextFloat() * 0.25F;
        }
        Random q = new Random(777L);
        for (int i = 0; i < 6; i++) NEB[i] = dir(q);
    }

    private static float[] dir(Random r) {
        double x, y, z, d;
        do {
            x = r.nextGaussian(); y = r.nextGaussian(); z = r.nextGaussian();
            d = x * x + y * y + z * z;
        } while (d < 1.0E-4);
        d = Math.sqrt(d);
        return new float[]{(float) (x / d), (float) (y / d), (float) (z / d)};
    }

    private static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F};
    }

    private static float[] frame(float nx, float ny, float nz) {
        float ux = 0, uy = 1, uz = 0;
        if (Math.abs(ny) > 0.9F) { ux = 1; uy = 0; }
        float t1x = uy * nz - uz * ny, t1y = uz * nx - ux * nz, t1z = ux * ny - uy * nx;
        float l = (float) Math.sqrt(t1x * t1x + t1y * t1y + t1z * t1z);
        t1x /= l; t1y /= l; t1z /= l;
        return new float[]{t1x, t1y, t1z, ny * t1z - nz * t1y, nz * t1x - nx * t1z, nx * t1y - ny * t1x};
    }

    public static void render(MatrixStack m, float td, ClientWorld w) {
        SkyConfig c = SkyConfig.get();
        float f = w.getSkyAngle(td);
        float cosv = MathHelper.cos(f * 6.2831855F);
        float day = MathHelper.clamp(cosv * 2.0F + 0.5F, 0.0F, 1.0F);
        float mul = c.dimAtNight ? 0.15F + 0.85F * day : 1.0F;
        float night = MathHelper.clamp(1.0F - (cosv * 2.0F + 0.25F), 0.0F, 1.0F);
        float starA = MathHelper.clamp(night * night * 2.0F, 0.0F, 1.0F);
        genStars();
        updateMeteors(w.getTime(), c, starA);

        RenderSystem.disableTexture();
        RenderSystem.disableFog();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();

        Matrix4f mat = m.peek().getModel();
        float[] top = rgb(c.top), hor = rgb(c.horizon), bot = rgb(c.bottom);
        int bands = 16, seg = 32;
        b.begin(7, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < bands; i++) {
            double lat0 = -90.0 + 180.0 * i / bands, lat1 = -90.0 + 180.0 * (i + 1) / bands;
            float[] c0 = band(lat0, top, hor, bot, mul), c1 = band(lat1, top, hor, bot, mul);
            float y0 = (float) Math.sin(Math.toRadians(lat0)) * R, y1 = (float) Math.sin(Math.toRadians(lat1)) * R;
            float r0 = (float) Math.cos(Math.toRadians(lat0)) * R, r1 = (float) Math.cos(Math.toRadians(lat1)) * R;
            for (int j = 0; j < seg; j++) {
                double a0 = Math.PI * 2 * j / seg, a1 = Math.PI * 2 * (j + 1) / seg;
                b.vertex(mat, (float) Math.cos(a0) * r0, y0, (float) Math.sin(a0) * r0).color(c0[0], c0[1], c0[2], 1.0F).next();
                b.vertex(mat, (float) Math.cos(a1) * r0, y0, (float) Math.sin(a1) * r0).color(c0[0], c0[1], c0[2], 1.0F).next();
                b.vertex(mat, (float) Math.cos(a1) * r1, y1, (float) Math.sin(a1) * r1).color(c1[0], c1[1], c1[2], 1.0F).next();
                b.vertex(mat, (float) Math.cos(a0) * r1, y1, (float) Math.sin(a0) * r1).color(c1[0], c1[1], c1[2], 1.0F).next();
            }
        }
        t.draw();

        if (c.aurora && starA > 0.02F) aurora(b, t, mat, starA, (w.getTime() % 200000L + td) * 0.03F);
        if (c.meteors && starA > 0.02F && !meteors.isEmpty()) drawMeteors(b, t, mat, starA, td);

        m.push();
        m.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
        m.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(f * 360.0F));
        mat = m.peek().getModel();

        if (c.nebula && starA > 0.02F) {
            for (int i = 0; i < 6; i++) blob(b, t, mat, NEB[i][0], NEB[i][1], NEB[i][2], NEB_R[i], NEB_COL[i], 0.16F * starA);
        }
        if (c.stars && starA > 0.01F) {
            int n = Math.min(c.starCount, sx.length);
            b.begin(7, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < n; i++) {
                float nx = sx[i], ny = sy[i], nz = sz[i], s = ss[i];
                float[] fr = frame(nx, ny, nz);
                float cx = nx * R, cy = ny * R, cz = nz * R;
                b.vertex(mat, cx - fr[0] * s - fr[3] * s, cy - fr[1] * s - fr[4] * s, cz - fr[2] * s - fr[5] * s).color(1, 1, 1, starA).next();
                b.vertex(mat, cx + fr[0] * s - fr[3] * s, cy + fr[1] * s - fr[4] * s, cz + fr[2] * s - fr[5] * s).color(1, 1, 1, starA).next();
                b.vertex(mat, cx + fr[0] * s + fr[3] * s, cy + fr[1] * s + fr[4] * s, cz + fr[2] * s + fr[5] * s).color(1, 1, 1, starA).next();
                b.vertex(mat, cx - fr[0] * s + fr[3] * s, cy - fr[1] * s + fr[4] * s, cz - fr[2] * s + fr[5] * s).color(1, 1, 1, starA).next();
            }
            t.draw();
        }
        if (c.sun) {
            disc(b, t, mat, R, c.sunSize * 2.2F, c.sunColor, 0.45F, 0.0F);
            disc(b, t, mat, R, c.sunSize, c.sunColor, 1.0F, 1.0F);
        }
        if (c.moon) {
            disc(b, t, mat, -R, c.moonSize * 1.8F, c.moonColor, 0.25F, 0.0F);
            disc(b, t, mat, -R, c.moonSize, c.moonColor, 1.0F, 1.0F);
        }
        m.pop();

        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.enableFog();
        RenderSystem.enableTexture();
        RenderSystem.depthMask(true);
    }

    private static void aurora(BufferBuilder b, Tessellator t, Matrix4f mat, float alpha, float time) {
        int n = 48;
        b.begin(7, VertexFormats.POSITION_COLOR);
        for (int layer = 0; layer < 2; layer++) {
            float base = 14 + layer * 7, rad = 92 - layer * 6;
            float[] cb = layer == 0 ? new float[]{0.2F, 1.0F, 0.5F} : new float[]{0.5F, 0.4F, 1.0F};
            float[] ct = layer == 0 ? new float[]{0.3F, 0.6F, 1.0F} : new float[]{0.9F, 0.3F, 0.9F};
            for (int i = 0; i < n; i++) {
                float f0 = i / (float) n, f1 = (i + 1) / (float) n;
                float a0 = (f0 - 0.5F) * 2.2F, a1 = (f1 - 0.5F) * 2.2F;
                float h0 = 22 + 8 * MathHelper.sin(a0 * 4 + time + layer * 2) + 5 * MathHelper.sin(a0 * 9 - time * 1.7F);
                float h1 = 22 + 8 * MathHelper.sin(a1 * 4 + time + layer * 2) + 5 * MathHelper.sin(a1 * 9 - time * 1.7F);
                float yb0 = base + 3 * MathHelper.sin(a0 * 3 + time * 0.7F), yb1 = base + 3 * MathHelper.sin(a1 * 3 + time * 0.7F);
                float e0 = MathHelper.sin(f0 * 3.14159F), e1 = MathHelper.sin(f1 * 3.14159F);
                b.vertex(mat, MathHelper.sin(a0) * rad, yb0, -MathHelper.cos(a0) * rad).color(cb[0], cb[1], cb[2], alpha * 0.55F * e0).next();
                b.vertex(mat, MathHelper.sin(a1) * rad, yb1, -MathHelper.cos(a1) * rad).color(cb[0], cb[1], cb[2], alpha * 0.55F * e1).next();
                b.vertex(mat, MathHelper.sin(a1) * rad, yb1 + h1, -MathHelper.cos(a1) * rad).color(ct[0], ct[1], ct[2], 0.0F).next();
                b.vertex(mat, MathHelper.sin(a0) * rad, yb0 + h0, -MathHelper.cos(a0) * rad).color(ct[0], ct[1], ct[2], 0.0F).next();
            }
        }
        t.draw();
    }

    private static void updateMeteors(long now, SkyConfig c, float starA) {
        if (now == lastTime) return;
        if (now < lastTime) meteors.clear();
        lastTime = now;
        Iterator<Meteor> it = meteors.iterator();
        while (it.hasNext()) {
            Meteor mt = it.next();
            if (++mt.age >= mt.life) it.remove();
        }
        if (c.meteors && starA > 0.3F && rnd.nextInt(Math.max(5, 120 - c.meteorRate * 10)) == 0) {
            float[] d = dir(rnd);
            d[1] = Math.abs(d[1]) * 0.6F + 0.35F;
            float[] o = dir(rnd);
            float tx = d[1] * o[2] - d[2] * o[1], ty = d[2] * o[0] - d[0] * o[2], tz = d[0] * o[1] - d[1] * o[0];
            float l = (float) Math.sqrt(tx * tx + ty * ty + tz * tz) + 1.0E-4F;
            Meteor mt = new Meteor();
            mt.px = d[0] * 95; mt.py = d[1] * 95; mt.pz = d[2] * 95;
            mt.vx = tx / l * 3.5F; mt.vy = ty / l * 3.5F - 0.8F; mt.vz = tz / l * 3.5F;
            mt.life = 14 + rnd.nextInt(10);
            meteors.add(mt);
        }
    }

    private static void drawMeteors(BufferBuilder b, Tessellator t, Matrix4f mat, float starA, float td) {
        b.begin(7, VertexFormats.POSITION_COLOR);
        for (Meteor mt : meteors) {
            float age = mt.age + td;
            float fade = MathHelper.clamp(1.0F - age / mt.life, 0.0F, 1.0F);
            float hx = mt.px + mt.vx * age, hy = mt.py + mt.vy * age, hz = mt.pz + mt.vz * age;
            float tx = hx - mt.vx * 5, ty = hy - mt.vy * 5, tz = hz - mt.vz * 5;
            float sx2 = mt.vy * hz - mt.vz * hy, sy2 = mt.vz * hx - mt.vx * hz, sz2 = mt.vx * hy - mt.vy * hx;
            float l = (float) Math.sqrt(sx2 * sx2 + sy2 * sy2 + sz2 * sz2) + 1.0E-4F;
            sx2 = sx2 / l * 0.35F; sy2 = sy2 / l * 0.35F; sz2 = sz2 / l * 0.35F;
            float a = starA * fade;
            b.vertex(mat, tx - sx2, ty - sy2, tz - sz2).color(1.0F, 0.95F, 0.8F, 0.0F).next();
            b.vertex(mat, tx + sx2, ty + sy2, tz + sz2).color(1.0F, 0.95F, 0.8F, 0.0F).next();
            b.vertex(mat, hx + sx2, hy + sy2, hz + sz2).color(1.0F, 1.0F, 1.0F, a).next();
            b.vertex(mat, hx - sx2, hy - sy2, hz - sz2).color(1.0F, 1.0F, 1.0F, a).next();
        }
        t.draw();
    }

    private static void blob(BufferBuilder b, Tessellator t, Matrix4f mat, float nx, float ny, float nz, float rad, int color, float a) {
        float[] fr = frame(nx, ny, nz), c = rgb(color);
        b.begin(6, VertexFormats.POSITION_COLOR);
        b.vertex(mat, nx * R, ny * R, nz * R).color(c[0], c[1], c[2], a).next();
        for (int k = 0; k <= 24; k++) {
            float ang = (float) (Math.PI * 2 * k / 24), co = MathHelper.cos(ang) * rad, si = MathHelper.sin(ang) * rad;
            b.vertex(mat, nx * R + fr[0] * co + fr[3] * si, ny * R + fr[1] * co + fr[4] * si, nz * R + fr[2] * co + fr[5] * si)
                    .color(c[0], c[1], c[2], 0.0F).next();
        }
        t.draw();
    }

    private static float[] band(double lat, float[] top, float[] hor, float[] bot, float mul) {
        float[] a = lat >= 0 ? top : bot;
        float t = (float) Math.pow(Math.abs(lat) / 90.0, lat >= 0 ? 0.6 : 0.8);
        return new float[]{
                (hor[0] + (a[0] - hor[0]) * t) * mul,
                (hor[1] + (a[1] - hor[1]) * t) * mul,
                (hor[2] + (a[2] - hor[2]) * t) * mul};
    }

    private static void disc(BufferBuilder b, Tessellator t, Matrix4f mat, float y, float size, int color, float centerA, float edgeA) {
        float[] c = rgb(color);
        b.begin(6, VertexFormats.POSITION_COLOR);
        b.vertex(mat, 0, y, 0).color(c[0], c[1], c[2], centerA).next();
        for (int k = 0; k <= 32; k++) {
            double a = Math.PI * 2 * k / 32;
            b.vertex(mat, (float) Math.cos(a) * size, y, (float) Math.sin(a) * size).color(c[0], c[1], c[2], edgeA).next();
        }
        t.draw();
    }
}
