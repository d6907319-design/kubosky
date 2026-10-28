package dev.kubosky.mixin;

import dev.kubosky.SkyConfig;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void kubosky$crosshair(MatrixStack matrices, CallbackInfo ci) {
        if (SkyConfig.get().customCross) {
            ci.cancel();
        }
    }
}
