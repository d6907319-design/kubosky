package dev.kubosky.mixin;

import dev.kubosky.SkyConfig;
import dev.kubosky.SkyRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private ClientWorld world;

    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
    private void kubosky$renderSky(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (SkyConfig.get().enabled && this.world != null && this.world.getRegistryKey() == World.OVERWORLD) {
            SkyRenderer.render(matrices, tickDelta, this.world);
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;FDDD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void kubosky$renderClouds(MatrixStack matrices, float tickDelta, double x, double y, double z, CallbackInfo ci) {
        if (!SkyConfig.get().clouds) {
            ci.cancel();
        }
    }
}
