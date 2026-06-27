package net.fullbrightnoshading.mixin;

import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightmapTextureManager.class)
public class MixinLightmapTextureManager {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object brightnessPlus$overrideGamma(SimpleOption<Double> option) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null && option == client.options.getGamma() && FullbrightNoShading.isFullbrightEnabled()) {
                return FullbrightNoShading.getRenderBrightness();
            }
        } catch (Throwable ignored) {
        }
        return option.getValue();
    }
}
